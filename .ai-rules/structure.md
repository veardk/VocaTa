---
title: Project Structure
description: "定义项目的目录结构、文件组织和命名约定。"
inclusion: always
---

# VocaTa项目结构规范

## 项目根目录结构

```
VocaTa/
├── .ai-rules/              # AI代理指导文件
│   ├── product.md         # 产品愿景文档
│   ├── tech.md           # 技术架构文档
│   └── structure.md      # 项目结构文档
├── .gitignore            # Git忽略文件配置
├── CLAUDE.md             # Claude Code工作指导
├── README.md             # 项目说明文档
├── vocata-server/        # 后端服务模块
├── vocata-web/           # 客户端前端（规划中）
└── vocata-admin/         # 管理端前端（规划中）
```

## 后端模块结构（vocata-server）

### 顶层目录
```
vocata-server/
├── src/main/java/        # Java源代码
├── src/main/resources/   # 配置和资源文件
├── src/test/java/        # 测试代码
├── target/               # Maven构建输出
├── .idea/                # IntelliJ IDEA配置
├── pom.xml               # Maven项目配置
├── Dockerfile            # Docker构建文件
└── .env                  # 环境变量配置
```

### Java源码包结构
```
src/main/java/com/vocata/
├── VocataServerApplication.java    # Spring Boot启动类
├── common/                         # 公共组件包
│   ├── constant/                  # 常量定义
│   ├── entity/                    # 基础实体类
│   │   └── BaseEntity.java       # 所有实体继承的基类
│   ├── exception/                 # 异常处理
│   │   ├── BizException.java     # 业务异常
│   │   └── GlobalExceptionHandler.java
│   ├── result/                    # 响应结果封装
│   │   ├── ApiResponse.java      # 统一API响应
│   │   ├── ApiCode.java          # 错误码枚举
│   │   └── PageResult.java       # 分页结果
│   └── utils/                     # 工具类
│       ├── UserContext.java      # 用户上下文工具
│       └── IpUtils.java          # IP工具类
├── config/                        # 配置类包
│   ├── MybatisPlusConfig.java    # MyBatis Plus配置
│   ├── SaTokenConfig.java        # Sa-Token配置
│   ├── RedisConfig.java          # Redis配置
│   └── WebConfig.java            # Web配置
├── auth/                          # 认证模块
│   ├── controller/               # 认证控制器
│   │   └── AuthController.java
│   ├── service/                  # 认证服务
│   │   ├── AuthService.java
│   │   └── impl/
│   ├── dto/                      # 认证数据传输对象
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   └── RegisterRequest.java
│   └── constants/                # 认证相关常量
├── user/                          # 用户模块
│   ├── controller/               # 用户控制器
│   │   └── UserController.java
│   ├── service/                  # 用户服务
│   │   ├── UserService.java
│   │   └── impl/
│   ├── mapper/                   # 用户数据访问
│   │   └── UserMapper.java
│   ├── entity/                   # 用户实体
│   │   └── User.java
│   └── dto/                      # 用户数据传输对象
├── character/                     # 角色模块
│   ├── controller/               # 角色控制器
│   ├── service/                  # 角色服务
│   ├── mapper/                   # 角色数据访问
│   ├── entity/                   # 角色实体
│   └── dto/                      # 角色数据传输对象
├── conversation/                  # 对话模块
│   ├── controller/               # 对话控制器
│   ├── service/                  # 对话服务
│   ├── mapper/                   # 对话数据访问
│   ├── entity/                   # 对话实体
│   │   ├── Conversation.java    # 对话会话
│   │   └── Message.java         # 消息记录
│   └── dto/                      # 对话数据传输对象
├── favorite/                      # 收藏模块
│   ├── controller/               # 收藏控制器
│   ├── service/                  # 收藏服务
│   ├── mapper/                   # 收藏数据访问
│   └── entity/                   # 收藏实体
├── search/                        # 搜索模块
│   ├── controller/               # 搜索控制器
│   └── service/                  # 搜索服务
├── ai/                           # AI集成模块
│   ├── client/                   # AI客户端
│   └── service/                  # AI服务
└── admin/                        # 管理功能模块
    ├── controller/               # 管理控制器
    └── service/                  # 管理服务
```

