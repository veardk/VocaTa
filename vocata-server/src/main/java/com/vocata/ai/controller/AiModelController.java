package com.vocata.ai.controller;

import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.LlmProvider;
import com.vocata.common.result.ApiResponse;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * AI模型调用控制器
 * 支持手动指定模型提供商和模型进行AI对话
 */
@RestController
@RequestMapping("/api/client/ai")
public class AiModelController {

    private static final Logger logger = LoggerFactory.getLogger(AiModelController.class);

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 获取所有可用的AI模型列表
     */
    @GetMapping("/models")
    public ApiResponse<Map<String, Object>> getAvailableModels() {
        Map<String, LlmProvider> providers = applicationContext.getBeansOfType(LlmProvider.class);

        List<Map<String, Object>> providerList = new ArrayList<>();

        for (Map.Entry<String, LlmProvider> entry : providers.entrySet()) {
            LlmProvider provider = entry.getValue();

            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("providerName", provider.getProviderName());
            providerInfo.put("beanName", entry.getKey());
            providerInfo.put("isAvailable", provider.isAvailable());
            providerInfo.put("maxContextLength", provider.getMaxContextLength());
            providerInfo.put("supportedModels", Arrays.asList(provider.getSupportedModels()));

            providerList.add(providerInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("providers", providerList);
        result.put("totalProviders", providerList.size());
        result.put("availableProviders", providerList.stream()
            .mapToInt(p -> (Boolean) p.get("isAvailable") ? 1 : 0)
            .sum());

        return ApiResponse.success(result);
    }

    /**
     * 使用指定模型进行AI对话
     */
    @PostMapping("/chat")
    public ApiResponse<String> chatWithModel(@RequestBody ChatRequest request) {
        logger.info("收到AI聊天请求，提供商: {}, 模型: {}", request.getProviderName(), request.getModelName());

        // 查找指定的LLM提供商
        LlmProvider provider = findProviderByName(request.getProviderName());
        if (provider == null) {
            throw new BizException(ApiCode.PARAM_ERROR, "未找到指定的AI提供商: " + request.getProviderName());
        }

        if (!provider.isAvailable()) {
            throw new BizException(ApiCode.AI_SERVICE_ERROR, "AI提供商不可用: " + request.getProviderName());
        }

        // 构建AI请求
        UnifiedAiRequest aiRequest = buildAiRequest(request);

        // 验证模型配置
        if (!provider.validateModelConfig(aiRequest.getModelConfig())) {
            throw new BizException(ApiCode.PARAM_ERROR, "模型配置无效，请检查模型名称和参数");
        }

        try {
            // 调用AI并收集完整响应
            String response = provider.streamChat(aiRequest)
                .map(UnifiedAiStreamChunk::getContent)
                .filter(Objects::nonNull)
                .reduce("", (accumulated, chunk) -> accumulated + chunk)
                .block();

            logger.info("AI响应生成完成，长度: {} 字符", response != null ? response.length() : 0);

            return ApiResponse.success(response);

        } catch (Exception e) {
            logger.error("AI调用失败", e);
            throw new BizException(ApiCode.AI_SERVICE_ERROR, "AI调用失败: " + e.getMessage());
        }
    }

    /**
     * 使用指定模型进行流式AI对话
     */
    @PostMapping("/stream-chat")
    public Flux<String> streamChatWithModel(@RequestBody ChatRequest request) {
        logger.info("收到AI流式聊天请求，提供商: {}, 模型: {}", request.getProviderName(), request.getModelName());

        // 查找指定的LLM提供商
        LlmProvider provider = findProviderByName(request.getProviderName());
        if (provider == null) {
            return Flux.error(new RuntimeException("未找到指定的AI提供商: " + request.getProviderName()));
        }

        if (!provider.isAvailable()) {
            return Flux.error(new RuntimeException("AI提供商不可用: " + request.getProviderName()));
        }

        // 构建AI请求
        UnifiedAiRequest aiRequest = buildAiRequest(request);

        // 验证模型配置
        if (!provider.validateModelConfig(aiRequest.getModelConfig())) {
            return Flux.error(new RuntimeException("模型配置无效，请检查模型名称和参数"));
        }

        try {
            return provider.streamChat(aiRequest)
                .map(UnifiedAiStreamChunk::getContent)
                .filter(Objects::nonNull)
                .doOnNext(chunk -> logger.debug("流式响应块: {}", chunk))
                .doOnComplete(() -> logger.info("流式AI响应完成"))
                .doOnError(error -> logger.error("流式AI调用失败", error));

        } catch (Exception e) {
            logger.error("流式AI调用初始化失败", e);
            return Flux.error(new RuntimeException("AI调用失败: " + e.getMessage()));
        }
    }

    /**
     * 根据提供商名称查找LLM提供商
     */
    private LlmProvider findProviderByName(String providerName) {
        Map<String, LlmProvider> providers = applicationContext.getBeansOfType(LlmProvider.class);

        // 优先精确匹配bean名称
        if (providers.containsKey(providerName)) {
            return providers.get(providerName);
        }

        // 然后匹配提供商显示名称
        for (LlmProvider provider : providers.values()) {
            if (provider.getProviderName().toLowerCase().contains(providerName.toLowerCase())) {
                return provider;
            }
        }

        // 最后尝试部分匹配
        for (Map.Entry<String, LlmProvider> entry : providers.entrySet()) {
            String beanName = entry.getKey().toLowerCase();
            String searchName = providerName.toLowerCase();

            if (beanName.contains(searchName) || searchName.contains(beanName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 构建AI请求对象
     */
    private UnifiedAiRequest buildAiRequest(ChatRequest request) {
        UnifiedAiRequest aiRequest = new UnifiedAiRequest();

        // 设置系统提示词和用户消息
        aiRequest.setSystemPrompt(request.getSystemPrompt());
        aiRequest.setUserMessage(request.getUserMessage());

        // 设置历史对话（如果有）
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            List<UnifiedAiRequest.ChatMessage> contextMessages = new ArrayList<>();
            for (ChatMessage msg : request.getMessages()) {
                contextMessages.add(new UnifiedAiRequest.ChatMessage(msg.getRole(), msg.getContent()));
            }
            aiRequest.setContextMessages(contextMessages);
        }

        // 设置模型配置
        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName(request.getModelName());
        modelConfig.setTemperature(request.getTemperature());
        modelConfig.setMaxTokens(request.getMaxTokens());
        modelConfig.setTopP(request.getTopP());

        aiRequest.setModelConfig(modelConfig);

        return aiRequest;
    }

    /**
     * 聊天请求DTO
     */
    public static class ChatRequest {
        private String providerName;    // 提供商名称（如 "siliconFlowLlmProvider" 或 "SiliconFlow AI"）
        private String modelName;       // 模型名称（如 "anthropic/claude-3-5-sonnet-20241022"）
        private String systemPrompt;    // 系统提示词
        private String userMessage;     // 用户消息
        private List<ChatMessage> messages; // 历史对话消息
        private Double temperature;     // 温度参数 0.0-2.0
        private Integer maxTokens;      // 最大token数
        private Double topP;           // top_p参数 0.0-1.0

        // Getters and Setters
        public String getProviderName() { return providerName; }
        public void setProviderName(String providerName) { this.providerName = providerName; }

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

        public List<ChatMessage> getMessages() { return messages; }
        public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }

        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

        public Double getTopP() { return topP; }
        public void setTopP(Double topP) { this.topP = topP; }
    }

    /**
     * 聊天消息DTO
     */
    public static class ChatMessage {
        private String role;    // "user", "assistant", "system"
        private String content; // 消息内容

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}