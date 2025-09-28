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
     * @deprecated 建议使用 getConversationRecentMessages 方法
     */
    @Deprecated
    List<MessageResponse> getConversationMessages(UUID conversationUuid);

    /**
     * 获取指定对话的最新消息（默认20条）
     * 适用于对话界面的初始加载，按时间倒序返回（最新消息在前）
     *
     * @param conversationUuid 对话UUID
     * @return 最新消息列表，最新消息在前
     */
    List<MessageResponse> getConversationRecentMessages(UUID conversationUuid);

    /**
     * 获取指定对话的最新消息（自定义数量）
     * 适用于对话界面的初始加载，按时间倒序返回（最新消息在前）
     *
     * @param conversationUuid 对话UUID
     * @param limit 限制数量（1-100）
     * @return 最新消息列表，最新消息在前
     */
    List<MessageResponse> getConversationRecentMessages(UUID conversationUuid, int limit);

    /**
     * 分页获取对话的历史消息
     * 适用于向前翻页查看历史消息，按时间倒序返回
     *
     * @param conversationUuid 对话UUID
     * @param offset 偏移量（从0开始）
     * @param limit 限制数量（1-100）
     * @return 历史消息列表，按时间倒序
     */
    List<MessageResponse> getConversationMessagesWithPagination(UUID conversationUuid, int offset, int limit);

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

    /**
     * 基于首次消息自动生成对话标题
     * @param conversationId 对话ID
     * @param firstMessage 首次用户消息内容
     */
    void generateConversationTitleAsync(Long conversationId, String firstMessage);

    /**
     * 检查并触发新对话的标题生成
     * 当对话满足条件时（第一次创建且有完整的一问一答），自动生成标题
     * @param conversationId 对话ID
     */
    void triggerTitleGenerationForNewConversation(Long conversationId);

    /**
     * 更新对话标题
     * @param conversationUuid 对话UUID
     * @param userId 用户ID
     * @param newTitle 新标题
     */
    void updateConversationTitle(UUID conversationUuid, Long userId, String newTitle);
}