package com.vocata.character.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.character.entity.Character;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 角色数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，提供基础CRUD操作
 * 复杂查询通过Service层使用QueryWrapper实现
 */
@Mapper
public interface CharacterMapper extends BaseMapper<Character> {

    /**
     * 原子性地增加角色对话计数
     * @param characterId 角色ID
     * @param increment 增加的数量
     * @return 影响的行数
     */
    @Update("UPDATE vocata_character SET " +
            "chat_count = chat_count + #{increment}, " +
            "chat_count_today = chat_count_today + #{increment}, " +
            "chat_count_week = chat_count_week + #{increment}, " +
            "updated_at = NOW() " +
            "WHERE id = #{characterId}")
    int incrementChatCount(@Param("characterId") Long characterId, @Param("increment") int increment);

    /**
     * 更新角色标签信息（原子操作）
     * 使用PostgreSQL数组类型转换
     * @param characterId 角色ID
     * @param tagIds 标签ID数组（JSON格式）
     * @param tagNames 标签名称数组（JSON格式）
     * @param primaryTagIds 主要标签ID数组（JSON格式）
     * @param tagSummary 标签摘要
     * @return 影响的行数
     */
    @Update("UPDATE vocata_character SET " +
            "tag_ids = #{tagIds}::bigint[], " +
            "tag_names = #{tagNames}::text[], " +
            "primary_tag_ids = #{primaryTagIds}::bigint[], " +
            "tag_summary = #{tagSummary}, " +
            "updated_at = NOW() " +
            "WHERE id = #{characterId}")
    int updateCharacterTags(@Param("characterId") Long characterId,
                           @Param("tagIds") String tagIds,
                           @Param("tagNames") String tagNames,
                           @Param("primaryTagIds") String primaryTagIds,
                           @Param("tagSummary") String tagSummary);

    /**
     * 获取公开角色列表（包含创建者名称）
     * @param page 分页参数
     * @param status 角色状态
     * @param isFeatured 是否精选
     * @return 角色列表（包含创建者名称）
     */
    @Select("<script>" +
            "SELECT c.*, " +
            "CASE " +
            "  WHEN c.is_official = 1 THEN '官方' " +
            "  WHEN c.create_id IS NULL THEN '官方' " +
            "  ELSE COALESCE(u.nickname, u.username, '未知用户') " +
            "END as creator_name " +
            "FROM vocata_character c " +
            "LEFT JOIN vocata_user u ON c.create_id = u.id " +
            "WHERE c.is_private = false " +
            "AND c.is_delete = 0 " +
            "<if test='status != null'> AND c.status = #{status} </if>" +
            "<if test='isFeatured != null'> AND c.is_featured = #{isFeatured} </if>" +
            "ORDER BY c.chat_count DESC, c.created_at DESC" +
            "</script>")
    IPage<Map<String, Object>> selectPublicCharactersWithCreator(Page<?> page,
                                                                @Param("status") Integer status,
                                                                @Param("isFeatured") Integer isFeatured);

    /**
     * 获取精选角色列表（包含创建者名称）
     * @param limit 限制数量
     * @return 精选角色列表（包含创建者名称）
     */
    @Select("SELECT c.*, " +
            "CASE " +
            "  WHEN c.is_official = 1 THEN '官方' " +
            "  WHEN c.create_id IS NULL THEN '官方' " +
            "  ELSE COALESCE(u.nickname, u.username, '未知用户') " +
            "END as creator_name " +
            "FROM vocata_character c " +
            "LEFT JOIN vocata_user u ON c.create_id = u.id " +
            "WHERE c.is_private = false " +
            "AND c.status = 1 " +
            "AND c.is_featured = 1 " +
            "AND c.is_delete = 0 " +
            "ORDER BY c.sort_weight DESC, c.created_at DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectFeaturedCharactersWithCreator(@Param("limit") int limit);

    /**
     * 获取热门角色列表（包含创建者名称）
     * @param limit 限制数量
     * @return 热门角色列表（包含创建者名称）
     */
    @Select("SELECT c.*, " +
            "CASE " +
            "  WHEN c.is_official = 1 THEN '官方' " +
            "  WHEN c.create_id IS NULL THEN '官方' " +
            "  ELSE COALESCE(u.nickname, u.username, '未知用户') " +
            "END as creator_name " +
            "FROM vocata_character c " +
            "LEFT JOIN vocata_user u ON c.create_id = u.id " +
            "WHERE c.is_private = false " +
            "AND c.status = 1 " +
            "AND c.is_trending = 1 " +
            "AND c.is_delete = 0 " +
            "ORDER BY c.trending_score DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTrendingCharactersWithCreator(@Param("limit") int limit);

}