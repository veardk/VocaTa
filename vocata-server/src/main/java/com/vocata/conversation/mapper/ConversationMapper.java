package com.vocata.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocata.conversation.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

/**
 * 对话会话Mapper接口
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /**
     * 根据UUID查找对话
     */
    @Select("SELECT * FROM vocata_conversations WHERE conversation_uuid = #{conversationUuid} AND is_delete = 0")
    Conversation findByConversationUuid(@Param("conversationUuid") UUID conversationUuid);

    /**
     * 根据用户ID查找所有对话，按更新时间倒序
     */
    @Select("SELECT * FROM vocata_conversations WHERE user_id = #{userId} AND is_delete = 0 ORDER BY update_date DESC")
    List<Conversation> findByUserIdOrderByUpdateDateDesc(@Param("userId") Long userId);

    /**
     * 根据用户ID和角色ID查找对话
     */
    @Select("SELECT * FROM vocata_conversations WHERE user_id = #{userId} AND character_id = #{characterId} AND is_delete = 0 ORDER BY update_date DESC")
    List<Conversation> findByUserIdAndCharacterId(@Param("userId") Long userId, @Param("characterId") Long characterId);

    /**
     * 根据用户ID和状态查找对话
     */
    @Select("SELECT * FROM vocata_conversations WHERE user_id = #{userId} AND status = #{status} AND is_delete = 0 ORDER BY update_date DESC")
    List<Conversation> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 根据用户ID查找所有对话，按创建时间倒序（最新创建的在前）
     */
    @Select("SELECT * FROM vocata_conversations WHERE user_id = #{userId} AND is_delete = 0 ORDER BY create_date DESC")
    List<Conversation> findByUserIdOrderByCreateDateDesc(@Param("userId") Long userId);
}