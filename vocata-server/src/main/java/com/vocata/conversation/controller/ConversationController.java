package com.vocata.conversation.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.vocata.common.result.ApiResponse;
import com.vocata.common.utils.UserContext;
import com.vocata.conversation.dto.request.CreateConversationRequest;
import com.vocata.conversation.dto.request.UpdateTitleRequest;
import com.vocata.conversation.dto.response.ConversationResponse;
import com.vocata.conversation.dto.response.MessageResponse;
import com.vocata.conversation.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 对话会话控制器
 */
@RestController
@RequestMapping("/api/client/conversations")
@SaCheckLogin
public class ConversationController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);

    @Autowired
    private ConversationService conversationService;

    /**
     * 获取当前用户的对话列表
     * GET /api/conversations
     */
    @GetMapping
    public ApiResponse<List<ConversationResponse>> getUserConversations() {
        Long userId = UserContext.getUserId();
        logger.info("获取用户{}的对话列表", userId);

        List<ConversationResponse> conversations = conversationService.getUserConversations(userId);

        return ApiResponse.success(conversations);
    }

    /**
     * 创建新的对话会话
     * POST /api/conversations
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> createConversation(@Validated @RequestBody CreateConversationRequest request) {
        Long userId = UserContext.getUserId();
        logger.info("用户{}创建新对话，角色ID: {}", userId, request.getCharacterId());

        ConversationResponse conversation = conversationService.createConversation(userId, request);

        return ApiResponse.success(conversation);
    }

    /**
     * 获取指定对话的所有消息
     * GET /api/conversations/{conversation_uuid}/messages
     */
    @GetMapping("/{conversationUuid}/messages")
    public ApiResponse<List<MessageResponse>> getConversationMessages(
            @PathVariable("conversationUuid") String conversationUuidStr) {
        Long userId = UserContext.getUserId();
        UUID conversationUuid = UUID.fromString(conversationUuidStr);

        logger.info("用户{}获取对话{}的消息列表", userId, conversationUuid);

        // 验证对话是否属于当前用户
        if (!conversationService.validateConversationOwnership(conversationUuid, userId)) {
            return ApiResponse.error(403, "无权限访问此对话");
        }

        List<MessageResponse> messages = conversationService.getConversationMessages(conversationUuid);

        return ApiResponse.success(messages);
    }

    /**
     * 归档对话
     * PUT /api/conversations/{conversation_uuid}/archive
     */
    @PutMapping("/{conversationUuid}/archive")
    public ApiResponse<Void> archiveConversation(@PathVariable("conversationUuid") String conversationUuidStr) {
        Long userId = UserContext.getUserId();
        UUID conversationUuid = UUID.fromString(conversationUuidStr);

        logger.info("用户{}归档对话{}", userId, conversationUuid);

        conversationService.archiveConversation(conversationUuid, userId);

        return ApiResponse.success("对话已归档");
    }

    /**
     * 删除对话
     * DELETE /api/conversations/{conversation_uuid}
     */
    @DeleteMapping("/{conversationUuid}")
    public ApiResponse<Void> deleteConversation(@PathVariable("conversationUuid") String conversationUuidStr) {
        Long userId = UserContext.getUserId();
        UUID conversationUuid = UUID.fromString(conversationUuidStr);

        logger.info("用户{}删除对话{}", userId, conversationUuid);

        conversationService.deleteConversation(conversationUuid, userId);

        return ApiResponse.success("对话已删除");
    }

    /**
     * 更新对话标题
     * PUT /api/conversations/{conversation_uuid}/title
     */
    @PutMapping("/{conversationUuid}/title")
    public ApiResponse<Void> updateConversationTitle(
            @PathVariable("conversationUuid") String conversationUuidStr,
            @RequestBody UpdateTitleRequest request) {
        Long userId = UserContext.getUserId();
        UUID conversationUuid = UUID.fromString(conversationUuidStr);

        logger.info("用户{}更新对话{}的标题", userId, conversationUuid);

        conversationService.updateConversationTitle(conversationUuid, userId, request.getTitle());

        return ApiResponse.success("标题更新成功");
    }

}