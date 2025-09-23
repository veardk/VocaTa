---
title: VocaTa后端开发规范
description: "VocaTa AI角色扮演平台后端开发的完整规范指南，包括架构模式、编码规范、API设计、数据库操作等"
inclusion: always
---

# VocaTa后端开发规范

## 1. 项目架构总览

### 1.1 技术栈规范

**核心技术栈**：
- Spring Boot 3.1.4 + Java 17
- MyBatis Plus 3.5.3.2 (ORM框架)
- Sa-Token 1.37.0 (认证授权)
- PostgreSQL 42.6.0 (主数据库)
- Redis (Redisson 3.23.4) (缓存和会话)
- Hutool 5.8.22 (工具库)

**主要依赖**：
- Spring Boot Validation (参数校验)
- Spring Boot AOP (切面编程)
- Spring Dotenv 4.0.0 (环境变量)
- Maven (构建工具，使用阿里云镜像)

### 1.2 项目模块结构

```
vocata-server/src/main/java/com/vocata/
├── VocataApplication.java          # 应用启动类
├── config/                         # 配置模块
│   ├── SaTokenConfig.java         # 安全配置
│   ├── MybatisPlusConfig.java     # 数据库配置
│   ├── RedisConfig.java           # Redis配置
│   └── WebConfig.java             # Web配置
├── common/                         # 通用组件模块
│   ├── entity/BaseEntity.java     # 基础实体类
│   ├── result/                     # 响应结果封装
│   ├── utils/                      # 工具类
│   ├── constant/                   # 常量定义
│   └── exception/                  # 异常处理
├── auth/                          # 认证授权模块
├── user/                          # 用户管理模块
├── character/                     # 角色管理模块
├── conversation/                  # 对话管理模块
├── favorite/                      # 收藏功能模块
├── admin/                         # 管理后台模块
├── ai/                           # AI集成模块
└── search/                       # 搜索功能模块
```

## 2. 架构设计规范

### 2.1 分层架构模式

每个业务模块必须遵循标准分层结构：

```
{module}/
├── controller/                    # 控制层 - REST API端点
├── service/                       # 业务逻辑接口
│   └── impl/                      # 业务逻辑实现
├── mapper/                        # 数据访问层
├── entity/                        # 数据库实体类
├── dto/                          # 数据传输对象
│   ├── request/                   # 请求DTO
│   └── response/                  # 响应DTO
└── constants/                     # 模块常量定义
```

### 2.2 统一响应格式

**强制要求**：所有Controller方法必须返回`ApiResponse<T>`格式：

```java
@GetMapping("/api/client/user/info")
public ApiResponse<UserInfoResponse> getCurrentUserInfo() {
    UserInfoResponse userInfo = userService.getCurrentUserInfo();
    return ApiResponse.success(userInfo);
}
```

**响应格式规范**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { /* 业务数据 */ },
  "timestamp": 1650000000000
}
```

### 2.3 异常处理架构

**业务异常规范**：
- 业务异常：使用`BizException`并配合`ApiCode`枚举
- 全局处理：`GlobalExceptionHandler`自动转换为统一响应格式
- 异常传播：底层异常必须包装为业务异常向上传递

```java
// 抛出业务异常
throw new BizException(ApiCode.USER_NOT_EXIST);

