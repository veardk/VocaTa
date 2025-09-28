---
title: Technical Architecture
description: "定义项目的技术栈、架构模式和开发规范。"
inclusion: always
---

# VocaTa技术架构文档

## 技术栈概览

### 后端核心技术
- **Java 17** - 现代Java特性支持
- **Spring Boot 3.1.4** - 企业级微服务框架
- **MyBatis Plus 3.5.3.2** - 高效ORM框架，支持代码生成和分页
- **Sa-Token 1.37.0** - 轻量级权限认证框架
- **PostgreSQL** - 关系型数据库
- **Redis** - 缓存和会话存储
- **Redisson 3.23.4** - Redis分布式锁和数据结构

### 前端核心技术
- **Vue 3 (Composition API)** - 现代前端框架，主要使用组合式API
- **Element Plus** - Vue 3 UI组件库
- **Axios** - HTTP客户端库
- **SCSS** - CSS预处理器
- **Vite** - 现代前端构建工具
- **ESLint + Prettier** - 代码规范和格式化工具
- **Pinia** - Vue 3 状态管理库
- **Vue Router 4** - Vue 3 路由管理

### 开发工具
- **Maven** - 项目构建和依赖管理
- **Docker** - 容器化部署
- **HuTool** - Java工具类库

## 架构设计模式

### 1. 分层架构（Layered Architecture）
```
┌─────────────────────────────────────┐
│           Controller Layer          │ <- REST API端点
├─────────────────────────────────────┤
│            Service Layer            │ <- 业务逻辑层
├─────────────────────────────────────┤
│            Mapper Layer             │ <- 数据访问层
├─────────────────────────────────────┤
│           Database Layer            │ <- PostgreSQL + Redis
└─────────────────────────────────────┘
```

### 2. 模块化设计
每个业务模块遵循标准目录结构：
```
com.vocata.{module}/
├── controller/     # REST端点控制器
├── service/        # 业务逻辑服务层
├── mapper/         # MyBatis数据访问层
├── entity/         # 数据库实体类
└── dto/           # 数据传输对象
```

### 3. 统一响应格式
所有API返回统一的`ApiResponse<T>`格式：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1634567890123
}
```

关于返回ID字段时都需要返回string类型给前端

## 核心架构组件

### 1. 认证授权架构
- **Sa-Token框架**：轻量级JWT认证
- **用户上下文**：`UserContext`线程本地存储
- **权限拦截**：基于路由的权限控制
- **多环境支持**：开发/测试/生产环境隔离

### 2. 数据库架构
- **基础实体**：所有实体继承`BaseEntity`
- **审计字段**：自动填充创建人/时间、更新人/时间
- **逻辑删除**：使用`@TableLogic`软删除
- **ID策略**：雪花算法生成分布式ID
- **命名规范**：表名`tb_`前缀，驼峰转下划线

### 3. 异常处理架构
- **全局异常处理**：`GlobalExceptionHandler`统一处理
- **业务异常**：`BizException`业务逻辑异常
- **错误码管理**：`ApiCode`枚举定义所有错误状态
- **异常响应**：自动转换为统一响应格式

### 4. 配置管理
- **多环境配置**：`application-{profile}.yml`
- **环境变量**：支持`.env`文件和环境变量注入
- **配置优先级**：环境变量 > 配置文件 > 默认值

## 开发和运行命令

### 本地开发
```bash
# 启动后端服务 (端口9009)
cd vocata-server
mvn spring-boot:run

# 指定环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=test

# 构建JAR包
mvn clean package
```

### 容器化部署
```bash
# 构建Docker镜像
docker build -t vocata-server .

# 运行容器
docker run -p 9009:9009 vocata-server
```

## PostgreSQL数据库设计规范

### 企业级设计原则
- **表设计**：全面规范，字段定义清晰，高可维护性和扩展性
- **关联模式**：禁用主外键约束，通过独立关联表实现所有关系
- **字段类型**：禁用ENUM，使用SMALLINT表示枚举值
- **索引策略**：非必要不创建索引，后期根据性能需求手动添加

### 命名规范
- **表名**：`vocata_` + 业务名称（如：`vocata_user`, `vocata_character`）
- **关联表**：以 `_relation` 结尾（如：`vocata_user_character_relation`）
- **字段名**：下划线命名法（如：`create_date`, `user_id`）
- **索引名**：`idx_` + 表名 + 字段名

### PostgreSQL数据类型标准
- **整数**：BIGSERIAL (主键)、BIGINT、INTEGER、SMALLINT
- **字符串**：VARCHAR(n)、TEXT
- **时间**：TIMESTAMP 
- **布尔**：BOOLEAN
- **JSON**：JSONB（高性能JSON存储）
- **数组**：支持PostgreSQL数组类型（如TEXT[]、INTEGER[]）

### 审计字段标准
所有业务表必须包含以下5个审计字段：
```sql
/* 审计字段区域 */
create_id BIGINT NOT NULL,                                    -- 创建人ID
update_id BIGINT,                                            -- 更新人ID
create_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 创建时间
update_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 更新时间
is_delete SMALLINT DEFAULT 0                                 -- 逻辑删除：0.否 1.是
```

### 核心业务表设计
- **vocata_user**：用户基础信息表
- **vocata_character**：AI角色信息表
- **vocata_conversation**：对话会话表
- **vocata_message**：消息记录表
- **vocata_favorite**：收藏功能表
- **vocata_admin**：管理员表
- **vocata_ai_service**：AI服务配置表
- **vocata_search_history**：搜索历史表

### 关联表设计
- **vocata_user_character_relation**：用户角色关联表
- **vocata_character_ai_service_relation**：角色AI服务关联表

### 统计分析表
- **vocata_usage_statistics**：使用统计表
- **vocata_system_log**：系统操作日志表

## API设计规范

### 路由设计
- **客户端API**：`/api/client/**` （部分需认证）
- **管理端API**：`/api/admin/**` （仅管理员）
- **公开API**：`/api/open/**` （无需认证）

### 请求响应格式
- 请求：使用DTO对象，支持JSR-303验证
- 响应：统一`ApiResponse<T>`包装
- 分页：返回`PageResult<T>`格式
- 错误：返回标准错误码和消息

## 测试策略

### 单元测试
- Service层业务逻辑测试
- Mapper层数据访问测试
- 工具类和辅助方法测试

### 集成测试
- Controller层API测试
- 数据库集成测试
- 外部服务集成测试

## 性能和监控

### 缓存策略
- Redis缓存热点数据
- Sa-Token会话缓存
- 数据库查询结果缓存

### 连接池配置
- HikariCP数据库连接池
- Redis连接池优化
- 合理的超时和重试设置

### 日志记录
- 结构化日志输出
- 不同环境的日志级别
- 关键业务操作审计日志