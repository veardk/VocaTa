package com.vocata.character.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * @return 角色分页列表
     */
    IPage<Character> getPublicCharacters(Page<Character> page, Integer status, Integer isFeatured, List<String> tags);

    /**
     * 分页查询用户创建的角色列表
     * @param page 分页参数
     * @param creatorId 创建者ID
     * @param status 角色状态，null表示不过滤
     * @return 角色分页列表
     */
    IPage<Character> getCharactersByCreator(Page<Character> page, Long creatorId, Integer status);

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
     * 获取精选角色列表
     * @param limit 限制数量
     * @return 精选角色列表
     */
    List<Character> getFeaturedCharacters(int limit);

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
}