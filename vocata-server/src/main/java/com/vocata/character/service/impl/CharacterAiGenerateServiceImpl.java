package com.vocata.character.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.impl.SiliconFlowLlmProvider;
import com.vocata.character.dto.request.CharacterAiGenerateRequest;
import com.vocata.character.dto.response.CharacterAiGenerateResponse;
import com.vocata.character.service.CharacterAiGenerateService;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * AI角色生成服务实现类
 */
@Service
public class CharacterAiGenerateServiceImpl implements CharacterAiGenerateService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterAiGenerateServiceImpl.class);

    @Autowired
    private SiliconFlowLlmProvider siliconFlowLlmProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${siliconflow.ai.default-model:Qwen/Qwen3-8B}")
    private String defaultModel;

    /**
     * AI角色生成提示词模板（结构化JSON输出）
     */
    private static final String CHARACTER_GENERATION_PROMPT = """
            你是一个专业的角色设计师，需要根据基本信息生成详细的角色设定。请严格按照JSON格式输出，不要添加任何格式说明或其他文字。

            角色基本信息：
            - 名称：{name}
            - 描述：{description}
            - 问候语：{greeting}

            请输出以下JSON格式，参考李白示例的风格：

            {
              "personalityTraits": ["特征1", "特征2"],
              "speakingStyle": "详细的说话风格描述，包括用词习惯、语气特点、称呼方式等",
              "exampleDialogues": [
                {"user": "用户问题1", "assistant": "角色回答1"},
                {"user": "用户问题2", "assistant": "角色回答2"},
                {"user": "用户问题3", "assistant": "角色回答3"},
                {"user": "用户问题4", "assistant": "角色回答4"},
                {"user": "用户问题5", "assistant": "角色回答5"}
              ],
              "tags": ["标签1", "标签2"],
              "searchKeywords": "用空格分隔的搜索关键词"
            }

            要求：
            1. personalityTraits：选择最突出的2个性格特征
            2. speakingStyle：描述角色的说话风格和语言特点，80-150字
            3. exampleDialogues：5个高质量的对话示例，体现角色特色
            4. tags：2个最相关的分类标签
            5. searchKeywords：提升搜索的关键词，用空格分隔

            只输出JSON，不要任何其他内容。
            """;

    @Override
    public CharacterAiGenerateResponse generateCharacter(CharacterAiGenerateRequest request) {
        logger.info("开始AI角色生成，角色名称: {}", request.getName());

        long startTime = System.currentTimeMillis();

        try {
            // 检查硅基流动AI是否可用
            if (!siliconFlowLlmProvider.isAvailable()) {
                throw new BizException(ApiCode.AI_SERVICE_ERROR, "硅基流动AI服务不可用");
            }

            // 构建AI请求
            UnifiedAiRequest aiRequest = buildAiRequest(request);

            // 验证模型配置
            if (!siliconFlowLlmProvider.validateModelConfig(aiRequest.getModelConfig())) {
                throw new BizException(ApiCode.PARAM_ERROR, "AI模型配置无效");
            }

            // 调用AI生成内容（增强内容清洗）
            String generatedContent = siliconFlowLlmProvider.streamChat(aiRequest)
                    .map(UnifiedAiStreamChunk::getContent)
                    .filter(Objects::nonNull)
                    .filter(content -> !content.trim().isEmpty())
                    .filter(content -> !"null".equals(content))  // 过滤字符串"null"
                    .reduce("", (accumulated, chunk) -> accumulated + chunk)
                    .map(this::cleanGeneratedContent)  // 进一步清洗内容
                    .block();

            if (generatedContent == null || generatedContent.trim().isEmpty()) {
                throw new BizException(ApiCode.AI_SERVICE_ERROR, "AI生成内容为空");
            }

            long endTime = System.currentTimeMillis();
            long generationTime = endTime - startTime;

            logger.info("AI角色生成完成，耗时: {}ms，内容长度: {}", generationTime, generatedContent.length());

            // 构建响应
            CharacterAiGenerateResponse response = new CharacterAiGenerateResponse();
            response.setName(request.getName());
            response.setDescription(request.getDescription());
            response.setGreeting(request.getGreeting());
            response.setGeneratedContent(generatedContent);
            response.setPersona(extractPersona(generatedContent));
            response.setGenerationTime(generationTime);
            response.setModelUsed(defaultModel);

            // 解析JSON内容并设置具体字段
            parseAndSetFields(response, generatedContent);

            return response;

        } catch (BizException e) {
            logger.error("AI角色生成业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("AI角色生成失败", e);
            throw new BizException(ApiCode.AI_SERVICE_ERROR, "AI角色生成失败: " + e.getMessage());
        }
    }

    /**
     * 构建AI请求对象
     */
    private UnifiedAiRequest buildAiRequest(CharacterAiGenerateRequest request) {
        UnifiedAiRequest aiRequest = new UnifiedAiRequest();

        // 构建用户消息，将模板中的占位符替换为实际值
        String userMessage = CHARACTER_GENERATION_PROMPT
                .replace("{name}", request.getName())
                .replace("{description}", request.getDescription())
                .replace("{greeting}", request.getGreeting());

        aiRequest.setUserMessage(userMessage);

        // 设置模型配置（优化速度）
        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName(defaultModel);
        modelConfig.setTemperature(0.7); // 降低温度提高一致性
        modelConfig.setMaxTokens(3000); // 减少token数量提高速度
        modelConfig.setTopP(0.8); // 适度降低采样范围

        aiRequest.setModelConfig(modelConfig);

        return aiRequest;
    }

    /**
     * 清洗生成的内容，移除无效字符和格式化
     */
    private String cleanGeneratedContent(String content) {
        if (content == null) {
            return "";
        }

        return content
                // 移除连续的null字符串
                .replaceAll("(?i)null+", "")
                // 移除多余的空白字符
                .replaceAll("\\s{3,}", " ")
                // 移除开头和结尾的空白
                .trim()
                // 确保句子之间有适当的间隔
                .replaceAll("([。！？])([^\\s])", "$1 $2")
                // 移除重复的换行符
                .replaceAll("\n{3,}", "\n\n");
    }

    /**
     * 解析AI生成的JSON内容并设置响应字段
     */
    private void parseAndSetFields(CharacterAiGenerateResponse response, String generatedContent) {
        try {
            // 尝试从生成内容中提取JSON
            String jsonContent = extractJsonFromContent(generatedContent);
            if (jsonContent == null) {
                logger.warn("无法从生成内容中提取JSON，使用默认值");
                setDefaultValues(response);
                return;
            }

            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            // 解析personalityTraits
            if (jsonNode.has("personalityTraits") && jsonNode.get("personalityTraits").isArray()) {
                List<String> traits = new ArrayList<>();
                jsonNode.get("personalityTraits").forEach(node -> traits.add(node.asText()));
                response.setPersonalityTraits(traits);
            }

            // 解析speakingStyle
            if (jsonNode.has("speakingStyle")) {
                response.setSpeakingStyle(jsonNode.get("speakingStyle").asText());
            }

            // 解析exampleDialogues
            if (jsonNode.has("exampleDialogues") && jsonNode.get("exampleDialogues").isArray()) {
                List<CharacterAiGenerateResponse.DialogueExample> dialogues = new ArrayList<>();
                jsonNode.get("exampleDialogues").forEach(dialogueNode -> {
                    if (dialogueNode.has("user") && dialogueNode.has("assistant")) {
                        dialogues.add(new CharacterAiGenerateResponse.DialogueExample(
                                dialogueNode.get("user").asText(),
                                dialogueNode.get("assistant").asText()
                        ));
                    }
                });
                response.setExampleDialogues(dialogues);
            }

            // 解析tags
            if (jsonNode.has("tags") && jsonNode.get("tags").isArray()) {
                List<String> tags = new ArrayList<>();
                jsonNode.get("tags").forEach(node -> tags.add(node.asText()));
                response.setTags(tags);
            }

            // 解析searchKeywords
            if (jsonNode.has("searchKeywords")) {
                response.setSearchKeywords(jsonNode.get("searchKeywords").asText());
            }

            logger.info("JSON解析成功，设置了字段: personalityTraits={}, tags={}",
                    response.getPersonalityTraits(), response.getTags());

        } catch (JsonProcessingException e) {
            logger.error("解析AI生成的JSON内容失败", e);
            setDefaultValues(response);
        }
    }

    /**
     * 从生成内容中提取JSON字符串
     */
    private String extractJsonFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 尝试找到JSON的开始和结束位置
        int startIndex = content.indexOf("{");
        int endIndex = content.lastIndexOf("}");

        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return content.substring(startIndex, endIndex + 1);
        }

        // 如果没有找到完整的JSON，尝试整个内容
        String trimmed = content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        return null;
    }

    /**
     * 设置默认值（当JSON解析失败时）
     */
    private void setDefaultValues(CharacterAiGenerateResponse response) {
        // 设置默认的性格特征
        response.setPersonalityTraits(Arrays.asList("智慧", "友善"));

        // 设置默认的说话风格
        response.setSpeakingStyle("温和友善，言辞得体，乐于助人。说话时会考虑对方的感受，用词恰当。");

        // 设置默认的对话示例
        List<CharacterAiGenerateResponse.DialogueExample> defaultDialogues = Arrays.asList(
                new CharacterAiGenerateResponse.DialogueExample("你好！", "你好！很高兴认识你！"),
                new CharacterAiGenerateResponse.DialogueExample("你能帮助我吗？", "当然可以！我很乐意帮助你。请告诉我你需要什么帮助。"),
                new CharacterAiGenerateResponse.DialogueExample("今天天气怎么样？", "今天是个美好的一天！无论天气如何，重要的是保持好心情。")
        );
        response.setExampleDialogues(defaultDialogues);

        // 设置默认标签
        response.setTags(Arrays.asList("友善", "智能"));

        // 设置默认搜索关键词
        response.setSearchKeywords(response.getName() + " 角色 智能助手 友善");
    }

    /**
     * 从生成的内容中提取人设prompt
     */
    private String extractPersona(String generatedContent) {
        // 先清洗内容
        String cleanedContent = cleanGeneratedContent(generatedContent);
        // 简化版本：直接返回清洗后的完整内容作为persona
        return cleanedContent;
    }
}