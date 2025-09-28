package com.vocata.character.controller;

import com.vocata.character.service.CharacterChatCountService;
import com.vocata.common.result.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 角色聊天计数API控制器
 */
@RestController
@RequestMapping("/api/admin/character/chat-count")
public class CharacterChatCountController {

    @Autowired
    private CharacterChatCountService characterChatCountService;

    /**
     * 获取角色聊天计数
     *
     * @param characterId 角色ID
     * @return 聊天计数信息
     */
    @GetMapping("/{characterId}")
    public ApiResponse<Map<String, Object>> getChatCount(@PathVariable Long characterId) {
        Long totalCount = characterChatCountService.getChatCount(characterId);
        Long todayCount = characterChatCountService.getTodayChatCount(characterId);

        Map<String, Object> result = new HashMap<>();
        result.put("characterId", characterId.toString());
        result.put("totalChatCount", totalCount);
        result.put("todayChatCount", todayCount);
        result.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success(result);
    }

    /**
     * 手动增加角色聊天计数（测试用）
     *
     * @param characterId 角色ID
     * @return 更新后的计数
     */
    @PostMapping("/{characterId}/increment")
    public ApiResponse<Map<String, Object>> incrementChatCount(@PathVariable Long characterId) {
        Long newCount = characterChatCountService.incrementChatCount(characterId);
        Long todayCount = characterChatCountService.getTodayChatCount(characterId);

        Map<String, Object> result = new HashMap<>();
        result.put("characterId", characterId.toString());
        result.put("newTotalCount", newCount);
        result.put("todayChatCount", todayCount);
        result.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success(result);
    }

    /**
     * 手动同步缓存到数据库
     *
     * @param characterId 角色ID（可选，为空则同步全部）
     * @return 同步结果
     */
    @PostMapping("/sync")
    public ApiResponse<Map<String, Object>> syncCacheToDatabase(@RequestParam(required = false) Long characterId) {
        characterChatCountService.syncCacheToDatabase(characterId);

        Map<String, Object> result = new HashMap<>();
        result.put("message", characterId != null ?
            "角色" + characterId + "的聊天计数已同步到数据库" :
            "所有角色的聊天计数已同步到数据库");
        result.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success(result);
    }

    /**
     * 预热缓存
     *
     * @return 预热结果
     */
    @PostMapping("/warm-up")
    public ApiResponse<Map<String, Object>> warmUpCache() {
        characterChatCountService.warmUpCache();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "缓存预热已完成");
        result.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success(result);
    }

    /**
     * 清理过期缓存
     *
     * @return 清理结果
     */
    @PostMapping("/cleanup")
    public ApiResponse<Map<String, Object>> cleanupExpiredCache() {
        characterChatCountService.cleanupExpiredTodayCache();

        Map<String, Object> result = new HashMap<>();
        result.put("message", "过期缓存清理已完成");
        result.put("timestamp", System.currentTimeMillis());

        return ApiResponse.success(result);
    }
}