### 资源文件结构
```
src/main/resources/
├── application.yml               # 默认配置（本地开发）
├── application-test.yml          # 测试环境配置
├── application-prod.yml          # 生产环境配置
├── logback-spring.xml           # 日志配置
├── static/                      # 静态资源
├── templates/                   # 模板文件
├── mapper/                      # MyBatis XML映射文件
│   ├── user/
│   ├── character/
│   └── conversation/
└── db/                          # 数据库相关
    └── migration/               # SQL迁移脚本
        ├── V1__init_schema.sql
        └── V2__add_character_tables.sql
```

## 模块设计原则

### 1. 业务模块独立性
- 每个业务模块（user、character、conversation等）保持相对独立
- 模块间通过service接口进行交互，避免直接依赖
- 公共功能放在common包中供各模块使用

### 2. 标准目录结构
每个业务模块必须包含以下标准目录：
- `controller/` - REST API控制器
- `service/` - 业务逻辑服务层
- `mapper/` - MyBatis数据访问层
- `entity/` - 数据库实体类
- `dto/` - 数据传输对象

### 3. 分层架构约束
- Controller层：只处理HTTP请求响应，不包含业务逻辑
- Service层：核心业务逻辑，可调用其他Service和Mapper
- Mapper层：纯数据访问，不包含业务逻辑
- Entity层：数据库实体，继承BaseEntity
- DTO层：API输入输出对象，与Entity分离

## 命名约定

### 包命名
- 全小写，使用点分隔：`com.vocata.module.layer`
- 模块名使用单数形式：`user`、`character`、`conversation`
- 层级名使用复数形式：`controllers`、`services`、`mappers`

关于返回ID字段时都需要返回string类型给前端。

### 类命名
- 使用PascalCase（大驼峰）
- Controller：`{Module}Controller.java`
- Service接口：`{Module}Service.java`
- Service实现：`{Module}ServiceImpl.java`
- Mapper：`{Module}Mapper.java`
- Entity：`{Module}.java`（业务实体名）
- DTO：根据用途命名，如`LoginRequest.java`、`UserResponse.java`

### 方法命名
- 使用camelCase（小驼峰）
- CRUD操作：`create`、`get`、`update`、`delete`
- 查询方法：`findBy{Condition}`、`listBy{Condition}`
- 业务方法：动词开头，语义明确

### 常量命名
- 全大写，下划线分隔：`MAX_RETRY_COUNT`
- 按功能分组，放在对应的Constants类中

## 配置文件组织

### 环境配置分离
- `application.yml` - 本地开发环境（端口9009）
- `application-test.yml` - 测试环境
- `application-prod.yml` - 生产环境

### 配置优先级
1. 环境变量（最高优先级）
2. `.env`文件
3. `application-{profile}.yml`
4. `application.yml`（默认配置）

## 数据库架构规范

### PostgreSQL表设计规范
- **表命名**：使用`vocata_`前缀，如`vocata_user`、`vocata_character`、`vocata_conversation`
- **关联表**：以`_relation`结尾，如`vocata_user_character_relation`
- **字段命名**：下划线命名法，如`create_date`、`user_id`
- **主键**：统一使用`id` (BIGSERIAL)
- **外键**：禁用物理外键，通过关联表实现关系
- **枚举值**：禁用ENUM，使用SMALLINT + 注释
- **审计字段**：每表包含create_id、update_id、create_date、update_date、is_delete

### 数据库文件结构
```
src/main/resources/db/
├── migration/                   # 数据库迁移脚本
│   ├── V1__init_core_tables.sql      # 核心表初始化
│   ├── V2__create_user_tables.sql    # 用户相关表
│   ├── V3__create_character_tables.sql # 角色相关表
│   ├── V4__create_conversation_tables.sql # 对话相关表
│   ├── V5__create_relation_tables.sql # 关联表
│   ├── V6__create_admin_tables.sql   # 管理表
│   ├── V7__create_ai_service_tables.sql # AI服务表
│   └── V8__create_statistics_tables.sql # 统计表
├── data/                        # 初始化数据
│   ├── init_admin_user.sql     # 管理员初始化
│   ├── init_character_data.sql # 角色初始化数据
│   └── init_ai_service_config.sql # AI服务初始化
└── schema/                      # 完整数据库结构
    └── vocata_schema.sql       # 完整建表脚本
```

## 前端结构规范

