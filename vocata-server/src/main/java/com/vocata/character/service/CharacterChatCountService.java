package com.vocata.character.service;

import cn.hutool.core.util.StrUtil;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.character.constants.ChatCountCacheConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 角色聊天计数缓存服务
 *
 * 实现功能：
 * 1. Redis缓存聊天计数，实时更新
 * 2. 定时同步数据库（每天2点）
 * 3. 项目启动时预热缓存
 * 4. 防缓存穿透、击穿、雪崩
 */
@Service
public class CharacterChatCountService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterChatCountService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CharacterMapper characterMapper;

    @Autowired
    private Environment environment;

    // 根据环境获取配置
    private long getTotalExpireSeconds() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0];
            switch (profile) {
                case "test":
                    return ChatCountCacheConstants.Test.TOTAL_EXPIRE_SECONDS;
                case "prod":
                    return ChatCountCacheConstants.Production.TOTAL_EXPIRE_SECONDS;
                default:
                    return ChatCountCacheConstants.Development.TOTAL_EXPIRE_SECONDS;
            }
        }
        return ChatCountCacheConstants.Development.TOTAL_EXPIRE_SECONDS;
    }

    private long getTodayExpireSeconds() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0];
            switch (profile) {
                case "test":
                    return ChatCountCacheConstants.Test.TODAY_EXPIRE_SECONDS;
                case "prod":
                    return ChatCountCacheConstants.Production.TODAY_EXPIRE_SECONDS;
                default:
                    return ChatCountCacheConstants.Development.TODAY_EXPIRE_SECONDS;
            }
        }
        return ChatCountCacheConstants.Development.TODAY_EXPIRE_SECONDS;
    }

    private long getLockExpireSeconds() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0];
            switch (profile) {
                case "test":
                    return ChatCountCacheConstants.Test.LOCK_EXPIRE_SECONDS;
                case "prod":
                    return ChatCountCacheConstants.Production.LOCK_EXPIRE_SECONDS;
                default:
                    return ChatCountCacheConstants.Development.LOCK_EXPIRE_SECONDS;
            }
        }
        return ChatCountCacheConstants.Development.LOCK_EXPIRE_SECONDS;
    }

    private long getNullCacheExpireSeconds() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0];
            switch (profile) {
                case "test":
                    return ChatCountCacheConstants.Test.NULL_CACHE_EXPIRE_SECONDS;
                case "prod":
                    return ChatCountCacheConstants.Production.NULL_CACHE_EXPIRE_SECONDS;
                default:
                    return ChatCountCacheConstants.Development.NULL_CACHE_EXPIRE_SECONDS;
            }
        }
        return ChatCountCacheConstants.Development.NULL_CACHE_EXPIRE_SECONDS;
    }

    private long getRandomExpireRange() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0];
            switch (profile) {
                case "test":
                    return ChatCountCacheConstants.Test.RANDOM_EXPIRE_RANGE;
                case "prod":
                    return ChatCountCacheConstants.Production.RANDOM_EXPIRE_RANGE;
                default:
                    return ChatCountCacheConstants.Development.RANDOM_EXPIRE_RANGE;
            }
        }
        return ChatCountCacheConstants.Development.RANDOM_EXPIRE_RANGE;
    }

    private boolean isDetailedLoggingEnabled() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0];
            switch (profile) {
                case "test":
                    return ChatCountCacheConstants.Test.DETAILED_LOGGING;
                case "prod":
                    return ChatCountCacheConstants.Production.DETAILED_LOGGING;
                default:
                    return ChatCountCacheConstants.Development.DETAILED_LOGGING;
            }
        }
        return ChatCountCacheConstants.Development.DETAILED_LOGGING;
    }

    private boolean isWarmupEnabled() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0];
            switch (profile) {
                case "test":
                    return ChatCountCacheConstants.Test.WARMUP_ENABLED;
                case "prod":
                    return ChatCountCacheConstants.Production.WARMUP_ENABLED;
                default:
                    return ChatCountCacheConstants.Development.WARMUP_ENABLED;
            }
        }
        return ChatCountCacheConstants.Development.WARMUP_ENABLED;
    }

    /**
     * 项目启动时预热缓存
     */
    @PostConstruct
    public void warmUpCache() {
        if (!isWarmupEnabled()) {
            logger.info("缓存预热已禁用，跳过预热");
            return;
        }

        logger.info("开始预热角色聊天计数缓存...");
        try {
            List<Character> characters = characterMapper.selectList(null);
            int loadCount = 0;

            for (Character character : characters) {
                if (character.getId() != null) {
                    String key = ChatCountCacheConstants.CHAT_COUNT_PREFIX + character.getId();

                    if (!redisTemplate.hasKey(key)) {
                        Long chatCount = character.getChatCount() != null ? character.getChatCount() : 0L;

                        long expireTime = getTotalExpireSeconds() + (long) (Math.random() * getRandomExpireRange());
                        redisTemplate.opsForValue().set(key, chatCount, expireTime, TimeUnit.SECONDS);
                        loadCount++;
                    }
                }
            }

            logger.info("缓存预热完成，共加载{}个角色的聊天计数", loadCount);
        } catch (Exception e) {
            logger.error("缓存预热失败", e);
        }
    }

    /**
     * 增加角色聊天计数
     *
     * @param characterId 角色ID
     * @return 增加后的计数
     */
    public Long incrementChatCount(Long characterId) {
        if (characterId == null) {
            logger.warn("角色ID为空，无法增加聊天计数");
            return 0L;
        }

        try {
            String key = ChatCountCacheConstants.CHAT_COUNT_PREFIX + characterId;
            String todayKey = ChatCountCacheConstants.CHAT_COUNT_TODAY_PREFIX + characterId + ":" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 使用Redis管道操作提高性能
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                // 增加总计数
                connection.incr(key.getBytes());
                // 增加今日计数
                connection.incr(todayKey.getBytes());
                return null;
            });

            // 设置今日计数过期时间（次日凌晨过期）
            redisTemplate.expire(todayKey, Duration.ofHours(25));

            Long newCount = (Long) results.get(0);

            // 检查缓存是否存在，如果不存在则从数据库加载
            if (newCount == 1) {
                Long dbCount = getChatCountFromDatabase(characterId);
                if (dbCount != null && dbCount > 0) {
                    redisTemplate.opsForValue().set(key, dbCount + 1,
                        getTotalExpireSeconds() + (long) (Math.random() * getRandomExpireRange()), TimeUnit.SECONDS);
                    newCount = dbCount + 1;
                }
            }

            if (isDetailedLoggingEnabled()) {
                logger.debug("角色{}聊天计数增加，当前计数: {}", characterId, newCount);
            }
            return newCount;

        } catch (Exception e) {
            logger.error("增加角色{}聊天计数失败", characterId, e);
            // 降级：异步写入数据库
            asyncIncrementDatabaseCount(characterId);
            return null;
        }
    }

    /**
     * 获取角色聊天计数（带缓存穿透保护）
     *
     * @param characterId 角色ID
     * @return 聊天计数
     */
    public Long getChatCount(Long characterId) {
        if (characterId == null) {
            return 0L;
        }

        String key = ChatCountCacheConstants.CHAT_COUNT_PREFIX + characterId;
        String lockKey = ChatCountCacheConstants.CHAT_COUNT_LOCK_PREFIX + characterId;
        String nullKey = ChatCountCacheConstants.CHAT_COUNT_NULL_PREFIX + characterId;

        try {
            // 1. 先检查缓存
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return (Long) cached;
            }

            // 2. 检查空值缓存（防穿透）
            if (redisTemplate.hasKey(nullKey)) {
                if (isDetailedLoggingEnabled()) {
                    logger.debug("角色{}命中空值缓存", characterId);
                }
                return 0L;
            }

            // 3. 获取分布式锁（防击穿）
            String lockValue = UUID.randomUUID().toString();
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, getLockExpireSeconds(), TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(lockAcquired)) {
                try {
                    // 双重检查
                    cached = redisTemplate.opsForValue().get(key);
                    if (cached != null) {
                        return (Long) cached;
                    }

                    // 从数据库获取
                    Long dbCount = getChatCountFromDatabase(characterId);

                    if (dbCount != null) {
                        // 设置缓存，添加随机过期时间
                        long expireTime = getTotalExpireSeconds() + (long) (Math.random() * getRandomExpireRange());
                        redisTemplate.opsForValue().set(key, dbCount, expireTime, TimeUnit.SECONDS);
                        return dbCount;
                    } else {
                        // 设置空值缓存
                        redisTemplate.opsForValue().set(nullKey, "null", getNullCacheExpireSeconds(), TimeUnit.SECONDS);
                        return 0L;
                    }
                } finally {
                    // 释放锁
                    releaseLock(lockKey, lockValue);
                }
            } else {
                // 未获取到锁，等待并重试
                Thread.sleep(50);
                return getChatCount(characterId);
            }

        } catch (Exception e) {
            logger.error("获取角色{}聊天计数失败", characterId, e);
            // 降级：直接从数据库获取
            Long dbCount = getChatCountFromDatabase(characterId);
            return dbCount != null ? dbCount : 0L;
        }
    }

    /**
     * 获取今日聊天计数
     *
     * @param characterId 角色ID
     * @return 今日聊天计数
     */
    public Long getTodayChatCount(Long characterId) {
        if (characterId == null) {
            return 0L;
        }

        String todayKey = ChatCountCacheConstants.CHAT_COUNT_TODAY_PREFIX + characterId + ":" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            Object cached = redisTemplate.opsForValue().get(todayKey);
            return cached != null ? (Long) cached : 0L;
        } catch (Exception e) {
            logger.error("获取角色{}今日聊天计数失败", characterId, e);
            return 0L;
        }
    }

    /**
     * 同步缓存数据到数据库
     *
     * @param characterId 角色ID（为空则同步所有）
     */
    public void syncCacheToDatabase(Long characterId) {
        try {
            if (characterId != null) {
                syncSingleCharacter(characterId);
            } else {
                syncAllCharacters();
            }
        } catch (Exception e) {
            logger.error("同步缓存到数据库失败", e);
        }
    }

    /**
     * 从数据库获取聊天计数
     */
    private Long getChatCountFromDatabase(Long characterId) {
        try {
            Character character = characterMapper.selectById(characterId);
            return character != null ? (character.getChatCount() != null ? character.getChatCount() : 0L) : null;
        } catch (Exception e) {
            logger.error("从数据库获取角色{}聊天计数失败", characterId, e);
            return null;
        }
    }

    /**
     * 异步增加数据库计数（降级方案）
     */
    @Async
    public void asyncIncrementDatabaseCount(Long characterId) {
        try {
            Character character = characterMapper.selectById(characterId);
            if (character != null) {
                Long currentCount = character.getChatCount() != null ? character.getChatCount() : 0L;
                character.setChatCount(currentCount + 1);
                characterMapper.updateById(character);
                logger.info("异步更新角色{}数据库聊天计数成功", characterId);
            }
        } catch (Exception e) {
            logger.error("异步更新角色{}数据库聊天计数失败", characterId, e);
        }
    }

    /**
     * 同步单个角色
     */
    private void syncSingleCharacter(Long characterId) {
        String key = ChatCountCacheConstants.CHAT_COUNT_PREFIX + characterId;
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            Long cacheCount = (Long) cached;
            Character character = characterMapper.selectById(characterId);

            if (character != null) {
                character.setChatCount(cacheCount);
                characterMapper.updateById(character);
                logger.debug("同步角色{}聊天计数到数据库: {}", characterId, cacheCount);
            }
        }
    }

    /**
     * 同步所有角色
     */
    private void syncAllCharacters() {
        Set<String> keys = redisTemplate.keys(ChatCountCacheConstants.CHAT_COUNT_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            logger.info("没有需要同步的聊天计数缓存");
            return;
        }

        int syncCount = 0;
        for (String key : keys) {
            try {
                String characterIdStr = key.replace(ChatCountCacheConstants.CHAT_COUNT_PREFIX, "");
                if (StrUtil.isNumeric(characterIdStr)) {
                    Long characterId = Long.parseLong(characterIdStr);
                    syncSingleCharacter(characterId);
                    syncCount++;
                }
            } catch (Exception e) {
                logger.error("同步聊天计数失败，key: {}", key, e);
            }
        }

        logger.info("同步聊天计数到数据库完成，共同步{}个角色", syncCount);
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(String lockKey, String lockValue) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
    }

    /**
     * 清理过期的今日计数缓存
     */
    public void cleanupExpiredTodayCache() {
        try {
            String pattern = ChatCountCacheConstants.CHAT_COUNT_TODAY_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return;
            }

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            List<String> expiredKeys = new ArrayList<>();

            for (String key : keys) {
                if (!key.endsWith(":" + today)) {
                    expiredKeys.add(key);
                }
            }

            if (!expiredKeys.isEmpty()) {
                redisTemplate.delete(expiredKeys);
                logger.info("清理过期今日计数缓存，共清理{}个key", expiredKeys.size());
            }

        } catch (Exception e) {
            logger.error("清理过期今日计数缓存失败", e);
        }
    }
}