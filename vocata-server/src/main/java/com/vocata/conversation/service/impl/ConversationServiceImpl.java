package com.vocata.conversation.service.impl;

import com.vocata.character.mapper.CharacterMapper;
import com.vocata.character.entity.Character;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.utils.UserContext;
import com.vocata.conversation.constants.ConversationStatus;
import com.vocata.conversation.dto.request.CreateConversationRequest;
import com.vocata.conversation.dto.response.ConversationResponse;
import com.vocata.conversation.dto.response.MessageResponse;
import com.vocata.conversation.entity.Conversation;
import com.vocata.conversation.entity.Message;
import com.vocata.conversation.mapper.ConversationMapper;
import com.vocata.conversation.mapper.MessageMapper;
import com.vocata.conversation.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 对话会话服务实现类
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationServiceImpl.class);

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private CharacterMapper characterMapper;

    @Override
    public List<ConversationResponse> getUserConversations(Long userId) {
        logger.info("获取用户{}的对话列表", userId);

        List<Conversation> conversations = conversationMapper.findByUserIdOrderByUpdateDateDesc(userId);

        return conversations.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationResponse createConversation(Long userId, CreateConversationRequest request) {
        logger.info("用户{}创建与角色{}的新对话", userId, request.getCharacterId());

        // 验证角色是否存在
        Character character = characterMapper.selectById(request.getCharacterId());
        if (character == null || character.getIsDelete() == 1) {
            throw new BizException(ApiCode.INVALID_PARAM, "角色不存在或已删除");
        }

        // 创建新对话
        Conversation conversation = new Conversation();
        conversation.setConversationUuid(UUID.randomUUID());
        conversation.setUserId(userId);
        conversation.setCharacterId(request.getCharacterId());
        conversation.setTitle(request.getTitle());
        conversation.setStatus(ConversationStatus.ACTIVE.getCode());
        conversation.setCreateId(userId);
        conversation.setUpdateId(userId);

        conversationMapper.insert(conversation);

        logger.info("成功创建对话，UUID: {}", conversation.getConversationUuid());

        // 转换并返回响应
        return convertToResponse(conversation);
    }

    @Override
    public Conversation getConversationByUuid(UUID conversationUuid) {
        Conversation conversation = conversationMapper.findByConversationUuid(conversationUuid);
        if (conversation == null) {
            throw new BizException(ApiCode.CONVERSATION_NOT_EXIST);
        }
        return conversation;
    }

    @Override
    public boolean validateConversationOwnership(UUID conversationUuid, Long userId) {
        Conversation conversation = conversationMapper.findByConversationUuid(conversationUuid);
        return conversation != null && conversation.getUserId().equals(userId);
    }

    @Override
    public List<MessageResponse> getConversationMessages(UUID conversationUuid) {
        logger.info("获取对话{}的消息列表", conversationUuid);

        // 验证对话是否存在
        Conversation conversation = getConversationByUuid(conversationUuid);

        // 获取消息列表
        List<Message> messages = messageMapper.findByConversationIdOrderByCreateDateAsc(conversation.getId());

        return messages.stream().map(this::convertMessageToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateLastMessageSummary(Long conversationId, String summary) {
        logger.info("更新对话{}的最后消息摘要", conversationId);

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setLastMessageSummary(summary);
            conversation.setUpdateId(UserContext.getUserId());
            conversationMapper.updateById(conversation);
        }
    }

    @Override
    @Transactional
    public void archiveConversation(UUID conversationUuid, Long userId) {
        logger.info("用户{}归档对话{}", userId, conversationUuid);

        Conversation conversation = conversationMapper.findByConversationUuid(conversationUuid);
        if (conversation == null) {
            throw new BizException(ApiCode.CONVERSATION_NOT_EXIST);
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new BizException(ApiCode.FORBIDDEN, "无权限操作此对话");
        }

        conversation.setStatus(ConversationStatus.ARCHIVED.getCode());
        conversation.setUpdateId(userId);
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional
    public void deleteConversation(UUID conversationUuid, Long userId) {
        logger.info("用户{}删除对话{}", userId, conversationUuid);

        Conversation conversation = conversationMapper.findByConversationUuid(conversationUuid);
        if (conversation == null) {
            throw new BizException(ApiCode.CONVERSATION_NOT_EXIST);
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new BizException(ApiCode.FORBIDDEN, "无权限操作此对话");
        }

        // 软删除对话
        conversation.setIsDelete(1);
        conversation.setUpdateId(userId);
        conversationMapper.updateById(conversation);

        // 软删除相关消息
        messageMapper.softDeleteByConversationId(conversation.getId());
    }

    /**
     * 将对话实体转换为响应DTO
     */
    private ConversationResponse convertToResponse(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setConversationUuid(conversation.getConversationUuid().toString());
        response.setCharacterId(conversation.getCharacterId().toString());
        response.setTitle(conversation.getTitle());
        response.setLastMessageSummary(conversation.getLastMessageSummary());
        response.setStatus(conversation.getStatus());
        response.setCreateDate(conversation.getCreateDate());
        response.setUpdateDate(conversation.getUpdateDate());

        // 获取角色信息
        if (conversation.getCharacterId() != null) {
            Character character = characterMapper.selectById(conversation.getCharacterId());
            if (character != null) {
                response.setCharacterName(character.getName());
                response.setCharacterAvatarUrl(character.getAvatarUrl());
            }
        }

        return response;
    }

    /**
     * 将消息实体转换为响应DTO
     */
    private MessageResponse convertMessageToResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setMessageUuid(message.getMessageUuid().toString());
        response.setSenderType(message.getSenderType());
        response.setContentType(message.getContentType());
        response.setTextContent(message.getTextContent());
        response.setAudioUrl(message.getAudioUrl());
        response.setLlmModelId(message.getLlmModelId());
        response.setTtsVoiceId(message.getTtsVoiceId());
        response.setMetadata(message.getMetadata());
        response.setCreateDate(message.getCreateDate());

        return response;
    }
}