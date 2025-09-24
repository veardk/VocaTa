package com.vocata.character.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.character.service.CharacterService;
import com.vocata.common.constant.CharacterStatus;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.utils.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色服务实现类
 */
@Service
public class CharacterServiceImpl extends ServiceImpl<CharacterMapper, Character> implements CharacterService {

    @Override
    public Character getById(Long id) {
        if (id == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }
        return super.getById(id);
    }

    @Override
    public Character getByCharacterCode(String characterCode) {
        if (StringUtils.isBlank(characterCode)) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        return this.getOne(new LambdaQueryWrapper<Character>()
                .eq(Character::getCharacterCode, characterCode)
                .eq(Character::getStatus, CharacterStatus.PUBLISHED));
    }

    @Override
    public Character create(Character character) {
        if (character == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        // 检查角色编码是否重复
        if (StringUtils.isNotBlank(character.getCharacterCode())) {
            long count = this.count(new LambdaQueryWrapper<Character>()
                    .eq(Character::getCharacterCode, character.getCharacterCode()));
            if (count > 0) {
                throw new BizException(ApiCode.DATA_ALREADY_EXISTS.getCode(), "角色编码已存在");
            }
        }

        // 设置默认值
        if (character.getStatus() == null) {
            character.setStatus(CharacterStatus.UNDER_REVIEW);
        }
        if (character.getIsPrivate() == null) {
            character.setIsPrivate(true);
        }
        if (character.getIsOfficial() == null) {
            character.setIsOfficial(0);
        }
        if (character.getIsFeatured() == null) {
            character.setIsFeatured(0);
        }
        if (character.getIsTrending() == null) {
            character.setIsTrending(0);
        }
        if (character.getChatCount() == null) {
            character.setChatCount(0L);
        }
        if (character.getUserCount() == null) {
            character.setUserCount(0);
        }
        if (character.getSortWeight() == null) {
            character.setSortWeight(0);
        }

        // 设置创建者
        Long currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            character.setCreatorId(currentUserId);
        }

        character.setCreateDate(LocalDateTime.now());
        character.setUpdateDate(LocalDateTime.now());

        this.save(character);
        return character;
    }

    @Override
    public Character update(Character character) {
        if (character == null || character.getId() == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        Character existing = this.getById(character.getId());
        if (existing == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND.getCode(), "角色不存在");
        }

        // 权限检查
        if (!hasPermission(character.getId(), UserContext.getUserId())) {
            throw new BizException(ApiCode.ACCESS_DENIED);
        }

        // 检查角色编码是否重复（排除自己）
        if (StringUtils.isNotBlank(character.getCharacterCode())
            && !character.getCharacterCode().equals(existing.getCharacterCode())) {
            long count = this.count(new LambdaQueryWrapper<Character>()
                    .eq(Character::getCharacterCode, character.getCharacterCode())
                    .ne(Character::getId, character.getId()));
            if (count > 0) {
                throw new BizException(ApiCode.DATA_ALREADY_EXISTS.getCode(), "角色编码已存在");
            }
        }

        character.setUpdateDate(LocalDateTime.now());
        this.updateById(character);
        return this.getById(character.getId());
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        Character character = this.getById(id);
        if (character == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND.getCode(), "角色不存在");
        }

        // 权限检查
        if (!hasPermission(id, UserContext.getUserId())) {
            throw new BizException(ApiCode.ACCESS_DENIED);
        }

        return this.removeById(id);
    }

    @Override
    public IPage<Character> getPublicCharacters(Page<Character> page, Integer status, Integer isFeatured, List<String> tags) {
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                .eq(Character::getIsPrivate, false)
                .orderByDesc(Character::getSortWeight)
                .orderByDesc(Character::getCreateDate);

        if (status != null) {
            wrapper.eq(Character::getStatus, status);
        }
        if (isFeatured != null) {
            wrapper.eq(Character::getIsFeatured, isFeatured);
        }
        // TODO: 标签过滤需要使用JSON查询，暂时跳过

        return this.page(page, wrapper);
    }

    @Override
    public IPage<Character> getCharactersByCreator(Page<Character> page, Long creatorId, Integer status) {
        if (creatorId == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                .eq(Character::getCreatorId, creatorId)
                .orderByDesc(Character::getUpdateDate);

        if (status != null) {
            wrapper.eq(Character::getStatus, status);
        }

        return this.page(page, wrapper);
    }

    @Override
    public IPage<Character> searchCharacters(Page<Character> page, String keyword, Integer status) {
        if (StringUtils.isBlank(keyword)) {
            return getPublicCharacters(page, status, null, null);
        }

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                .eq(Character::getIsPrivate, false)
                .and(w -> w.like(Character::getName, keyword)
                         .or()
                         .like(Character::getDescription, keyword)
                         .or()
                         .like(Character::getSearchKeywords, keyword))
                .orderByDesc(Character::getChatCount);

        if (status != null) {
            wrapper.eq(Character::getStatus, status);
        }

        return this.page(page, wrapper);
    }

    @Override
    public List<Character> getTrendingCharacters(int limit) {
        Page<Character> page = new Page<>(1, limit);
        IPage<Character> result = this.page(page, new LambdaQueryWrapper<Character>()
                .eq(Character::getIsPrivate, false)
                .eq(Character::getStatus, CharacterStatus.PUBLISHED)
                .eq(Character::getIsTrending, 1)
                .orderByDesc(Character::getTrendingScore));
        return result.getRecords();
    }

    @Override
    public List<Character> getFeaturedCharacters(int limit) {
        Page<Character> page = new Page<>(1, limit);
        IPage<Character> result = this.page(page, new LambdaQueryWrapper<Character>()
                .eq(Character::getIsPrivate, false)
                .eq(Character::getStatus, CharacterStatus.PUBLISHED)
                .eq(Character::getIsFeatured, 1)
                .orderByDesc(Character::getSortWeight)
                .orderByDesc(Character::getCreateDate));
        return result.getRecords();
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        if (id == null || status == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        if (!CharacterStatus.isValidStatus(status)) {
            throw new BizException(ApiCode.PARAM_ERROR.getCode(), "无效的状态值");
        }

        return this.update(new LambdaUpdateWrapper<Character>()
                .eq(Character::getId, id)
                .set(Character::getStatus, status)
                .set(Character::getUpdateDate, LocalDateTime.now()));
    }

    @Override
    public boolean incrementChatCount(Long characterId, int increment) {
        if (characterId == null || increment <= 0) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        // 使用Mapper原子操作避免并发问题
        return this.baseMapper.incrementChatCount(characterId, increment) > 0;
    }

    @Override
    public boolean hasPermission(Long characterId, Long userId) {
        if (characterId == null || userId == null) {
            return false;
        }

        // 检查是否是管理员
        if (UserContext.isAdmin()) {
            return true;
        }

        // 检查是否是角色创建者
        Character character = this.getById(characterId);
        if (character != null && userId.equals(character.getCreatorId())) {
            return true;
        }

        return false;
    }
}