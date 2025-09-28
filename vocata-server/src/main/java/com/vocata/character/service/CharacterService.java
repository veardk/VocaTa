package com.vocata.character.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.character.dto.request.CharacterCreateWithAiRequest;
import com.vocata.character.dto.response.CharacterAiGenerateResponse;
import com.vocata.character.dto.response.CharacterCreateWithAiResponse;
import com.vocata.character.entity.Character;

import java.util.List;

/**
 * 角色服务接口
 */
public interface CharacterService {

    /**
     * 根据ID获取角色详情
     * @param id 角色ID
     * @return 角色信息
     */
    Character getById(Long id);

    /**
     * 根据角色编码获取角色详情
     * @param characterCode 角色编码
     * @return 角色信息
     */
    Character getByCharacterCode(String characterCode);

    /**
     * 创建角色
     * @param character 角色信息
     * @return 创建的角色
     */
    Character create(Character character);

    /**
     * 更新角色
     * @param character 角色信息
     * @return 更新后的角色
     */
    Character update(Character character);

    /**
     * 删除角色（软删除）
     * @param id 角色ID
     * @return 是否删除成功
     */
    boolean delete(Long id);

    /**
     * 分页查询公开角色列表
     * @param page 分页参数
     * @param status 角色状态，null表示不过滤
     * @param isFeatured 是否精选，null表示不过滤
     * @param tags 标签列表，null表示不过滤
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @return 角色分页列表
     */
    IPage<Character> getPublicCharacters(Page<Character> page, Integer status, Integer isFeatured,
                                       List<String> tags, String orderBy, String orderDirection);

    /**
     * 分页查询用户创建的角色列表
     * @param page 分页参数
     * @param createId 创建者ID
     * @param status 角色状态，null表示不过滤
     * @return 角色分页列表
     */
    IPage<Character> getCharactersByCreator(Page<Character> page, Long createId, Integer status);

    /**
     * 全文搜索角色
     * @param page 分页参数
     * @param keyword 搜索关键词
     * @param status 角色状态，null表示不过滤
     * @return 角色分页列表
     */
    IPage<Character> searchCharacters(Page<Character> page, String keyword, Integer status);

    /**
     * 获取热门角色列表
     * @param limit 限制数量
     * @return 热门角色列表
     */
    List<Character> getTrendingCharacters(int limit);

    /**
     * 获取热门角色列表（包含创建者名称）
     * @param limit 限制数量
     * @return 热门角色列表（包含创建者名称）
     */
    List<java.util.Map<String, Object>> getTrendingCharactersWithCreator(int limit);

    /**
     * 获取精选角色列表
     * @param limit 限制数量
     * @return 精选角色列表
     */
    List<Character> getFeaturedCharacters(int limit);

    /**
     * 获取精选角色列表（包含创建者名称）
     * @param limit 限制数量
     * @return 精选角色列表（包含创建者名称）
     */
    List<java.util.Map<String, Object>> getFeaturedCharactersWithCreator(int limit);

    /**
     * 分页查询公开角色列表（包含创建者名称）
     * @param page 分页参数
     * @param status 角色状态，null表示不过滤
     * @param isFeatured 是否精选，null表示不过滤
     * @param tags 标签列表，null表示不过滤
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @return 角色分页列表（包含创建者名称）
     */
    com.baomidou.mybatisplus.core.metadata.IPage<java.util.Map<String, Object>> getPublicCharactersWithCreator(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Character> page,
            Integer status, Integer isFeatured,
            List<String> tags, String orderBy, String orderDirection);

    /**
     * 更新角色状态
     * @param id 角色ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 增加角色对话次数
     * @param characterId 角色ID
     * @param increment 增量
     * @return 是否更新成功
     */
    boolean incrementChatCount(Long characterId, int increment);

    /**
     * 检查用户是否有权限操作角色
     * @param characterId 角色ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean hasPermission(Long characterId, Long userId);

    // ========== 标签管理相关方法 ==========

    /**
     * 同步角色标签信息
     * 将JSON格式标签转换为数组字段，保持数据一致性
     * @param characterId 角色ID
     * @return 是否同步成功
     */
    boolean syncCharacterTags(Long characterId);

    /**
     * 根据标签ID查询角色列表
     * @param page 分页参数
     * @param tagIds 标签ID数组
     * @param status 角色状态，null表示不过滤
     * @return 角色分页列表
     */
    IPage<Character> getCharactersByTagIds(Page<Character> page, Long[] tagIds, Integer status);

    /**
     * 根据主要标签查询推荐角色
     * @param primaryTagIds 主要标签ID数组
     * @param limit 限制数量
     * @param excludeCharacterId 排除的角色ID
     * @return 推荐角色列表
     */
    List<Character> getRecommendedCharacters(Long[] primaryTagIds, int limit, Long excludeCharacterId);

    /**
     * 更新角色标签数组字段
     * @param characterId 角色ID
     * @param tagIds 标签ID数组
     * @param tagNames 标签名称数组
     * @param primaryTagIds 主要标签ID数组
     * @param tagSummary 标签摘要
     * @return 是否更新成功
     */
    boolean updateCharacterTagFields(Long characterId, Long[] tagIds, String[] tagNames,
                                   Long[] primaryTagIds, String tagSummary);

    /**
     * 创建角色并异步生成AI设定
     * @param request 创建请求
     * @return 创建响应
     */
    CharacterCreateWithAiResponse createWithAi(CharacterCreateWithAiRequest request);

    /**
     * 异步更新角色的AI生成字段
     * @param characterId 角色ID
     * @param aiResponse AI生成的响应数据
     * @return 是否更新成功
     */
    boolean updateAiGeneratedFields(Long characterId, CharacterAiGenerateResponse aiResponse);

}