# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VocaTa is an AI role-playing platform where users can have voice and text conversations with characters like Harry Potter, Socrates, etc.

**核心技术栈**: Spring Boot 3.1.4 + Java 17 + MyBatis Plus 3.5.3.2 + Sa-Token 1.37.0 + PostgreSQL 42.6.0 + Redis (Lettuce) + Hutool 5.8.22 + 七牛云存储

**主要依赖**: Spring Boot Validation, Spring Boot AOP, Spring Boot Mail, 七牛云 SDK

**架构特点**: 前后端分离、RESTful API、JWT认证、雪花ID、软删除、Docker化部署

## 构建和运行命令

### 后端服务 (vocata-server)
```bash
# 本地开发运行 (端口 9009)
cd vocata-server
mvn spring-boot:run

# 指定本地环境运行
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 构建JAR包
mvn clean package -DskipTests

# 测试环境运行
mvn spring-boot:run -Dspring-boot.run.profiles=test

# 生产环境运行
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Docker构建和运行
docker-compose up -d          # 启动所有服务
docker-compose up vocata-server # 仅启动后端服务
docker-compose down           # 停止所有服务
```

### 前端应用
```bash
# 客户端前端 (端口 3000)
cd vocata-web
npm install
npm run dev
npm run build

# 管理后台 (端口 3001)
cd vocata-admin
npm install
npm run dev
npm run build:test
```

## Environment Configuration

项目使用基于Profile的多环境配置：

### 本地开发环境 (local)
- **配置文件**: `application.yml` + `application-local.yml`
- **服务端口**: 9009
- **数据库**: 云端PostgreSQL (Aiven)
- **缓存**: 云端Redis
- **文件存储**: 七牛云存储
- **邮件服务**: 163邮箱SMTP
- **日志级别**: DEBUG，输出到控制台和文件 `logs/vocata-local.log`
- **特性**:
  - 敏感配置直接写在 application-local.yml 中（开发环境）
  - MyBatis SQL日志输出
  - 详细的调试信息

### 测试环境 (test)
- **配置文件**: `application-test.yml`
- **数据库**: 测试专用数据库
- **Redis**: database: 1 (隔离测试数据)
- **日志级别**: INFO，输出到 `logs/vocata-test.log`
- **Sa-Token**: 7天有效期，启用操作日志

### 生产环境 (prod)
- **配置文件**: `application-prod.yml`
- **数据库连接池**: 最大20个连接，最小10个空闲连接
- **Redis连接池**: 最大16个活跃连接
- **日志**: 输出到 `/var/log/vocata/vocata-server.log`，最大100MB，保留30个历史文件
- **Sa-Token**: 30天有效期，禁用并发登录，关闭操作日志
- **安全**: 禁用SQL日志，启用生产级别的性能优化

**环境切换**: `--spring.profiles.active=local|test|prod` 或 IDE Active Profiles 配置

## Core Architecture Patterns

### 1. 统一API响应格式
所有Controller必须返回 `ApiResponse<T>` 包装器：
```java
public ApiResponse<PageResult<UserResponse>> getUsers() {
    return ApiResponse.success(userService.getUsers());
}
```

**重要**: 所有ID字段在API响应中统一使用String类型，避免前端JavaScript大数精度丢失问题：
```java
public class UserResponse {
    private String id;  // 统一使用String类型
    private String userId;
    // 其他字段...
}
```

### 2. Exception Handling Architecture
- Business exceptions: `throw new BizException(ApiCode.USER_NOT_EXIST)`
- Global handler: `GlobalExceptionHandler` converts to `ApiResponse` format
- Status codes: `ApiCode` enum defines all error codes

### 3. 认证与授权系统
- **认证框架**: Sa-Token (JWT-based)
- **用户上下文**: `UserContext.getUserId()` / `UserContext.checkAdmin()`
- **路由保护策略**:
  - `/api/client/**` - 客户端APIs (部分公开，部分需要认证)
  - `/api/admin/**` - 管理员专用APIs
  - `/api/open/**` - 完全公开APIs

**用户注册机制**:
- 用户名由系统自动生成，格式为 `vocata-{随机字符串}`
- 用户只需提供邮箱和密码即可完成注册
- 注册后可通过设置接口修改昵称等个人信息

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
- `auth` - 认证授权模块 (注册、登录、登出、验证码)
- `user` - 用户管理模块 (用户信息、设置)
- `file` - 文件管理模块 (七牛云存储集成)
- `admin` - 管理后台模块 (管理员认证、用户管理)
- `common` - 通用组件模块 (基础实体、响应包装器、异常处理)

