package com.vocata.common.constant;

/**
 * Redis缓存键常量
 */
public class CacheKeys {

    // 用户相关缓存键
    public static final String USER_INFO = "user:info:";
    public static final String USER_PERMISSION = "user:permission:";
    public static final String USER_LOGIN_TOKEN = "user:token:";

    // 角色相关缓存键
    public static final String CHARACTER_INFO = "character:info:";
    public static final String CHARACTER_LIST = "character:list:";
    public static final String CHARACTER_SEARCH = "character:search:";

    // 对话相关缓存键
    public static final String CONVERSATION_INFO = "conversation:info:";
    public static final String CONVERSATION_MESSAGES = "conversation:messages:";
    public static final String CONVERSATION_CONTEXT = "conversation:context:";

    // 收藏相关缓存键
    public static final String USER_FAVORITES = "favorite:user:";

    // 系统配置相关缓存键
    public static final String SYSTEM_CONFIG = "system:config:";
    public static final String AI_CONFIG = "ai:config:";

    // 限流相关缓存键
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";
    public static final String API_RATE_LIMIT = RATE_LIMIT_PREFIX + "api:";
    public static final String USER_RATE_LIMIT = RATE_LIMIT_PREFIX + "user:";

    // 验证码相关缓存键
    public static final String CAPTCHA_PREFIX = "captcha:";
    public static final String SMS_CODE_PREFIX = "sms_code:";
    public static final String EMAIL_CODE_PREFIX = "email_code:";

    /**
     * 构建用户信息缓存键
     */
    public static String getUserInfoKey(Long userId) {
        return USER_INFO + userId;
    }

    /**
     * 构建角色信息缓存键
     */
    public static String getCharacterInfoKey(Long characterId) {
        return CHARACTER_INFO + characterId;
    }

    /**
     * 构建对话信息缓存键
     */
    public static String getConversationInfoKey(Long conversationId) {
        return CONVERSATION_INFO + conversationId;
    }

    /**
     * 构建用户收藏缓存键
     */
    public static String getUserFavoritesKey(Long userId) {
        return USER_FAVORITES + userId;
    }

    /**
     * 构建API限流缓存键
     */
    public static String getApiRateLimitKey(String api, String identifier) {
        return API_RATE_LIMIT + api + ":" + identifier;
    }
}