package com.vocata.common.constant;

/**
 * 系统常量
 */
public class SystemConstants {

    // 系统相关
    public static final String SYSTEM_NAME = "VocaTa";
    public static final String DEFAULT_AVATAR = "/images/default-avatar.png";

    // 分页相关
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // 缓存相关
    public static final String CACHE_PREFIX = "vocata:";
    public static final String USER_CACHE_PREFIX = CACHE_PREFIX + "user:";
    public static final String CHARACTER_CACHE_PREFIX = CACHE_PREFIX + "character:";
    public static final String CONVERSATION_CACHE_PREFIX = CACHE_PREFIX + "conversation:";

    // 缓存过期时间（秒）
    public static final long CACHE_EXPIRE_SECONDS = 3600L; // 1小时
    public static final long USER_CACHE_EXPIRE_SECONDS = 1800L; // 30分钟
    public static final long SHORT_CACHE_EXPIRE_SECONDS = 300L; // 5分钟

    // 用户相关
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 20;
    public static final int MIN_USERNAME_LENGTH = 2;
    public static final int MAX_USERNAME_LENGTH = 20;

    // 文件上传相关
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
    public static final String[] ALLOWED_IMAGE_TYPES = {"jpg", "jpeg", "png", "gif", "webp"};
    public static final String[] ALLOWED_AUDIO_TYPES = {"mp3", "wav", "m4a", "ogg"};

    // AI相关
    public static final int MAX_CONVERSATION_CONTEXT_LENGTH = 4000; // 4K上下文
    public static final int MAX_MESSAGE_LENGTH = 1000;
    public static final int DEFAULT_AI_RESPONSE_TIMEOUT = 30; // 30秒

    // 角色相关
    public static final int MAX_CHARACTER_NAME_LENGTH = 50;
    public static final int MAX_CHARACTER_DESCRIPTION_LENGTH = 500;
    public static final int MAX_SYSTEM_PROMPT_LENGTH = 2000;

    // 操作日志相关
    public static final String LOG_MODULE_USER = "用户模块";
    public static final String LOG_MODULE_CHARACTER = "角色模块";
    public static final String LOG_MODULE_CONVERSATION = "对话模块";
    public static final String LOG_MODULE_ADMIN = "管理模块";

    public static final String LOG_TYPE_QUERY = "查询";
    public static final String LOG_TYPE_CREATE = "创建";
    public static final String LOG_TYPE_UPDATE = "更新";
    public static final String LOG_TYPE_DELETE = "删除";
    public static final String LOG_TYPE_LOGIN = "登录";
    public static final String LOG_TYPE_LOGOUT = "登出";
}