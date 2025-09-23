# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VocaTa is an AI role-playing platform where users can have voice and text conversations with characters like Harry Potter, Socrates, etc.

**核心技术栈**: Spring Boot 3.1.4 + Java 17 + MyBatis Plus 3.5.3.2 + Sa-Token 1.37.0 + PostgreSQL 42.6.0 + Redis (Redisson 3.23.4) + Hutool 5.8.22

**主要依赖**: Spring Boot Validation, Spring Boot AOP, Spring Dotenv 4.0.0

## 构建和运行命令

### 后端服务 (vocata-server)
```bash
# 本地开发运行 (端口 9010)
cd vocata-server
mvn spring-boot:run

# 构建JAR包
mvn clean package

# 测试环境运行
mvn spring-boot:run -Dspring-boot.run.profiles=test

# 生产环境运行
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Maven仓库配置
# 使用阿里云Maven镜像加速构建
```

### Frontend (when available)
```bash
# Client frontend
cd vocata-web
npm install
npm run dev

# Admin frontend
cd vocata-admin
npm install
npm run dev
```

## Environment Configuration

项目使用基于Profile的环境配置：

- **本地开发 (默认)**: `application.yml` + `.env`
  - 数据库: `vocata_local` @ localhost:5432
  - Redis: localhost:6379 (database: 0)
  - 端口: 9010
  - 日志级别: DEBUG，输出到控制台和 `logs/vocata-local.log`
  - 环境变量: 通过 `.env` 文件配置（参考 `.env.example`）

- **测试环境**: `application-test.yml`
  - 数据库: `vocata_test` @ test-server.vocata.com:5432
  - Redis: test-server.vocata.com:6379 (database: 1)
  - 日志级别: INFO，输出到 `logs/vocata-test.log`
  - Sa-Token: 7天有效期，启用操作日志

- **生产环境**: `application-prod.yml`
  - 数据库连接池: 最大20个连接，最小10个空闲连接
  - Redis连接池: 最大16个活跃连接
  - 日志: 输出到 `/var/log/vocata/vocata-server.log`，最大100MB，保留30个历史文件
  - Sa-Token: 7天有效期，禁用并发登录，关闭操作日志

**环境切换**: `--spring.profiles.active=test|prod` 或 IDE Active Profiles 配置

## Core Architecture Patterns

### 1. Unified API Response Format
All Controllers must return `ApiResponse<T>` wrapper:
```java
public ApiResponse<PageResult<UserResponse>> getUsers() {
    return ApiResponse.success(userService.getUsers());
}
```

### 2. Exception Handling Architecture
- Business exceptions: `throw new BizException(ApiCode.USER_NOT_EXIST)`
- Global handler: `GlobalExceptionHandler` converts to `ApiResponse` format
- Status codes: `ApiCode` enum defines all error codes

### 3. Authentication & Authorization
- **Framework**: Sa-Token (JWT-based)
- **User Context**: `UserContext.getUserId()` / `UserContext.checkAdmin()`
- **Route Protection**:
  - `/api/client/**` - client APIs (some public, some auth required)
  - `/api/admin/**` - admin-only APIs
  - `/api/open/**` - public APIs

### 4. 数据访问模式
- **基础实体**: 所有实体必须继承 `BaseEntity`，自动填充审计字段
- **表命名**: 数据库表使用 `vocata_` 前缀，关联表以 `_relation` 结尾
- **实体映射**: 实体类使用 `@TableName("vocata_user")` 指定表名
- **主键策略**: 使用 `@TableId(type = IdType.ASSIGN_ID)` 生成雪花ID
- **软删除**: `@TableLogic` 注解在 `isDelete` 字段，值为 0(未删除)/1(已删除)
- **审计字段**: 自动填充 `create_id`, `create_date`, `update_id`, `update_date`, `is_delete`
- **分页查询**: 返回 `PageResult<T>` 包装器处理分页数据

### 5. 模块结构规范
每个业务模块遵循标准结构：
```
src/main/java/com/vocata/{module}/
├── controller/     # REST API端点
├── service/        # 业务逻辑接口
│   └── impl/       # 业务逻辑实现
├── mapper/         # MyBatis Plus数据访问层
├── entity/         # 数据库实体类
├── dto/           # 请求/响应对象
└── constants/      # 模块常量定义
```

