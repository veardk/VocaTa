package com.vocata.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocata.conversation.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

/**
 * 消息Mapper接口
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 根据UUID查找消息
     */
    @Select("SELECT * FROM vocata_messages WHERE message_uuid = #{messageUuid} AND is_delete = 0")
    Message findByMessageUuid(@Param("messageUuid") UUID messageUuid);

    /**
     * 根据对话ID查找所有消息，按创建时间升序
     */
    @Select("SELECT * FROM vocata_messages WHERE conversation_id = #{conversationId} AND is_delete = 0 ORDER BY create_date ASC")
    List<Message> findByConversationIdOrderByCreateDateAsc(@Param("conversationId") Long conversationId);

    /**
     * 根据对话ID查找最后一条消息
     */
    @Select("SELECT * FROM vocata_messages WHERE conversation_id = #{conversationId} AND is_delete = 0 ORDER BY create_date DESC LIMIT 1")
    Message findLastMessageByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 根据对话ID和发送方类型查找消息
     */
    @Select("SELECT * FROM vocata_messages WHERE conversation_id = #{conversationId} AND sender_type = #{senderType} AND is_delete = 0 ORDER BY create_date ASC")
    List<Message> findByConversationIdAndSenderType(@Param("conversationId") Long conversationId, @Param("senderType") Integer senderType);

    /**
     * 统计对话中的消息数量
     */
    @Select("SELECT COUNT(*) FROM vocata_messages WHERE conversation_id = #{conversationId} AND is_delete = 0")
    int countByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 根据对话ID删除所有消息（软删除）
     */
    @Select("UPDATE vocata_messages SET is_delete = 1 WHERE conversation_id = #{conversationId}")
    void softDeleteByConversationId(@Param("conversationId") Long conversationId);
}