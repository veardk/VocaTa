package com.vocata.character.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.character.dto.request.CharacterAiGenerateRequest;
import com.vocata.character.dto.request.CharacterCreateWithAiRequest;
import com.vocata.character.dto.response.CharacterAiGenerateResponse;
import com.vocata.character.dto.response.CharacterCreateWithAiResponse;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.character.service.CharacterAiGenerateService;
import com.vocata.character.service.CharacterService;
import com.vocata.character.service.CharacterChatCountService;
import com.vocata.common.constant.CharacterStatus;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.utils.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Service
public class CharacterServiceImpl extends ServiceImpl<CharacterMapper, Character> implements CharacterService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterServiceImpl.class);

    @Autowired
    private CharacterChatCountService characterChatCountService;

    @Autowired
    private CharacterAiGenerateService characterAiGenerateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Character getById(Long id) {
        if (id == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }
        Character character = super.getById(id);
        if (character != null) {
            // 从Redis获取最新的聊天计数
            Long chatCount = characterChatCountService.getChatCount(id);
            Long todayChatCount = characterChatCountService.getTodayChatCount(id);

            character.setChatCount(chatCount);
            character.setChatCountToday(todayChatCount != null ? todayChatCount.intValue() : 0);
        }
        return character;
    }

    @Override
    public Character getByCharacterCode(String characterCode) {
        if (StringUtils.isBlank(characterCode)) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        Character character = this.getOne(new LambdaQueryWrapper<Character>()
                .eq(Character::getCharacterCode, characterCode)
                .eq(Character::getStatus, CharacterStatus.PUBLISHED));

        if (character != null) {
            // 从Redis获取最新的聊天计数
            Long chatCount = characterChatCountService.getChatCount(character.getId());
            Long todayChatCount = characterChatCountService.getTodayChatCount(character.getId());

            character.setChatCount(chatCount);
            character.setChatCountToday(todayChatCount != null ? todayChatCount.intValue() : 0);
        }
        return character;
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
            character.setCreateId(currentUserId);
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
    public IPage<Character> getPublicCharacters(Page<Character> page, Integer status, Integer isFeatured,
                                               List<String> tags, String orderBy, String orderDirection) {
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                .eq(Character::getIsPrivate, false);

        if (status != null) {
            wrapper.eq(Character::getStatus, status);
        }
        if (isFeatured != null) {
            wrapper.eq(Character::getIsFeatured, isFeatured);
        }
        // TODO: 标签过滤需要使用JSON查询，暂时跳过

        // 动态排序
        applyOrderBy(wrapper, orderBy, orderDirection);

        return this.page(page, wrapper);
    }

    @Override
    public IPage<Character> getCharactersByCreator(Page<Character> page, Long createId, Integer status) {
        if (createId == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                .eq(Character::getCreateId, createId)
                .orderByDesc(Character::getUpdateDate);

        if (status != null) {
            wrapper.eq(Character::getStatus, status);
        }

        return this.page(page, wrapper);
    }

    @Override
    public IPage<Character> searchCharacters(Page<Character> page, String keyword, Integer status) {
        if (StringUtils.isBlank(keyword)) {
            return getPublicCharacters(page, status, null, null, "chat_count", "desc");
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
    public List<Map<String, Object>> getTrendingCharactersWithCreator(int limit) {
        return this.baseMapper.selectTrendingCharactersWithCreator(limit);
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
    public List<Map<String, Object>> getFeaturedCharactersWithCreator(int limit) {
        return this.baseMapper.selectFeaturedCharactersWithCreator(limit);
    }

    @Override
    public IPage<Map<String, Object>> getPublicCharactersWithCreator(Page<Character> page, Integer status,
                                                                   Integer isFeatured, List<String> tags,
                                                                   String orderBy, String orderDirection) {
        // 设置默认排序参数
        if (StringUtils.isBlank(orderBy)) {
            orderBy = "chat_count";
        }
        if (StringUtils.isBlank(orderDirection)) {
            orderDirection = "desc";
        }

        return this.baseMapper.selectPublicCharactersWithCreator(page, status, isFeatured, orderBy, orderDirection);
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
        if (character != null && userId.equals(character.getCreateId())) {
            return true;
        }

        return false;
    }

    // ========== 标签管理相关方法实现 ==========

    @Override
    public boolean syncCharacterTags(Long characterId) {
        if (characterId == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        Character character = this.getById(characterId);
        if (character == null) {
            throw new BizException(ApiCode.DATA_NOT_FOUND, "角色不存在");
        }

        try {
            // 解析JSON标签数据
            Long[] tagIds = parseJsonToLongArray(character.getTags());
            String[] tagNames = parseJsonToStringArray(character.getTags());

            // 生成标签摘要
            String tagSummary = generateTagSummary(tagNames);

            // 确定主要标签（前3个）
            Long[] primaryTagIds = tagIds != null && tagIds.length > 0 ?
                Arrays.copyOf(tagIds, Math.min(tagIds.length, 3)) : new Long[0];

            // 更新数组字段
            return updateCharacterTagFields(characterId, tagIds, tagNames, primaryTagIds, tagSummary);
        } catch (Exception e) {
            // 标签同步失败不影响主流程，记录日志但不抛异常
            return false;
        }
    }

    @Override
    public IPage<Character> getCharactersByTagIds(Page<Character> page, Long[] tagIds, Integer status) {
        if (tagIds == null || tagIds.length == 0) {
            return this.getPublicCharacters(page, status, null, null, "chat_count", "desc");
        }

        // 使用QueryWrapper查询包含任意标签的角色
        LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                .eq(Character::getIsPrivate, false);

        if (status != null) {
            wrapper.eq(Character::getStatus, status);
        }

        // 这里需要自定义SQL或者先查询所有再过滤
        // 暂时使用简化逻辑：通过tagSummary字段进行模糊匹配
        if (tagIds.length > 0) {
            wrapper.and(w -> {
                for (Long tagId : tagIds) {
                    w.like(Character::getTagSummary, tagId.toString()).or();
                }
            });
        }

        wrapper.orderByDesc(Character::getChatCount);
        return this.page(page, wrapper);
    }

    @Override
    public List<Character> getRecommendedCharacters(Long[] primaryTagIds, int limit, Long excludeCharacterId) {
        if (primaryTagIds == null || primaryTagIds.length == 0) {
            return getTrendingCharacters(limit);
        }

        Page<Character> page = new Page<>(1, limit);
        IPage<Character> result = getCharactersByTagIds(page, primaryTagIds, CharacterStatus.PUBLISHED);

        List<Character> characters = result.getRecords();

        // 排除指定角色
        if (excludeCharacterId != null) {
            characters = characters.stream()
                    .filter(c -> !excludeCharacterId.equals(c.getId()))
                    .collect(Collectors.toList());
        }

        return characters;
    }

    @Override
    public boolean updateCharacterTagFields(Long characterId, Long[] tagIds, String[] tagNames,
                                          Long[] primaryTagIds, String tagSummary) {
        if (characterId == null) {
            throw new BizException(ApiCode.PARAM_ERROR);
        }

        try {
            // 转换数组为JSON字符串用于PostgreSQL
            String tagIdsJson = arrayToJson(tagIds);
            String tagNamesJson = arrayToJson(tagNames);
            String primaryTagIdsJson = arrayToJson(primaryTagIds);

            return this.baseMapper.updateCharacterTags(characterId, tagIdsJson, tagNamesJson,
                                                     primaryTagIdsJson, tagSummary) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== 私有工具方法 ==========

    /**
     * 解析JSON字符串为Long数组
     */
    private Long[] parseJsonToLongArray(String jsonString) {
        if (StringUtils.isBlank(jsonString)) {
            return new Long[0];
        }
        try {
            // 简化实现：假设JSON格式为 ["1","2","3"]
            String cleaned = jsonString.replaceAll("[\\[\\]\"\\s]", "");
            if (StringUtils.isBlank(cleaned)) {
                return new Long[0];
            }
            return Arrays.stream(cleaned.split(","))
                    .filter(StringUtils::isNotBlank)
                    .map(Long::parseLong)
                    .toArray(Long[]::new);
        } catch (Exception e) {
            return new Long[0];
        }
    }

    /**
     * 解析JSON字符串为String数组
     */
    private String[] parseJsonToStringArray(String jsonString) {
        if (StringUtils.isBlank(jsonString)) {
            return new String[0];
        }
        try {
            // 简化实现：假设JSON格式为 ["动漫","治愈","女友"]
            String cleaned = jsonString.replaceAll("[\\[\\]\"]", "");
            if (StringUtils.isBlank(cleaned)) {
                return new String[0];
            }
            return Arrays.stream(cleaned.split(","))
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .toArray(String[]::new);
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * 生成标签摘要
     */
    private String generateTagSummary(String[] tagNames) {
        if (tagNames == null || tagNames.length == 0) {
            return "";
        }
        return String.join("、", Arrays.stream(tagNames)
                .limit(5) // 最多5个标签
                .collect(Collectors.toList()));
    }

    /**
     * 数组转JSON字符串（用于PostgreSQL数组类型）
     */
    private String arrayToJson(Object[] array) {
        if (array == null || array.length == 0) {
            return "{}";
        }
        return "{" + Arrays.stream(array)
                .map(Object::toString)
                .collect(Collectors.joining(",")) + "}";
    }

    /**
     * 应用动态排序
     */
    private void applyOrderBy(LambdaQueryWrapper<Character> wrapper, String orderBy, String orderDirection) {
        // 设置默认值
        if (StringUtils.isBlank(orderBy)) {
            orderBy = "chat_count";
        }
        if (StringUtils.isBlank(orderDirection)) {
            orderDirection = "desc";
        }

        boolean isAsc = "asc".equalsIgnoreCase(orderDirection);

        switch (orderBy.toLowerCase()) {
            case "chat_count":
                if (isAsc) {
                    wrapper.orderByAsc(Character::getChatCount);
                } else {
                    wrapper.orderByDesc(Character::getChatCount);
                }
                break;
            case "created_at":
                if (isAsc) {
                    wrapper.orderByAsc(Character::getCreateDate);
                } else {
                    wrapper.orderByDesc(Character::getCreateDate);
                }
                break;
            case "updated_at":
                if (isAsc) {
                    wrapper.orderByAsc(Character::getUpdateDate);
                } else {
                    wrapper.orderByDesc(Character::getUpdateDate);
                }
                break;
            case "trending_score":
                if (isAsc) {
                    wrapper.orderByAsc(Character::getTrendingScore);
                } else {
                    wrapper.orderByDesc(Character::getTrendingScore);
                }
                break;
            case "sort_weight":
                if (isAsc) {
                    wrapper.orderByAsc(Character::getSortWeight);
                } else {
                    wrapper.orderByDesc(Character::getSortWeight);
                }
                break;
            default:
                // 默认按对话次数降序
                wrapper.orderByDesc(Character::getChatCount);
                break;
        }

        // 添加二级排序：创建时间降序
        wrapper.orderByDesc(Character::getCreateDate);
    }

    @Override
    public CharacterCreateWithAiResponse createWithAi(CharacterCreateWithAiRequest request) {
        logger.info("开始创建带AI生成的角色，名称: {}", request.getName());

        // 1. 先创建基础角色记录
        Character character = new Character();
        BeanUtils.copyProperties(request, character);

        // 生成唯一的角色编码
        character.setCharacterCode(generateCharacterCode(request.getName()));

        // 设置默认值
        character.setStatus(CharacterStatus.UNDER_REVIEW);
        character.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false);
        character.setIsOfficial(0);
        character.setIsFeatured(0);
        character.setIsTrending(0);
        character.setTrendingScore(0);
        character.setChatCount(0L);
        character.setChatCountToday(0);
        character.setChatCountWeek(0);
        character.setUserCount(0);
        character.setSortWeight(0);
        character.setCreateId(UserContext.getUserId());

        // 设置默认的模型配置
        character.setTemperature(new BigDecimal("0.7"));
        character.setContextWindow(3000);
        character.setLanguage("zh-CN");

        // 保存角色记录
        boolean saved = this.save(character);
        if (!saved) {
            throw new BizException(ApiCode.ERROR, "角色创建失败");
        }

        logger.info("角色基础信息创建成功，ID: {}", character.getId());

        // 2. 构建响应对象
        CharacterCreateWithAiResponse response = new CharacterCreateWithAiResponse();
        response.setCharacterId(character.getId().toString());
        response.setName(character.getName());
        response.setDescription(character.getDescription());
        response.setGreeting(character.getGreeting());
        response.setAvatarUrl(character.getAvatarUrl());
        response.setIsPrivate(character.getIsPrivate());
        response.setStatus(character.getStatus());
        response.setAiGenerationStatus("AI生成任务已启动，详细角色设定将在后台自动生成");

        // 3. 异步启动AI生成任务
        asyncGenerateAiFields(character.getId(), request);

        return response;
    }

    @Override
    public boolean updateAiGeneratedFields(Long characterId, CharacterAiGenerateResponse aiResponse) {
        logger.info("开始更新角色AI生成字段，角色ID: {}", characterId);

        try {
            LambdaUpdateWrapper<Character> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Character::getId, characterId);

            // 设置persona
            if (StringUtils.isNotBlank(aiResponse.getPersona())) {
                updateWrapper.set(Character::getPersona, aiResponse.getPersona());
            }

            // 设置性格特征
            if (aiResponse.getPersonalityTraits() != null && !aiResponse.getPersonalityTraits().isEmpty()) {
                String personalityTraitsJson = listToJson(aiResponse.getPersonalityTraits());
                updateWrapper.set(Character::getPersonalityTraits, personalityTraitsJson);
            }

            // 设置说话风格
            if (StringUtils.isNotBlank(aiResponse.getSpeakingStyle())) {
                updateWrapper.set(Character::getSpeakingStyle, aiResponse.getSpeakingStyle());
            }

            // 设置示例对话
            if (aiResponse.getExampleDialogues() != null && !aiResponse.getExampleDialogues().isEmpty()) {
                String exampleDialoguesJson = dialogueListToJson(aiResponse.getExampleDialogues());
                updateWrapper.set(Character::getExampleDialogues, exampleDialoguesJson);
            }

            // 设置标签
            if (aiResponse.getTags() != null && !aiResponse.getTags().isEmpty()) {
                String tagsJson = listToJson(aiResponse.getTags());
                updateWrapper.set(Character::getTags, tagsJson);
            }

            // 设置搜索关键词
            if (StringUtils.isNotBlank(aiResponse.getSearchKeywords())) {
                updateWrapper.set(Character::getSearchKeywords, aiResponse.getSearchKeywords());
            }

            // 执行更新
            boolean updated = this.update(updateWrapper);
            if (updated) {
                logger.info("角色AI生成字段更新成功，角色ID: {}", characterId);
            } else {
                logger.error("角色AI生成字段更新失败，角色ID: {}", characterId);
            }

            return updated;

        } catch (Exception e) {
            logger.error("更新角色AI生成字段时发生异常，角色ID: " + characterId, e);
            return false;
        }
    }

    /**
     * 异步生成AI字段
     */
    @Async
    public void asyncGenerateAiFields(Long characterId, CharacterCreateWithAiRequest request) {
        logger.info("开始异步生成AI字段，角色ID: {}", characterId);

        try {
            // 构建AI生成请求
            CharacterAiGenerateRequest aiRequest = new CharacterAiGenerateRequest();
            aiRequest.setName(request.getName());
            aiRequest.setDescription(request.getDescription());
            aiRequest.setGreeting(request.getGreeting());

            // 调用AI生成服务
            CharacterAiGenerateResponse aiResponse = characterAiGenerateService.generateCharacter(aiRequest);

            // 更新数据库字段
            boolean updateResult = updateAiGeneratedFields(characterId, aiResponse);

            if (updateResult) {
                logger.info("角色AI字段异步生成完成，角色ID: {}", characterId);
            } else {
                logger.error("角色AI字段异步更新失败，角色ID: {}", characterId);
            }

        } catch (Exception e) {
            logger.error("角色AI字段异步生成失败，角色ID: " + characterId, e);
        }
    }

    /**
     * 生成唯一的角色编码
     */
    private String generateCharacterCode(String name) {
        // 基于名称和时间戳生成唯一编码
        String baseCode = name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
        if (baseCode.length() > 10) {
            baseCode = baseCode.substring(0, 10);
        }
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        String uuid = UUID.randomUUID().toString().substring(0, 4);
        return baseCode + "_" + timestamp + "_" + uuid;
    }

    /**
     * 将List转换为JSON字符串
     */
    private String listToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            logger.error("List转JSON失败", e);
            return "[]";
        }
    }

    /**
     * 将对话列表转换为JSON字符串
     */
    private String dialogueListToJson(List<CharacterAiGenerateResponse.DialogueExample> dialogues) {
        try {
            return objectMapper.writeValueAsString(dialogues);
        } catch (JsonProcessingException e) {
            logger.error("对话列表转JSON失败", e);
            return "[]";
        }
    }

}