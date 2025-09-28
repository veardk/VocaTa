package com.vocata.character.constants;

/**
 * 角色聊天计数缓存常量配置
 */
public class ChatCountCacheConstants {

    /**
     * Redis键前缀
     */
    public static final String CHAT_COUNT_PREFIX = "vocata:character:chat_count:";
    public static final String CHAT_COUNT_TODAY_PREFIX = "vocata:character:chat_count_today:";
    public static final String CHAT_COUNT_LOCK_PREFIX = "vocata:character:chat_count_lock:";
    public static final String CHAT_COUNT_NULL_PREFIX = "vocata:character:chat_count_null:";

    /**
     * 缓存过期时间（秒）
     */
    public static final long TOTAL_EXPIRE_SECONDS = 86400L; // 24小时
    public static final long TODAY_EXPIRE_SECONDS = 3600L; // 1小时
    public static final long LOCK_EXPIRE_SECONDS = 10L; // 10秒
    public static final long NULL_CACHE_EXPIRE_SECONDS = 300L; // 5分钟
    public static final long RANDOM_EXPIRE_RANGE = 1800L; // 30分钟随机范围

    /**
     * 开发环境配置
     */
    public static class Development {
        public static final long TOTAL_EXPIRE_SECONDS = 86400L;
        public static final long TODAY_EXPIRE_SECONDS = 3600L;
        public static final long LOCK_EXPIRE_SECONDS = 10L;
        public static final long NULL_CACHE_EXPIRE_SECONDS = 300L;
        public static final long RANDOM_EXPIRE_RANGE = 1800L;
        public static final boolean SYNC_ENABLED = true;
        public static final boolean WARMUP_ENABLED = true;
        public static final boolean CLEANUP_ENABLED = true;
        public static final boolean DETAILED_LOGGING = true;
        public static final boolean PERFORMANCE_MONITORING = true;
    }

    /**
     * 测试环境配置
     */
    public static class Test {
        public static final long TOTAL_EXPIRE_SECONDS = 43200L; // 12小时
        public static final long TODAY_EXPIRE_SECONDS = 1800L; // 30分钟
        public static final long LOCK_EXPIRE_SECONDS = 5L;
        public static final long NULL_CACHE_EXPIRE_SECONDS = 180L; // 3分钟
        public static final long RANDOM_EXPIRE_RANGE = 900L; // 15分钟
        public static final boolean SYNC_ENABLED = true;
        public static final boolean WARMUP_ENABLED = true;
        public static final boolean CLEANUP_ENABLED = true;
        public static final boolean DETAILED_LOGGING = false;
        public static final boolean PERFORMANCE_MONITORING = true;
    }

    /**
     * 生产环境配置
     */
    public static class Production {
        public static final long TOTAL_EXPIRE_SECONDS = 172800L; // 48小时
        public static final long TODAY_EXPIRE_SECONDS = 7200L; // 2小时
        public static final long LOCK_EXPIRE_SECONDS = 15L;
        public static final long NULL_CACHE_EXPIRE_SECONDS = 600L; // 10分钟
        public static final long RANDOM_EXPIRE_RANGE = 3600L; // 1小时
        public static final boolean SYNC_ENABLED = true;
        public static final boolean WARMUP_ENABLED = true;
        public static final boolean CLEANUP_ENABLED = true;
        public static final boolean DETAILED_LOGGING = false;
        public static final boolean PERFORMANCE_MONITORING = false;
    }

    /**
     * 定时任务cron表达式
     */
    public static final String SYNC_CRON = "0 0 2 * * ?"; // 每天凌晨2点
    public static final String CLEANUP_CRON = "0 0 * * * ?"; // 每小时
    public static final String WARMUP_CRON = "0 0 3 * * ?"; // 每天凌晨3点

    private ChatCountCacheConstants() {
        // 私有构造函数，防止实例化
    }
}