**当前已实现模块**:
- `auth` - 认证授权模块
- `user` - 用户管理模块
- `character` - 角色管理模块
- `conversation` - 对话管理模块
- `favorite` - 收藏功能模块
- `admin` - 管理后台模块
- `ai` - AI集成模块
- `search` - 搜索功能模块
- `common` - 通用组件模块

## 数据库架构规范

### 表设计规范
- **表命名**: 使用 `vocata_` 前缀，如 `vocata_user`, `vocata_character`
- **关联表**: 以 `_relation` 结尾，如 `vocata_user_character_relation`
- **字段类型**: 禁用ENUM，使用SMALLINT表示枚举值
- **主键**: 使用 `BIGSERIAL PRIMARY KEY`
- **时间字段**: 使用 `TIMESTAMP WITH TIME ZONE`
- **JSON数据**: 使用 `JSONB` 类型
- **外键约束**: 禁用物理外键，通过关联表实现关系

### 审计字段标准
每张表必须包含以下审计字段：
```sql
create_id BIGINT NOT NULL,           -- 创建人ID
update_id BIGINT,                    -- 更新人ID
create_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_delete SMALLINT DEFAULT 0         -- 软删除标记 0.否 1.是
```

### MyBatis Plus配置
- **下划线转驼峰**: 自动转换字段命名
- **逻辑删除**: `is_delete` 字段，值为 0/1
- **ID策略**: `ASSIGN_ID` 生成雪花ID
- **更新策略**: `NOT_NULL` 忽略空值更新
- **SQL日志**: 开发环境输出，测试/生产环境关闭

## 关键配置文件

### 核心配置类
- **安全配置**: `SaTokenConfig.java`
  - 定义路由保护规则
  - 设置用户上下文
  - 管理员权限检查
  - 排除公开API路径

- **数据库配置**: `MybatisPlusConfig.java`
  - 分页插件配置
  - 审计字段自动填充
  - 逻辑删除配置

### 环境配置文件
- **本地开发**: `application.yml` + `.env`
- **测试环境**: `application-test.yml`
- **生产环境**: `application-prod.yml`
- **环境变量模板**: `.env.example`

### 开发规范文档
- `docs/后端开发规范.md` - 编码规范和最佳实践
- `docs/数据库结构规范要求.md` - 数据库设计规范
- `docs/api接口定义规范要求.md` - API设计规范
- `docs/后端文件组织规范要求.md` - 项目结构规范

## 项目实现状态

### 已完成模块
- **用户认证系统**: 注册、登录、登出功能
- **权限框架**: 基于Sa-Token的角色访问控制
- **数据访问层**: MyBatis Plus + PostgreSQL集成
- **统一响应格式**: `ApiResponse<T>` 包装器
- **异常处理**: 全局异常处理和错误码体系
- **基础实体**: `BaseEntity` 审计字段自动填充
- **用户上下文**: `UserContext` 线程安全的用户信息管理
- **配置管理**: 多环境配置和环境变量支持

### 开发中模块
- **角色管理系统**: 实体和基础结构已创建
- **对话管理**: 实体和基础结构已创建
- **收藏功能**: 实体和基础结构已创建
- **AI集成层**: 基础架构已规划

### 待实现功能
- AI服务集成和对话生成
- 文件上传和存储
- 邮件服务集成
- 前端应用 (客户端和管理后台)
- Docker容器化部署
- 性能监控和日志分析

## 开发工作流程

### 本地开发环境搭建
1. 复制 `.env.example` 为 `.env` 并配置本地环境变量
2. 启动 PostgreSQL 和 Redis 服务
3. 创建 `vocata_local` 数据库
4. 运行 `mvn spring-boot:run` 启动服务
5. 服务启动在 http://localhost:9010/api

### 常用开发命令
```bash
# 清理编译
mvn clean compile

# 运行测试（当前无测试用例）
mvn test

# 热重载开发（IDE中配置）
mvn spring-boot:run -Dspring-boot.run.fork=false

# 查看项目依赖
mvn dependency:tree

# 检查代码风格
mvn checkstyle:check
```