**规划中模块**:
- `character` - 角色管理模块
- `conversation` - 对话管理模块
- `favorite` - 收藏功能模块
- `ai` - AI集成模块
- `search` - 搜索功能模块

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
- **本地开发**: `application.yml` + `application-local.yml`
- **测试环境**: `application-test.yml`
- **生产环境**: `application-prod.yml`
- **Docker配置**: `docker-compose.yml`, `docker-compose.test.yml`, `docker-compose.prod.yml`

### 开发规范文档
- `docs/后端开发规范.md` - 编码规范和最佳实践
- `docs/数据库结构规范要求.md` - 数据库设计规范
- `docs/api接口定义规范要求.md` - API设计规范
- `docs/后端文件组织规范要求.md` - 项目结构规范

## 项目实现状态

### 已完成功能
- **用户认证系统**: 注册(邮箱验证码)、登录、登出、密码重置
- **权限框架**: 基于Sa-Token的JWT认证和角色访问控制
- **数据访问层**: MyBatis Plus + PostgreSQL集成，审计字段自动填充
- **文件存储**: 七牛云存储集成，支持图片上传
- **统一响应格式**: `ApiResponse<T>` 包装器，ID字段统一String类型
- **异常处理**: 全局异常处理和完整的错误码体系
- **基础实体**: `BaseEntity` 审计字段自动填充，软删除支持
- **用户上下文**: `UserContext` 线程安全的用户信息管理
- **多环境配置**: 本地/测试/生产环境配置支持
- **邮件服务**: 163邮箱SMTP集成，验证码发送
- **管理后台**: 管理员认证、用户管理功能
- **Docker化部署**: 完整的docker-compose配置(开发/测试/生产)
- **CI/CD流程**: GitHub Actions自动化构建和测试

### 技术债务和优化点
- 单元测试覆盖率待提升
- API文档生成 (Swagger/OpenAPI)
- 性能监控和链路追踪
- 日志聚合和分析
- 缓存策略优化
- 数据库连接池调优

### 待开发核心功能
- **AI角色对话**: 角色管理、对话生成、语音转换
- **用户互动**: 收藏、评论、分享功能
- **搜索系统**: 角色搜索、对话历史搜索
- **前端应用**: 客户端和管理后台界面

## Docker容器化部署

### 开发环境部署
项目提供完整的Docker容器化解决方案，支持一键启动所有服务：

```bash
# 启动所有服务 (PostgreSQL + Redis + 后端 + 前端 + 工具)
docker-compose up -d

# 仅启动基础服务
docker-compose up -d postgres redis

# 启动后端服务
docker-compose up -d vocata-server

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f vocata-server
```

### 服务端口分配
- **后端API服务**: http://localhost:9009
- **客户端前端**: http://localhost:3000
- **管理后台**: http://localhost:3001
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379
- **pgAdmin**: http://localhost:5050 (admin@vocata.com / admin123)
- **MailHog**: http://localhost:8025 (邮件测试工具)

### 多环境Docker配置
- `docker-compose.yml` - 本地开发环境
- `docker-compose.test.yml` - 测试环境
- `docker-compose.prod.yml` - 生产环境

### 健康检查和依赖管理
所有服务都配置了健康检查和服务依赖关系，确保启动顺序正确。

## CI/CD自动化流程

项目配置了完整的GitHub Actions CI/CD流水线 (`.github/workflows/ci.yml`)：

### 触发条件
- Pull Request到 `develop` 或 `master` 分支
- Push到 `develop` 分支

### CI流程
1. **后端CI** (`backend-ci`):
   - JDK 17环境设置
   - Maven依赖缓存
   - 代码编译和风格检查
   - 单元测试执行
   - JAR包构建
   - 构建产物上传

2. **前端客户端CI** (`frontend-web-ci`):
   - Node.js 20环境设置
   - npm依赖安装
   - TypeScript类型检查
   - ESLint代码检查
   - 测试版本构建

3. **管理后台CI** (`frontend-admin-ci`):
   - 与客户端CI流程相同
   - 独立构建和检查

4. **CI结果汇总** (`ci-summary`):
   - 汇总所有组件的CI结果
   - 生成详细的执行报告
   - 失败时阻止合并操作