### 客户端前端 (vocata-web)
```
vocata-web/
├── public/                 # 静态资源目录
│   ├── index.html         # HTML入口模板
│   ├── favicon.ico        # 网站图标
│   └── assets/            # 公共静态资源
├── src/                   # 源码目录
│   ├── api/               # API请求相关
│   │   ├── index.js       # API入口，集中导出所有接口
│   │   ├── request.js     # Axios实例配置
│   │   └── modules/       # 按业务模块划分的API
│   │       ├── user.js    # 用户相关API
│   │       ├── character.js # 角色相关API
│   │       ├── conversation.js # 对话相关API
│   │       └── favorite.js # 收藏相关API
│   ├── assets/            # 静态资源
│   │   ├── images/        # 图片资源
│   │   ├── styles/        # 全局样式
│   │   │   ├── index.scss # 样式入口文件
│   │   │   ├── variables.scss # 全局变量
│   │   │   ├── mixins.scss # 混合样式
│   │   │   └── components/ # 组件样式
│   │   └── icons/         # 图标资源
│   ├── components/        # 公共组件
│   │   ├── common/        # 通用基础组件
│   │   │   ├── BaseButton.vue
│   │   │   ├── BaseModal.vue
│   │   │   └── BaseTable.vue
│   │   └── business/      # 业务组件
│   │       ├── UserAvatar.vue
│   │       ├── CharacterCard.vue
│   │       └── ConversationItem.vue
│   ├── composables/       # 可复用的组合式函数
│   │   ├── useAuth.js     # 认证相关
│   │   ├── useApi.js      # API调用
│   │   └── useStorage.js  # 本地存储
│   ├── config/            # 配置文件
│   │   ├── constants.js   # 常量定义
│   │   └── env.js         # 环境配置
│   ├── hooks/             # 自定义钩子
│   │   └── usePermission.js # 权限钩子
│   ├── layouts/           # 布局组件
│   │   ├── MainLayout.vue # 主布局
│   │   └── AuthLayout.vue # 认证布局
│   ├── router/            # 路由相关
│   │   ├── index.js       # 路由入口
│   │   ├── routes.js      # 路由配置
│   │   └── guards.js      # 路由守卫
│   ├── store/             # 状态管理(Pinia)
│   │   ├── index.js       # Store入口
│   │   └── modules/       # 按模块划分的状态
│   │       ├── user.js    # 用户状态
│   │       ├── character.js # 角色状态
│   │       └── conversation.js # 对话状态
│   ├── utils/             # 工具函数
│   │   ├── auth.js        # 认证工具
│   │   ├── storage.js     # 存储工具
│   │   ├── format.js      # 格式化工具
│   │   └── validation.js  # 验证工具
│   ├── views/             # 页面组件
│   │   ├── Home/          # 首页
│   │   │   └── index.vue
│   │   ├── User/          # 用户相关页面
│   │   │   ├── Login.vue  # 登录页
│   │   │   ├── Register.vue # 注册页
│   │   │   └── Profile.vue # 个人中心
│   │   ├── Character/     # 角色相关页面
│   │   │   ├── List.vue   # 角色列表
│   │   │   └── Detail.vue # 角色详情
│   │   ├── Conversation/  # 对话相关页面
│   │   │   ├── List.vue   # 对话列表
│   │   │   └── Chat.vue   # 对话界面
│   │   └── NotFound.vue   # 404页面
│   ├── App.vue            # 根组件
│   ├── main.js            # 入口文件
│   └── vite-env.d.ts      # Vite类型声明
├── .env                   # 环境变量配置
├── .env.development       # 开发环境配置
├── .env.production        # 生产环境配置
├── .eslintrc.js          # ESLint配置
├── .prettierrc           # Prettier配置
├── package.json          # 项目依赖
├── vite.config.js        # Vite配置
└── README.md             # 项目说明
```

### 管理端前端 (vocata-admin)
```
vocata-admin/
├── public/               # 静态资源目录
├── src/                  # 源码目录
│   ├── api/              # 管理API
│   │   └── modules/
│   │       ├── admin.js  # 管理员API
│   │       ├── user.js   # 用户管理API
│   │       └── character.js # 角色管理API
│   ├── components/       # 管理组件
│   │   ├── common/       # 通用组件
│   │   └── admin/        # 管理专用组件
│   ├── layouts/          # 管理布局
│   │   └── AdminLayout.vue
│   ├── views/            # 管理页面
│   │   ├── Dashboard/    # 仪表板
│   │   ├── UserManage/   # 用户管理
│   │   ├── CharacterManage/ # 角色管理
│   │   └── SystemConfig/ # 系统配置
│   └── ...
└── package.json
```