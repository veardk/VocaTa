package com.vocata.common.result;

/**
 * API状态码枚举
 */
public enum ApiCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "请求的资源不存在"),
    ERROR(500, "系统内部错误"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    // 通用错误码 (1000-1099)
    PARAM_ERROR(1001, "请求参数错误"),
    INVALID_PARAM(1001, "参数无效"),
    VALIDATE_FAILED(1002, "参数验证失败"),
    DATA_NOT_FOUND(1003, "数据不存在"),
    DATA_ALREADY_EXISTS(1004, "数据已存在"),
    ACCESS_DENIED(1005, "权限不足"),
    OPERATION_FAILED(1006, "操作失败"),
    
    // 用户相关 (1100-1199)
    USER_NOT_EXIST(1100, "用户不存在"),
    USER_ALREADY_EXISTS(1101, "用户已存在"),
    USER_PASSWORD_ERROR(1102, "用户名或密码错误"),
    USER_DISABLED(1103, "用户已被禁用"),
    USER_NOT_LOGIN(1104, "用户未登录"),
    USER_TOKEN_EXPIRED(1105, "用户登录已过期"),
    // 角色相关 (2000-2099)
    CHARACTER_NOT_EXIST(2001, "角色不存在"),
    CHARACTER_ACCESS_DENIED(2002, "无权访问该角色"),
    CHARACTER_CODE_ALREADY_EXISTS(2003, "角色编码已存在"),
    CHARACTER_NAME_ALREADY_EXISTS(2004, "角色名称已存在"),
    CHARACTER_STATUS_INVALID(2005, "角色状态无效"),
    CHARACTER_CREATE_FAILED(2006, "角色创建失败"),
    CHARACTER_UPDATE_FAILED(2007, "角色更新失败"),
    CHARACTER_DELETE_FAILED(2008, "角色删除失败"),
    CHARACTER_NOT_PUBLISHED(2009, "角色未发布"),
    CHARACTER_UNDER_REVIEW(2010, "角色审核中"),
    CHARACTER_REJECTED(2011, "角色已被拒绝"),
    CHARACTER_BANNED(2012, "角色已被封禁"),
    CHARACTER_PERMISSION_DENIED(2013, "无权限操作该角色"),
    CHARACTER_OWNER_ONLY(2014, "仅角色创建者可执行此操作"),
    CHARACTER_ADMIN_ONLY(2015, "仅管理员可执行此操作"),

    // 对话相关 (3000-3099)
    CONVERSATION_NOT_EXIST(3001, "对话不存在"),
    CONVERSATION_ACCESS_DENIED(3002, "无权访问该对话"),

    // 收藏相关 (4000-4099)
    FAVORITE_ALREADY_EXISTS(4001, "已收藏该内容"),
    FAVORITE_NOT_EXIST(4002, "收藏不存在"),

    // AI服务相关 (5000-5099)
    AI_SERVICE_ERROR(5001, "AI服务异常"),
    AI_SERVICE_UNAVAILABLE(5002, "AI服务不可用"),

    // 文件相关 (6000-6099)
    FILE_UPLOAD_FAILED(6001, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORTED(6002, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(6003, "文件大小超出限制"),
    FILE_EMPTY(6004, "文件不能为空"),
    FILE_TYPE_NOT_ALLOWED(6005, "文件类型不允许");

    private final Integer code;
    private final String message;

    ApiCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