### 构建产物管理
- JAR包和前端构建产物自动上传
- 保留7天的构建历史
- 支持手动下载和部署

## 开发工作流程

### 本地开发环境搭建

**方式一：使用已配置的云服务 (推荐)**
1. 项目已配置云端PostgreSQL和Redis服务
2. 检查 `application-local.yml` 配置无误
3. 运行 `mvn spring-boot:run` 启动服务
4. 访问 http://localhost:9009/api

**方式二：使用Docker本地环境**
1. 启动Docker服务
2. 运行 `docker-compose up -d postgres redis` 启动数据库
3. 运行 `mvn spring-boot:run` 启动后端服务
4. 可选：启动 `pgAdmin` 和 `MailHog` 等开发工具

**方式三：完整Docker化开发**
1. 运行 `docker-compose up -d` 启动所有服务
2. 后端服务会自动等待数据库就绪后启动
3. 访问各服务的对应端口

### 开发规范和最佳实践

**代码提交流程**:
1. 从 `develop` 分支创建功能分支: `feat/功能描述`
2. 完成开发后提交到功能分支
3. 创建Pull Request到 `develop` 分支
4. CI/CD自动执行代码检查和构建
5. 代码审查通过后合并到 `develop`
6. 定期从 `develop` 合并到 `master` 进行发布

**环境配置优先级**:
1. `application-local.yml` > `application.yml` (本地开发)
2. `application-test.yml` > `application.yml` (测试环境)
3. `application-prod.yml` > `application.yml` (生产环境)

**ID字段处理规范**:
- 数据库层：使用 `BIGINT` 类型和雪花ID生成策略
- 服务层：内部处理使用 `Long` 类型
- API层：响应对象统一使用 `String` 类型避免精度丢失

### 常用开发命令
```bash
# 项目构建和测试
mvn clean compile                    # 清理编译
mvn clean package -DskipTests        # 构建JAR包(跳过测试)
mvn test                            # 运行单元测试
mvn spring-boot:run                 # 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=local  # 指定环境启动

# 代码质量检查
mvn checkstyle:check               # 代码风格检查(如果配置了checkstyle)
mvn dependency:tree                # 查看依赖树
mvn dependency:analyze             # 分析依赖使用情况

# Docker相关命令
docker-compose up -d               # 后台启动所有服务
docker-compose up vocata-server    # 前台启动后端服务
docker-compose logs -f vocata-server  # 查看服务日志
docker-compose ps                  # 查看服务状态
docker-compose down               # 停止所有服务
docker-compose restart vocata-server  # 重启后端服务

# 数据库和缓存管理
docker-compose exec postgres psql -U vocata -d vocata_local  # 连接数据库
docker-compose exec redis redis-cli  # 连接Redis

# 开发工具
curl http://localhost:9009/api/health  # 健康检查
curl -H "Authorization: Bearer TOKEN" http://localhost:9009/api/client/user/info  # API测试
```

## API文档和测试

### API设计规范
- **统一前缀**: `/api/`
- **版本控制**: 暂未启用，预留 `/api/v1/` 格式
- **路由分类**:
  - `/api/open/**` - 公开API，无需认证
  - `/api/client/**` - 客户端API，部分需要认证
  - `/api/admin/**` - 管理员API，需要管理员权限
  - `/api/health` - 健康检查端点

### 请求响应格式
所有API响应遵循统一格式：
```json
{
  "code": 200,
  "message": "成功",
  "data": {},
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### 认证机制
- **认证头**: `Authorization: Bearer <token>`
- **Token类型**: Sa-Token生成的JWT令牌
- **Token有效期**: 本地30天，测试/生产7天
- **刷新机制**: 基于活跃时间自动延期

### 常用API端点
```bash
# 用户认证
POST /api/open/auth/register        # 用户注册
POST /api/open/auth/login           # 用户登录
POST /api/open/auth/logout          # 用户登出
POST /api/open/auth/send-code       # 发送验证码
POST /api/open/auth/reset-password  # 重置密码

# 用户管理
GET /api/client/user/info           # 获取用户信息
PUT /api/client/user/info           # 更新用户信息
GET /api/client/user/profile        # 获取用户资料

# 文件上传
POST /api/client/file/upload        # 文件上传(七牛云)

# 管理后台
POST /api/admin/auth/login          # 管理员登录
GET /api/admin/user/list            # 用户列表
PUT /api/admin/user/{id}/status     # 更新用户状态

# 系统监控
GET /api/health                     # 健康检查
```