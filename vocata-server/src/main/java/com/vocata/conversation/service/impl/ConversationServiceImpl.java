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
import com.vocata.conversation.service.ConversationTitleGenerationService;
import com.vocata.ai.llm.LlmProvider;
import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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

    @Autowired
    @Qualifier("primaryLlmProvider")
    private LlmProvider llmProvider;

    @Autowired
    private ConversationTitleGenerationService titleGenerationService;

    @Value("${gemini.api.default-model:gemini-2.5-flash-lite}")
    private String defaultLlmModel;

    @Override
    public List<ConversationResponse> getUserConversations(Long userId) {
        logger.info("获取用户{}的对话列表", userId);

        // 按创建时间降序排序 - 最新创建的对话在最前面
        List<Conversation> conversations = conversationMapper.findByUserIdOrderByCreateDateDesc(userId);

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
        logger.warn("使用已废弃的方法 getConversationMessages，建议使用 getConversationRecentMessages");
        logger.info("获取对话{}的所有消息", conversationUuid);

        // 验证对话是否存在
        Conversation conversation = getConversationByUuid(conversationUuid);

        // 获取消息列表（升序，保持向后兼容）
        List<Message> messages = messageMapper.findByConversationIdOrderByCreateDateAsc(conversation.getId());

        return messages.stream().map(this::convertMessageToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MessageResponse> getConversationRecentMessages(UUID conversationUuid) {
        return getConversationRecentMessages(conversationUuid, 20); // 默认20条
    }

    @Override
    public List<MessageResponse> getConversationRecentMessages(UUID conversationUuid, int limit) {
        logger.info("获取对话{}的最新{}条消息", conversationUuid, limit);

        // 参数验证
        if (limit <= 0 || limit > 100) {
            throw new BizException(ApiCode.INVALID_PARAM, "消息数量限制必须在1-100之间");
        }

        // 验证对话是否存在
        Conversation conversation = getConversationByUuid(conversationUuid);

        // 获取最新消息列表（倒序，最新的在前）
        List<Message> messages = messageMapper.findRecentMessagesByConversationId(conversation.getId(), limit);

        return messages.stream().map(this::convertMessageToResponse).collect(Collectors.toList());
    }

    @Override
    public List<MessageResponse> getConversationMessagesWithPagination(UUID conversationUuid, int offset, int limit) {
        logger.info("分页获取对话{}的消息，offset: {}, limit: {}", conversationUuid, offset, limit);

        // 参数验证
        if (offset < 0) {
            throw new BizException(ApiCode.INVALID_PARAM, "偏移量不能为负数");
        }
        if (limit <= 0 || limit > 100) {
            throw new BizException(ApiCode.INVALID_PARAM, "消息数量限制必须在1-100之间");
        }

        // 验证对话是否存在
        Conversation conversation = getConversationByUuid(conversationUuid);

        // 分页获取消息列表（倒序）
        List<Message> messages = messageMapper.findMessagesByConversationIdWithPagination(
                conversation.getId(), offset, limit);

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

        // 使用MyBatis Plus的deleteById方法进行软删除
        // 这将自动触发逻辑删除机制，设置 is_delete = 1
        conversationMapper.deleteById(conversation.getId());
        logger.info("已软删除对话，ID: {}", conversation.getId());

        // 软删除相关消息
        messageMapper.softDeleteByConversationId(conversation.getId());
        logger.info("已软删除对话{}的所有相关消息", conversation.getId());
    }

    @Override
    @Async
    public void generateConversationTitleAsync(Long conversationId, String firstMessage) {
        logger.info("开始异步生成对话{}的标题，基于首次消息: {}", conversationId, firstMessage);

        try {
            // 构建生成标题的提示词
            String titlePrompt = "请根据用户的第一句话，为这次对话生成一个简短、准确的标题（不超过20个字符）。" +
                    "只需要返回标题本身，不要有任何额外的解释或格式。\n" +
                    "用户的话: " + firstMessage;

            // 调用LLM生成标题 - 构建请求对象
            UnifiedAiRequest titleRequest = new UnifiedAiRequest();
            titleRequest.setUserMessage(titlePrompt);
            titleRequest.setSystemPrompt("你是一个专业的对话标题生成助手。请根据用户的第一句话，生成一个简短、准确的中文对话标题。");

            // 设置简单的模型配置
            UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
            modelConfig.setModelName(defaultLlmModel); // 使用配置的LLM模型
            modelConfig.setTemperature(0.3); // 较低温度确保生成的标题较为稳定
            titleRequest.setModelConfig(modelConfig);

            UnifiedAiStreamChunk titleChunk = llmProvider.chat(titleRequest);
            String generatedTitle = titleChunk != null ? titleChunk.getAccumulatedContent() : null;

            // 清理生成的标题（去除引号和多余的空格）
            if (generatedTitle != null) {
                generatedTitle = generatedTitle.trim()
                        .replaceAll("^[\"'`]+|[\"'`]+$", "") // 去除首尾引号
                        .substring(0, Math.min(generatedTitle.length(), 50)); // 限制长度

                if (generatedTitle.isEmpty()) {
                    generatedTitle = "新对话";
                }
            } else {
                generatedTitle = "新对话";
            }

            // 更新数据库中的标题
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation != null) {
                conversation.setTitle(generatedTitle);
                conversation.setUpdateId(conversation.getUserId());
                conversationMapper.updateById(conversation);

                logger.info("成功生成并更新对话{}的标题: {}", conversationId, generatedTitle);
            }

        } catch (Exception e) {
            logger.error("生成对话{}标题时出错: {}", conversationId, e.getMessage(), e);
            // 发生错误时设置默认标题
            try {
                Conversation conversation = conversationMapper.selectById(conversationId);
                if (conversation != null && (conversation.getTitle() == null || conversation.getTitle().trim().isEmpty())) {
                    conversation.setTitle("新对话");
                    conversation.setUpdateId(conversation.getUserId());
                    conversationMapper.updateById(conversation);
                }
            } catch (Exception ex) {
                logger.error("设置默认标题时也出错了: {}", ex.getMessage());
            }
        }
    }

    /**
     * 新方法：基于一问一答生成对话标题
     *
     * @param conversationId 对话ID
     */
    @Override
    public void triggerTitleGenerationForNewConversation(Long conversationId) {
        logger.info("检查对话{}是否需要生成标题", conversationId);

        if (titleGenerationService.shouldGenerateTitle(conversationId)) {
            logger.info("对话{}满足标题生成条件，开始异步生成", conversationId);
            titleGenerationService.generateTitleAsync(conversationId);
        } else {
            logger.debug("对话{}不满足标题生成条件，跳过", conversationId);
        }
    }

    @Override
    @Transactional
    public void updateConversationTitle(UUID conversationUuid, Long userId, String newTitle) {
        logger.info("用户{}更新对话{}的标题为: {}", userId, conversationUuid, newTitle);

        Conversation conversation = conversationMapper.findByConversationUuid(conversationUuid);
        if (conversation == null) {
            throw new BizException(ApiCode.CONVERSATION_NOT_EXIST);
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new BizException(ApiCode.FORBIDDEN, "无权限操作此对话");
        }

        // 验证标题长度
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new BizException(ApiCode.INVALID_PARAM, "标题不能为空");
        }

        if (newTitle.length() > 100) {
            throw new BizException(ApiCode.INVALID_PARAM, "标题长度不能超过100个字符");
        }

        conversation.setTitle(newTitle.trim());
        conversation.setUpdateId(userId);
        conversationMapper.updateById(conversation);

        logger.info("成功更新对话{}的标题", conversationUuid);
    }

    /**
     * 将对话实体转换为响应DTO
     */
    private ConversationResponse convertToResponse(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();

        // conversation_uuid是永久不变的唯一标识，绝不能修改
        if (conversation.getConversationUuid() != null) {
            response.setConversationUuid(conversation.getConversationUuid().toString());
        } else {
            logger.error("严重错误：对话ID {}的conversation_uuid为NULL，这违反了数据完整性约束", conversation.getId());
            throw new BizException(ApiCode.ERROR, "对话数据异常，请联系管理员");
        }

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
                response.setGreeting(character.getGreeting());
            }
        }

        return response;
    }

    /**
     * 将消息实体转换为响应DTO
     */
    private MessageResponse convertMessageToResponse(Message message) {
        MessageResponse response = new MessageResponse();

        // 防护性检查UUID - 临时修复，待TypeHandler修复生效后可删除
        if (message.getMessageUuid() != null) {
            response.setMessageUuid(message.getMessageUuid().toString());
        } else {
            // 数据库中有UUID但Java对象中为null，这是TypeHandler问题
            logger.warn("消息ID {}的UUID在Java对象中为null，这可能是TypeHandler问题", message.getId());
            response.setMessageUuid("uuid-missing-" + message.getId()); // 临时处理
        }

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