// ApiCode枚举定义
USER_NOT_EXIST(1002, "用户不存在")
```

### 2.4 认证授权架构

**Sa-Token集成规范**：
- 用户上下文：`UserContext.getUserId()` / `UserContext.checkAdmin()`
- 路由保护：
  - `/api/client/**` - 客户端API (部分需要认证)
  - `/api/admin/**` - 管理员专用API
  - `/api/open/**` - 完全开放API

**权限控制示例**：
```java
// 控制器方法中获取当前用户
Long currentUserId = UserContext.getUserId();

// 管理员权限检查
UserContext.checkAdmin();
```

## 3. 数据访问规范

### 3.1 实体类设计规范

**基础实体要求**：
- 所有实体类必须继承`BaseEntity`
- 使用`@TableName("vocata_prefix")`指定表名
- 主键策略：`@TableId(type = IdType.ASSIGN_ID)` (雪花ID)
- 逻辑删除：`@TableLogic`在`isDelete`字段

```java
@TableName("vocata_user")
public class User extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;
    // 其他字段...
}
```

**审计字段规范**：
```java
public class BaseEntity {
    @TableField(fill = FieldFill.INSERT, value = "create_id")
    private Long createId;

    @TableField(fill = FieldFill.INSERT, value = "create_date")
    private LocalDateTime createDate;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "update_id")
    private Long updateId;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "update_date")
    private LocalDateTime updateDate;

    @TableLogic
    @TableField(value = "is_delete")
    private Integer isDelete;
}
```

### 3.2 MyBatis Plus使用规范

**查询方式选择**：
- 简单查询：直接使用MyBatis Plus内置方法
- 单行SQL：使用`@Select`、`@Insert`等注解
- 复杂查询：使用XML映射文件

**XML映射规范**：
- 遵循"单一职责原则"，每个方法一个明确功能
- 返回专门的DTO对象，避免复杂嵌套
- 优先使用数据库索引提升性能

**分页查询规范**：
```java
// Service层分页查询
public PageResult<UserResponse> getUsers(PageRequest pageRequest) {
    Page<User> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
    Page<User> userPage = userMapper.selectPage(page, null);
    return PageResult.of(userPage, UserResponse::fromEntity);
}
```

### 3.3 数据库设计规范

**表命名规范**：
- 使用`vocata_`前缀：`vocata_user`, `vocata_character`
- 关联表以`_relation`结尾：`vocata_user_character_relation`
- 禁用ENUM类型，使用SMALLINT表示枚举

**字段设计规范**：
- 主键：`BIGSERIAL PRIMARY KEY`
- 时间字段：`TIMESTAMP WITH TIME ZONE`
- JSON数据：使用`JSONB`类型
- 外键：禁用物理外键，通过关联表实现

**审计字段标准**：
```sql
create_id BIGINT NOT NULL,           -- 创建人ID
update_id BIGINT,                    -- 更新人ID
create_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_delete SMALLINT DEFAULT 0         -- 软删除标记 0.否 1.是
```

## 4. API接口设计规范

### 4.1 RESTful API规范
`/api` 前缀已经配置，创建路由时可省略
如`/api/client/{resource}`可写为 `/client/{resource}/xxx`

**URL命名规范**：
```
/api/[模块]/[资源集合(复数)]/[资源标识]/[子资源集合(复数)]
```

**路由分类**：
- 客户端接口：`/api/client/{resource}`
- 管理端接口：`/api/admin/{resource}`
- 开放接口：`/api/open/{resource}`

**HTTP方法使用**：
- GET：获取资源 (安全、幂等)
- POST：创建资源 (非幂等)
- PUT：全量更新资源 (幂等)
- PATCH：部分更新资源 (幂等)
- DELETE：删除资源 (幂等)

### 4.2 参数校验规范

**请求参数校验**：
- 所有Request DTO必须使用`@Valid`注解
- 使用JSR-303注解进行字段校验
- 必须校验所有必填字段和业务约束

```java
public class UpdateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Length(min = 2, max = 20, message = "用户名长度应为2-20个字符")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;
}
```

### 4.3 响应设计规范

**分页响应格式**：
```java
public class PageResult<T> {
    private Integer pageNum;        // 当前页码
    private Integer pageSize;       // 每页大小
    private Long total;             // 总记录数
    private Integer totalPages;     // 总页数
    private List<T> list;          // 数据列表
    private Boolean hasNext;        // 是否有下一页
    private Boolean hasPrevious;    // 是否有上一页
}
```

**统一响应格式**：

统一响应格式
所有API返回统一的`ApiResponse<T>`格式：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1634567890123
}
```

## 5. 编码规范

### 5.1 命名规范

**类命名规范**：
- Controller：`{业务名}Controller.java`
- Service接口：`{业务名}Service.java`
- Service实现：`{业务名}ServiceImpl.java`
- 请求DTO：`{操作}{业务名}Request.java`
- 响应DTO：`{业务名}{内容}Response.java`
- 基础信息类：`{业务名}BasicInfo.java`

**变量命名规范**：
- 使用camelCase驼峰命名
- 常量使用UPPER_SNAKE_CASE
- 包名全小写，使用点分隔

### 5.2 对象映射规范

**禁止使用Lombok**：
- 手动编写getter/setter方法
- 实体转DTO使用手动映射：

```java
public static UserInfoResponse fromEntity(User user) {
    UserInfoResponse response = new UserInfoResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());
    // 其他字段映射...
    return response;
}
```

### 5.3 异常处理规范

**异常层级规范**：
- 业务异常：继承`BizException`
- 每个错误场景必须有明确错误码
- 异常信息不得暴露敏感信息

```java
// 业务异常示例
public void validateUser(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
        throw new BizException(ApiCode.USER_NOT_EXIST);
    }
}
```

## 6. 日志与监控规范

### 6.1 日志规范

**日志级别使用**：
- INFO：接口入口、出口，重要业务操作
- DEBUG：业务逻辑关键点，开发调试信息
- WARN：可恢复的异常情况
- ERROR：系统错误，异常情况

**日志格式规范**：
```java
private static final Logger log = LoggerFactory.getLogger(UserController.class);

@GetMapping("/api/client/user/info")
public ApiResponse<UserInfoResponse> getCurrentUserInfo() {
    log.info("获取当前用户信息");
    try {
        UserInfoResponse userInfo = userService.getCurrentUserInfo();
        log.info("获取用户信息成功, 用户ID: {}", userInfo.getId());
        return ApiResponse.success(userInfo);
    } catch (Exception e) {
        log.error("获取用户信息失败", e);
        throw e;
    }
}
```

### 6.2 操作日志规范

**切面日志使用**：
```java
@OperationLog(module = "用户管理", operation = "更新用户信息")
@PutMapping("/api/client/user/info")
public ApiResponse<Void> updateUserInfo(@Valid @RequestBody UpdateUserRequest request) {
    // 业务逻辑
}
```

## 7. 性能优化规范

### 7.1 查询优化

**分页处理**：
- 大量数据查询必须分页
- 使用MyBatis Plus分页插件
- 合理设置页面大小限制

**索引使用**：
- 优先使用数据库表结构的索引
- 复杂查询优化SQL语句
- 避免全表扫描

```


## 8. 环境配置规范

### 8.1 多环境配置

**配置文件管理**：
- 本地开发：`application.yml` + `.env`
- 测试环境：`application-test.yml`
- 生产环境：`application-prod.yml`

**环境变量规范**：
- 敏感信息通过环境变量配置
- 使用`.env.example`作为模板
- 生产环境禁止硬编码密码

### 8.2 数据库连接配置

**连接池配置**：
- 本地：基础配置，适合开发调试
- 测试：中等负载配置
- 生产：高并发优化配置

```yaml
# 生产环境示例
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
```

## 9. 代码质量规范

### 9.1 代码风格

**编码规范**：
- 遵循Java编码规范
- 使用合适的注释说明复杂逻辑
- 方法保持单一职责，避免过长
- 适当使用设计模式

### 9.2 单元测试

**测试覆盖要求**：
- 核心业务逻辑必须有单元测试
- 测试用例考虑边界条件和异常情况
- Service层测试覆盖率不低于80%

### 9.3 代码审查

**提交前检查**：
- 进行自我代码审查
- 确保编译无错误、无警告
- 遵循团队约定的代码审查标准
- 确保日志输出合理

## 10. 安全规范

### 10.1 数据安全

**敏感数据处理**：
- 密码字段加密存储
- 个人信息脱敏展示
- SQL注入防护
- XSS攻击防护

### 10.2 接口安全

**访问控制**：
- 合理使用Sa-Token权限控制
- 敏感操作增加二次验证
- 限制API调用频率
- 记录关键操作日志

## 11. 部署与运维规范

### 11.1 构建规范

**Maven构建**：
```bash
# 本地开发
mvn spring-boot:run

# 构建JAR包
mvn clean package

# 测试环境
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### 11.2 监控规范

**应用监控**：
- 接口响应时间监控
- 异常率监控
- 数据库连接池监控
- Redis连接状态监控

## 12. 开发工作流程

### 12.1 开发环境搭建

1. 复制`.env.example`为`.env`并配置环境变量
2. 启动PostgreSQL和Redis服务
3. 创建`vocata_local`数据库
4. 运行`mvn spring-boot:run`启动服务
5. 访问http://localhost:9009/api验证服务

### 12.2 功能开发流程

1. **需求分析**：明确功能需求和接口设计
2. **数据库设计**：设计表结构和索引
3. **实体创建**：创建Entity和相关DTO
4. **数据访问层**：编写Mapper接口和XML
5. **业务逻辑层**：实现Service接口和业务逻辑
6. **控制层**：编写Controller和API接口
7. **测试验证**：单元测试和接口测试
8. **代码审查**：自我审查和团队审查

这份规范文档将指导VocaTa项目的后端开发，确保代码质量、架构一致性和开发效率。所有开发者必须严格遵循此规范进行开发。