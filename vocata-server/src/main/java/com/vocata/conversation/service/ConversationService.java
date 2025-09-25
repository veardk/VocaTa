package com.vocata.conversation.service;

import com.vocata.conversation.dto.request.CreateConversationRequest;
import com.vocata.conversation.dto.response.ConversationResponse;
import com.vocata.conversation.dto.response.MessageResponse;
import com.vocata.conversation.entity.Conversation;

import java.util.List;
import java.util.UUID;

/**
 * 对话会话服务接口
 */
public interface ConversationService {

    /**
     * 获取当前用户的所有对话列表，按更新时间倒序
     */
    List<ConversationResponse> getUserConversations(Long userId);

    /**
     * 创建新的对话会话
     */
    ConversationResponse createConversation(Long userId, CreateConversationRequest request);

    /**
     * 根据UUID获取对话详情
     */
    Conversation getConversationByUuid(UUID conversationUuid);

    /**
     * 根据UUID验证对话是否属于指定用户
     */
    boolean validateConversationOwnership(UUID conversationUuid, Long userId);

    /**
     * 获取指定对话的所有消息，按创建时间升序
     */
    List<MessageResponse> getConversationMessages(UUID conversationUuid);

    /**
     * 更新对话的最后消息摘要
     */
    void updateLastMessageSummary(Long conversationId, String summary);

    /**
     * 归档对话
     */
    void archiveConversation(UUID conversationUuid, Long userId);

    /**
     * 删除对话（软删除）
     */
    void deleteConversation(UUID conversationUuid, Long userId);
}