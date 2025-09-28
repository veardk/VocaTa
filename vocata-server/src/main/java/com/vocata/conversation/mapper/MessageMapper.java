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
     * 根据对话ID查找最新的指定数量消息，按创建时间倒序
     * 用于对话界面显示最近消息
     *
     * @param conversationId 对话ID
     * @param limit 限制数量，默认20，最大100
     * @return 消息列表，按创建时间倒序（最新的在前）
     */
    @Select("SELECT * FROM vocata_messages WHERE conversation_id = #{conversationId} AND is_delete = 0 ORDER BY create_date DESC LIMIT #{limit}")
    List<Message> findRecentMessagesByConversationId(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    /**
     * 根据对话ID分页查找历史消息，按创建时间倒序
     * 用于向前翻页查看历史消息
     *
     * @param conversationId 对话ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 消息列表，按创建时间倒序
     */
    @Select("SELECT * FROM vocata_messages WHERE conversation_id = #{conversationId} AND is_delete = 0 ORDER BY create_date DESC LIMIT #{limit} OFFSET #{offset}")
    List<Message> findMessagesByConversationIdWithPagination(@Param("conversationId") Long conversationId,
                                                             @Param("offset") int offset,
                                                             @Param("limit") int limit);

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