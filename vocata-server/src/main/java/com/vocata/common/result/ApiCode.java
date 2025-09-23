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

    // 参数验证相关
    VALIDATE_FAILED(1001, "参数验证失败"),

    // 用户相关
    USER_NOT_EXIST(1002, "用户不存在"),
    USER_ALREADY_EXISTS(1003, "用户已存在"),
    USER_PASSWORD_ERROR(1004, "用户名或密码错误"),
    USER_DISABLED(1005, "用户已被禁用"),

    // 角色相关
    CHARACTER_NOT_EXIST(2001, "角色不存在"),
    CHARACTER_ACCESS_DENIED(2002, "无权访问该角色"),

    // 对话相关
    CONVERSATION_NOT_EXIST(3001, "对话不存在"),
    CONVERSATION_ACCESS_DENIED(3002, "无权访问该对话"),

    // 收藏相关
    FAVORITE_ALREADY_EXISTS(4001, "已收藏该内容"),
    FAVORITE_NOT_EXIST(4002, "收藏不存在"),

    // AI服务相关
    AI_SERVICE_ERROR(5001, "AI服务异常"),
    AI_SERVICE_UNAVAILABLE(5002, "AI服务不可用"),

    // 文件相关
    FILE_UPLOAD_FAILED(6001, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORTED(6002, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(6003, "文件大小超出限制");

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