package com.vocata.character.task;

import com.vocata.character.service.CharacterChatCountService;
import com.vocata.character.constants.ChatCountCacheConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 角色聊天计数定时任务
 *
 * 功能：
 * 1. 每天凌晨2点同步Redis缓存数据到数据库
 * 2. 清理过期的今日计数缓存
 */
@Component
public class CharacterChatCountTask {

    private static final Logger logger = LoggerFactory.getLogger(CharacterChatCountTask.class);

    @Autowired
    private CharacterChatCountService characterChatCountService;

    /**
     * 每天凌晨2点同步缓存数据到数据库
     * Cron表达式：秒 分 时 日 月 周年
     * 0 0 2 * * ? 表示每天凌晨2点执行
     */
    @Scheduled(cron = ChatCountCacheConstants.SYNC_CRON)
    public void syncCacheToDatabase() {
        logger.info("开始执行定时任务：同步角色聊天计数缓存到数据库");
        long startTime = System.currentTimeMillis();

        try {
            // 同步所有角色的聊天计数
            characterChatCountService.syncCacheToDatabase(null);

            long endTime = System.currentTimeMillis();
            logger.info("定时任务执行完成：同步角色聊天计数缓存到数据库，耗时：{}ms", endTime - startTime);

        } catch (Exception e) {
            logger.error("定时任务执行失败：同步角色聊天计数缓存到数据库", e);
        }
    }

    /**
     * 每小时清理过期的今日计数缓存
     * Cron表达式：0 0 * * * ? 表示每小时的0分0秒执行
     */
    @Scheduled(cron = ChatCountCacheConstants.CLEANUP_CRON)
    public void cleanupExpiredTodayCache() {
        logger.info("开始执行定时任务：清理过期今日计数缓存");
        long startTime = System.currentTimeMillis();

        try {
            characterChatCountService.cleanupExpiredTodayCache();

            long endTime = System.currentTimeMillis();
            logger.info("定时任务执行完成：清理过期今日计数缓存，耗时：{}ms", endTime - startTime);

        } catch (Exception e) {
            logger.error("定时任务执行失败：清理过期今日计数缓存", e);
        }
    }

    /**
     * 每天凌晨3点重新预热缓存
     * 在数据同步完成后1小时，重新预热热门角色的缓存
     */
    @Scheduled(cron = ChatCountCacheConstants.WARMUP_CRON)
    public void warmUpHotCharactersCache() {
        logger.info("开始执行定时任务：预热热门角色缓存");
        long startTime = System.currentTimeMillis();

        try {
            characterChatCountService.warmUpCache();

            long endTime = System.currentTimeMillis();
            logger.info("定时任务执行完成：预热热门角色缓存，耗时：{}ms", endTime - startTime);

        } catch (Exception e) {
            logger.error("定时任务执行失败：预热热门角色缓存", e);
        }
    }
}