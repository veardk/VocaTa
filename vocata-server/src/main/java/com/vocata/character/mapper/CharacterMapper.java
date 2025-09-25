package com.vocata.character.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocata.character.entity.Character;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

}