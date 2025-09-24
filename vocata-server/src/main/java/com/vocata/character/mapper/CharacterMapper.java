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
            "update_date = NOW() " +
            "WHERE id = #{characterId}")
    int incrementChatCount(@Param("characterId") Long characterId, @Param("increment") int increment);

}