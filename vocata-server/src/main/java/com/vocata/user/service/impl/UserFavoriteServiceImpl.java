package com.vocata.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vocata.character.dto.response.CharacterResponse;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.common.result.ApiCode;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.PageResult;
import com.vocata.user.dto.response.FavoriteResponse;
import com.vocata.user.entity.UserFavorite;
import com.vocata.user.mapper.UserFavoriteMapper;
import com.vocata.user.service.UserFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户收藏服务实现类
 */
@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements UserFavoriteService {

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private CharacterMapper characterMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFavorite(Long userId, Long characterId) {
        Map<String, Object> result = new HashMap<>();

        // 检查角色是否存在且可用
        Character character = characterMapper.selectOne(
            new LambdaQueryWrapper<Character>()
                .eq(Character::getId, characterId)
                .eq(Character::getIsDelete, 0)
                .eq(Character::getStatus, 1)
        );
        if (character == null) {
            throw new BizException(ApiCode.CHARACTER_NOT_EXIST);
        }

        // 检查当前收藏状态
        Long existingFavoriteId = userFavoriteMapper.checkUserFavorite(userId, characterId);

        if (existingFavoriteId != null) {
            // 已收藏，执行取消收藏操作（直接删除记录）
            boolean success = this.removeById(existingFavoriteId);
            result.put("success", success);
            result.put("isFavorited", false);
            result.put("action", "remove");
            result.put("message", "取消收藏成功");
        } else {
            // 未收藏，执行收藏操作
            UserFavorite userFavorite = new UserFavorite(userId, characterId);

            boolean success = this.save(userFavorite);
            result.put("success", success);
            result.put("isFavorited", true);
            result.put("action", "add");
            result.put("message", "收藏成功");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean favoriteCharacter(Long userId, Long characterId) {
        // 检查角色是否存在且可用
        Character character = characterMapper.selectOne(
            new LambdaQueryWrapper<Character>()
                .eq(Character::getId, characterId)
                .eq(Character::getIsDelete, 0)
                .eq(Character::getStatus, 1)
        );
        if (character == null) {
            throw new BizException(ApiCode.CHARACTER_NOT_EXIST);
        }

        // 检查是否已经收藏
        Long existingFavoriteId = userFavoriteMapper.checkUserFavorite(userId, characterId);
        if (existingFavoriteId != null) {
            throw new BizException(ApiCode.FAVORITE_ALREADY_EXISTS);
        }

        // 创建收藏记录
        UserFavorite userFavorite = new UserFavorite(userId, characterId);

        return this.save(userFavorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfavoriteCharacter(Long userId, Long characterId) {
        // 检查收藏记录是否存在
        Long favoriteId = userFavoriteMapper.checkUserFavorite(userId, characterId);
        if (favoriteId == null) {
            throw new BizException(ApiCode.FAVORITE_NOT_EXIST);
        }

        // 直接删除收藏记录
        return this.removeById(favoriteId);
    }

    @Override
    public PageResult<FavoriteResponse> getUserFavorites(Long userId, Integer pageNum, Integer pageSize) {
        // 参数校验
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 分页查询收藏记录
        Page<UserFavorite> page = new Page<>(pageNum, pageSize);
        Page<UserFavorite> favoriteRecords = userFavoriteMapper.getFavoritesByUserId(page, userId);

        // 构建响应对象列表
        List<FavoriteResponse> responseList = favoriteRecords.getRecords().stream().map(favorite -> {
            FavoriteResponse response = new FavoriteResponse();
            response.setId(favorite.getId());
            response.setUserId(favorite.getUserId());
            response.setCharacterId(favorite.getCharacterId());
            response.setCreatedAt(favorite.getCreatedAt());

            // 获取角色详细信息
            Character character = characterMapper.selectById(favorite.getCharacterId());
            if (character != null) {
                CharacterResponse characterResponse = convertToCharacterResponse(character);
                response.setCharacter(characterResponse);
            }

            return response;
        }).collect(Collectors.toList());

        return new PageResult<>(pageNum, pageSize, favoriteRecords.getTotal(), responseList);
    }

    /**
     * 将Character实体转换为CharacterResponse
     */
    private CharacterResponse convertToCharacterResponse(Character character) {
        CharacterResponse response = new CharacterResponse();
        response.setId(character.getId());
        response.setCharacterCode(character.getCharacterCode());
        response.setName(character.getName());
        response.setDescription(character.getDescription());
        response.setGreeting(character.getGreeting());
        response.setAvatarUrl(character.getAvatarUrl());
        response.setTags(character.getTags());
        response.setLanguage(character.getLanguage());
        response.setStatus(character.getStatus());
        response.setIsOfficial(character.getIsOfficial());
        response.setIsFeatured(character.getIsFeatured());
        response.setIsTrending(character.getIsTrending());
        response.setTrendingScore(character.getTrendingScore());
        response.setChatCount(character.getChatCount());
        response.setUserCount(character.getUserCount());
        response.setIsPrivate(character.getIsPrivate());
        response.setTagIds(character.getTagIds());
        response.setTagNames(character.getTagNames());
        response.setPrimaryTagIds(character.getPrimaryTagIds());
        response.setTagSummary(character.getTagSummary());
        response.setCreateId(character.getCreateId());
        response.setCreatedAt(character.getCreateDate());
        response.setUpdatedAt(character.getUpdateDate());
        return response;
    }

    @Override
    public Integer getUserFavoriteCount(Long userId) {
        Integer count = userFavoriteMapper.getFavoriteCountByUserId(userId);
        return count != null ? count : 0;
    }

    @Override
    public Map<String, Boolean> batchCheckFavoriteStatus(Long userId, List<Long> characterIds) {
        if (characterIds == null || characterIds.isEmpty()) {
            return new HashMap<>();
        }

        // 获取已收藏的角色ID列表
        List<Map<String, Object>> favoriteList = userFavoriteMapper.batchCheckFavoriteStatus(userId, characterIds);
        Map<String, Boolean> favoriteMap = favoriteList.stream()
            .collect(Collectors.toMap(
                item -> String.valueOf(item.get("character_id")),
                item -> true
            ));

        // 构建完整的结果Map，未收藏的设为false
        Map<String, Boolean> result = new HashMap<>();
        for (Long characterId : characterIds) {
            result.put(String.valueOf(characterId), favoriteMap.getOrDefault(String.valueOf(characterId), false));
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getFavoriteRanking(Integer limit) {
        if (limit == null || limit < 1 || limit > 100) {
            limit = 10;
        }
        return userFavoriteMapper.getFavoriteRanking(limit);
    }

    @Override
    public boolean isUserFavorite(Long userId, Long characterId) {
        Long favoriteId = userFavoriteMapper.checkUserFavorite(userId, characterId);
        return favoriteId != null;
    }
}