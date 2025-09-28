package com.vocata.conversation.service;

import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.LlmProvider;
import com.vocata.conversation.entity.Conversation;
import com.vocata.conversation.entity.Message;
import com.vocata.conversation.mapper.ConversationMapper;
import com.vocata.conversation.mapper.MessageMapper;
import com.vocata.conversation.constants.SenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 对话标题自动生成服务
 *
 * 根据对话的第一轮问答（用户问题 + AI回答）自动生成简短的对话标题
 * 只有当对话是首次创建且没有标题时才会触发生成
 */
@Service
public class ConversationTitleGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationTitleGenerationService.class);

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    @Qualifier("siliconFlowLlmProvider")
    private LlmProvider titleGenerationLlmProvider;

    @Value("${siliconflow.ai.default-model:Qwen/Qwen2.5-7B-Instruct}")
    private String titleGenerationModel;

    /**
     * 检查对话是否需要生成标题
     *
     * @param conversationId 对话ID
     * @return true 如果需要生成标题，false 如果不需要
     */
    public boolean shouldGenerateTitle(Long conversationId) {
        try {
            // 检查对话是否存在
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                logger.debug("对话不存在，跳过标题生成: {}", conversationId);
                return false;
            }

            // 检查是否已有标题
            if (conversation.getTitle() != null && !conversation.getTitle().trim().isEmpty()) {
                logger.debug("对话已有标题，跳过生成: {} - 标题: {}", conversationId, conversation.getTitle());
                return false;
            }

            // 检查消息数量，确保有至少一轮完整对话（用户消息 + AI回复）
            List<Message> messages = messageMapper.findRecentMessagesByConversationId(conversationId, 10);
            if (messages.size() < 2) {
                logger.debug("对话消息数量不足，跳过标题生成: {} - 消息数: {}", conversationId, messages.size());
                return false;
            }

            // 检查是否有用户消息和AI回复
            boolean hasUserMessage = messages.stream()
                    .anyMatch(msg -> msg.getSenderType() == SenderType.USER.getCode());
            boolean hasAiMessage = messages.stream()
                    .anyMatch(msg -> msg.getSenderType() == SenderType.CHARACTER.getCode());

            if (!hasUserMessage || !hasAiMessage) {
                logger.debug("对话缺少完整的问答，跳过标题生成: {} - 用户消息: {}, AI消息: {}",
                        conversationId, hasUserMessage, hasAiMessage);
                return false;
            }

            logger.info("对话满足标题生成条件: {}", conversationId);
            return true;

        } catch (Exception e) {
            logger.error("检查标题生成条件时出错，对话ID: {}", conversationId, e);
            return false;
        }
    }

    /**
     * 异步生成对话标题
     * 基于对话中的第一轮问答内容生成简短、准确的标题
     *
     * @param conversationId 对话ID
     */
    @Async
    public void generateTitleAsync(Long conversationId) {
        try {
            logger.info("开始异步生成对话标题: {}", conversationId);

            // 稍微延迟确保主线程的事务已提交
            Thread.sleep(500);

            // 再次检查是否需要生成标题（防止并发情况）
            if (!shouldGenerateTitle(conversationId)) {
                logger.info("对话不需要生成标题，结束处理: {}", conversationId);
                return;
            }

            // 获取对话的所有消息，按时间正序（最老的在前）
            List<Message> messages = messageMapper.findByConversationIdOrderByCreateDateAsc(conversationId);
            logger.debug("查询到对话消息数量: {}", messages.size());

            if (messages.size() < 2) {
                logger.warn("对话消息数量不足，无法生成标题: {} - 消息数: {}", conversationId, messages.size());
                return;
            }

            // 找到第一条用户消息和第一条AI回复（按时间顺序）
            String firstUserMessage = null;
            String firstAiReply = null;

            // 按时间正序查找第一轮对话
            for (Message message : messages) {
                logger.debug("处理消息: messageId={}, senderType={}, content={}...",
                    message.getId(), message.getSenderType(),
                    message.getTextContent() != null ? message.getTextContent().substring(0, Math.min(50, message.getTextContent().length())) : "null");

                if (firstUserMessage == null && message.getSenderType() == SenderType.USER.getCode()) {
                    firstUserMessage = message.getTextContent();
                    logger.debug("找到第一条用户消息");
                } else if (firstUserMessage != null && firstAiReply == null &&
                          message.getSenderType() == SenderType.CHARACTER.getCode()) {
                    firstAiReply = message.getTextContent();
                    logger.debug("找到第一条AI回复");
                    break; // 找到完整的一轮对话后停止
                }
            }

            if (firstUserMessage == null || firstAiReply == null) {
                logger.warn("未找到完整的第一轮对话，无法生成标题: {} - 用户消息: {}, AI回复: {}",
                    conversationId, firstUserMessage != null, firstAiReply != null);
                return;
            }

            logger.info("找到完整的第一轮对话，开始生成标题: {}", conversationId);

            // 调用AI生成标题
            String generatedTitle = generateTitleWithAi(firstUserMessage, firstAiReply);

            if (generatedTitle != null && !generatedTitle.trim().isEmpty()) {
                // 更新数据库中的标题
                Conversation conversation = conversationMapper.selectById(conversationId);
                if (conversation != null && (conversation.getTitle() == null || conversation.getTitle().trim().isEmpty())) {
                    conversation.setTitle(generatedTitle);
                    conversation.setUpdateId(conversation.getUserId());
                    conversationMapper.updateById(conversation);

                    logger.info("成功生成并更新对话标题: {} -> {}", conversationId, generatedTitle);
                } else {
                    logger.info("对话已有标题，跳过更新: {}", conversationId);
                }
            } else {
                logger.warn("AI生成的标题为空，设置默认标题: {}", conversationId);
                setDefaultTitle(conversationId);
            }

        } catch (Exception e) {
            logger.error("生成对话标题失败: {}", conversationId, e);
            try {
                setDefaultTitle(conversationId);
            } catch (Exception ex) {
                logger.error("设置默认标题也失败了: {}", conversationId, ex);
            }
        }
    }

    /**
     * 使用AI生成对话标题
     *
     * @param userMessage 用户的第一条消息
     * @param aiReply AI的第一条回复
     * @return 生成的标题，失败时返回null
     */
    private String generateTitleWithAi(String userMessage, String aiReply) {
        try {
            // 检查LLM提供者是否可用
            if (!titleGenerationLlmProvider.isAvailable()) {
                logger.warn("硅基流动LLM提供者不可用，无法生成标题");
                return null;
            }

            // 构建标题生成的提示词
            String titlePrompt = String.format(
                "请根据以下对话内容，生成一个简短、准确的中文标题（不超过15个字符）。只需要返回标题本身，不要有任何额外的解释、引号或格式。\n\n" +
                "用户问：%s\n\n" +
                "AI答：%s\n\n" +
                "标题要求：\n" +
                "1. 简洁明了，不超过15个字符\n" +
                "2. 准确概括对话主题\n" +
                "3. 使用中文\n" +
                "4. 不要使用引号、书名号等标点符号\n" +
                "5. 直接返回标题，不要任何前缀后缀",
                userMessage.length() > 200 ? userMessage.substring(0, 200) + "..." : userMessage,
                aiReply.length() > 200 ? aiReply.substring(0, 200) + "..." : aiReply
            );

            // 构建AI请求
            UnifiedAiRequest titleRequest = new UnifiedAiRequest();
            titleRequest.setUserMessage(titlePrompt);
            titleRequest.setSystemPrompt("你是一个专业的对话标题生成助手。请根据用户提供的对话内容，生成一个简短、准确的中文标题。只返回标题，不要任何额外内容。");

            // 设置模型配置，使用免费的硅基流动模型
            UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
            modelConfig.setModelName(titleGenerationModel);
            modelConfig.setTemperature(0.3); // 较低温度确保生成稳定
            modelConfig.setMaxTokens(50); // 限制token数量，标题应该很短
            titleRequest.setModelConfig(modelConfig);

            logger.debug("开始调用AI生成标题，模型: {}", titleGenerationModel);

            // 调用AI服务生成标题
            UnifiedAiStreamChunk titleChunk = titleGenerationLlmProvider.chat(titleRequest);
            String generatedTitle = titleChunk != null ? titleChunk.getAccumulatedContent() : null;

            if (generatedTitle != null) {
                // 清理生成的标题
                generatedTitle = cleanGeneratedTitle(generatedTitle);
                logger.info("AI生成标题成功: {}", generatedTitle);
                return generatedTitle;
            } else {
                logger.warn("AI返回的标题为空");
                return null;
            }

        } catch (Exception e) {
            logger.error("调用AI生成标题时出错", e);
            return null;
        }
    }

    /**
     * 清理AI生成的标题
     *
     * @param rawTitle 原始生成的标题
     * @return 清理后的标题
     */
    private String cleanGeneratedTitle(String rawTitle) {
        if (rawTitle == null) {
            return null;
        }

        String cleanedTitle = rawTitle.trim();

        // 去除常见的引号和标点符号
        cleanedTitle = cleanedTitle.replaceAll("^[\"'`『』「」【】]+|[\"'`『』「」【】]+$", "");

        // 去除可能的前缀
        cleanedTitle = cleanedTitle.replaceAll("^(标题：|标题:|题目：|题目:)", "");

        // 限制长度为15个字符
        if (cleanedTitle.length() > 15) {
            cleanedTitle = cleanedTitle.substring(0, 15);
        }

        // 如果清理后为空，返回默认值
        if (cleanedTitle.trim().isEmpty()) {
            cleanedTitle = "新对话";
        }

        return cleanedTitle.trim();
    }

    /**
     * 设置默认标题
     *
     * @param conversationId 对话ID
     */
    private void setDefaultTitle(Long conversationId) {
        try {
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation != null && (conversation.getTitle() == null || conversation.getTitle().trim().isEmpty())) {
                conversation.setTitle("新对话");
                conversation.setUpdateId(conversation.getUserId());
                conversationMapper.updateById(conversation);
                logger.info("设置默认标题成功: {}", conversationId);
            }
        } catch (Exception e) {
            logger.error("设置默认标题失败: {}", conversationId, e);
        }
    }
}