package com.vocata.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.common.result.PageResult;
import com.vocata.user.dto.response.FavoriteResponse;

import java.util.List;
import java.util.Map;

/**
 * 用户收藏服务接口
 */
public interface UserFavoriteService {

    /**
     * 切换收藏状态（收藏/取消收藏）
     * @param userId 用户ID
     * @param characterId 角色ID
     * @return 操作结果，包含isFavorited（当前是否已收藏）和action（执行的操作：add/remove）
     */
    Map<String, Object> toggleFavorite(Long userId, Long characterId);

    /**
     * 收藏角色
     * @param userId 用户ID
     * @param characterId 角色ID
     * @return 是否收藏成功
     */
    boolean favoriteCharacter(Long userId, Long characterId);

    /**
     * 取消收藏角色
     * @param userId 用户ID
     * @param characterId 角色ID
     * @return 是否取消收藏成功
     */
    boolean unfavoriteCharacter(Long userId, Long characterId);

    /**
     * 分页获取用户收藏的角色列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 收藏角色分页列表
     */
    PageResult<FavoriteResponse> getUserFavorites(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 获取用户收藏角色数量
     * @param userId 用户ID
     * @return 收藏数量
     */
    Integer getUserFavoriteCount(Long userId);

    /**
     * 批量检查收藏状态
     * @param userId 用户ID
     * @param characterIds 角色ID列表
     * @return 收藏状态Map，key为characterId，value为是否收藏
     */
    Map<String, Boolean> batchCheckFavoriteStatus(Long userId, List<Long> characterIds);

    /**
     * 获取角色收藏数排行榜
     * @param limit 限制数量，默认10
     * @return 角色收藏排行列表
     */
    List<Map<String, Object>> getFavoriteRanking(Integer limit);

    /**
     * 检查用户是否已收藏指定角色
     * @param userId 用户ID
     * @param characterId 角色ID
     * @return 是否已收藏
     */
    boolean isUserFavorite(Long userId, Long characterId);
}