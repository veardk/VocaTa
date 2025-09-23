# VocaTa Server

VocaTa AI角色扮演平台后端服务

## 技术栈

- Spring Boot 3.1.4
- Java 17
- MyBatis Plus 3.5.3.2
- Sa-Token 1.37.0 (JWT认证)
- Redis (缓存)
- PostgreSQL (数据库)

## 项目结构

```
src/main/java/com/vocata/
├── VocataApplication.java              # 启动类
├── config/                             # 配置层
│   ├── SaTokenConfig.java
│   ├── MybatisPlusConfig.java
│   ├── WebConfig.java
│   └── RedisConfig.java
├── common/                             # 公共层
│   ├── result/                         # 统一返回
│   │   ├── ApiResponse.java
│   │   ├── ApiCode.java
│   │   └── PageResult.java
│   ├── exception/                      # 异常处理
│   │   ├── BizException.java
│   │   └── GlobalExceptionHandler.java
│   ├── utils/                          # 工具类
│   │   ├── UserContext.java
│   │   ├── IpUtils.java
│   │   └── DeviceIdUtils.java
│   ├── constant/                       # 常量
│   │   ├── SystemConstants.java
│   │   └── CacheKeys.java
│   └── entity/                         # 基础实体
│       └── BaseEntity.java
├── auth/                               # 认证模块
│   ├── controller/
│   ├── service/
│   └── dto/
├── user/                               # 用户模块
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── dto/
├── character/                          # 角色模块
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── dto/
├── conversation/                       # 对话模块
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── dto/
├── favorite/                           # 收藏模块
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   └── entity/
├── ai/                                 # AI集成
│   ├── service/
│   └── client/
├── search/                             # 搜索模块
│   ├── controller/
│   └── service/
└── admin/                              # 管理后台
    ├── controller/
    └── service/
```

## 核心功能

- ✅ 用户认证（注册/登录/登出）
- ✅ 统一返回格式和异常处理
- ✅ 权限控制和用户上下文
- ✅ 基础实体和自动填充
- ✅ 数据库初始化和种子数据
- 🚧 角色管理和搜索
- 🚧 对话功能（文本/语音）
- 🚧 AI集成和角色扮演
- 🚧 收藏和个人中心
- 🚧 管理后台

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Redis 6+

### 1. 数据库准备

```sql
-- 创建数据库
CREATE DATABASE vocata_dev;

-- 创建用户
CREATE USER vocata_dev WITH PASSWORD 'vocata_dev';
GRANT ALL PRIVILEGES ON DATABASE vocata_dev TO vocata_dev;
```

### 2. 配置文件

复制 `application-dev.yml` 并修改数据库连接配置：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vocata_dev
    username: vocata_dev
    password: vocata_dev
```

### 3. 启动应用

```bash
# 安装依赖
mvn clean install

# 启动应用
mvn spring-boot:run
```

### 4. 验证启动

访问 http://localhost:8080，应用启动成功。

## API接口

### 认证接口

```bash
# 注册
POST /api/client/auth/register
Content-Type: application/json

{
  "username": "test",
  "password": "123456",
  "email": "test@example.com",
  "nickname": "测试用户"
}

# 登录
POST /api/client/auth/login
Content-Type: application/json

{
  "username": "test",
  "password": "123456"
}

# 登出
POST /api/client/auth/logout
Authorization: Bearer <token>
```

### 用户接口

```bash
# 获取当前用户信息
GET /api/client/user/info
Authorization: Bearer <token>

# 更新用户信息
PUT /api/client/user/info
Authorization: Bearer <token>
Content-Type: application/json

{
  "nickname": "新昵称",
  "bio": "个人简介"
}
```

## 开发规范

### API返回格式

所有API必须返回统一格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1672531200000
}
```

### 异常处理

业务异常使用 `BizException`：

```java
if (user == null) {
    throw new BizException(ApiCode.USER_NOT_EXIST);
}
```

### 权限控制

使用 `UserContext` 获取当前用户：

```java
Long userId = UserContext.getUserId();
UserContext.checkAdmin(); // 检查管理员权限
```

### 数据库实体

继承 `BaseEntity` 获得审计字段：

```java
@TableName("tb_example")
public class Example extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    // 其他字段...
}
```

## 部署说明

### 生产环境配置

1. 修改 `application-prod.yml`
2. 设置环境变量
3. 使用外部配置文件

### Docker部署

```bash
# 构建镜像
docker build -t vocata-server .

# 运行容器
docker run -d \
  --name vocata-server \
  -p 8080:8080 \
  -e DB_HOST=localhost \
  -e DB_USERNAME=vocata \
  -e DB_PASSWORD=password \
  vocata-server
```

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交变更
4. 创建 Pull Request

## License

MIT License