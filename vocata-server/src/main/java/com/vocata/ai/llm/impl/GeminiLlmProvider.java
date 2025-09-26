package com.vocata.ai.llm.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Google Gemini LLM 提供者实现
 */
@Component("geminiLlmProvider")
public class GeminiLlmProvider implements LlmProvider, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(GeminiLlmProvider.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.api.default-model:gemini-1.5-flash}")
    private String defaultModel;

    @Value("${gemini.api.timeout:60}")
    private int timeoutSeconds;

    private WebClient webClient;

    @Override
    public int getMaxContextLength() {
        // Gemini 1.5 Flash支持最大2M token上下文
        return 2000000;
    }

    @Override
    public String[] getSupportedModels() {
        return new String[]{
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-1.0-pro"
        };
    }

    @Override
    public int estimateTokens(String text) {
        // 简单估算：1个token大约4个字符
        return text == null ? 0 : (text.length() / 4);
    }

    @Override
    public boolean validateModelConfig(UnifiedAiRequest.ModelConfig config) {
        if (config == null) return true;

        // 验证模型名称
        if (config.getModelName() != null) {
            boolean isValidModel = Arrays.asList(getSupportedModels()).contains(config.getModelName());
            if (!isValidModel) return false;
        }

        // 验证温度参数
        if (config.getTemperature() != null) {
            double temp = config.getTemperature();
            if (temp < 0.0 || temp > 2.0) return false;
        }

        // 验证最大token数
        if (config.getMaxTokens() != null) {
            int maxTokens = config.getMaxTokens();
            if (maxTokens < 1 || maxTokens > 8192) return false;
        }

        return true;
    }

    @Override
    public void afterPropertiesSet() {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        logger.info("Gemini LLM Provider initialized with model: {}", defaultModel);
    }

    @Override
    public Flux<UnifiedAiStreamChunk> streamChat(UnifiedAiRequest request) {
        return Flux.defer(() -> {
            try {
                Map<String, Object> requestBody = buildGeminiRequest(request);
                String model = request.getModelConfig() != null && request.getModelConfig().getModelName() != null
                    ? request.getModelConfig().getModelName()
                    : defaultModel;

                logger.debug("发送Gemini请求，模型: {}", model);

                return webClient
                        .post()
                        .uri("/v1beta/models/{model}:streamGenerateContent?key={apiKey}", model, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .window(Duration.ofMillis(100))  // 每100ms创建一个窗口
                        .flatMap(window -> window.collectList())  // 收集窗口内的数据
                        .filter(lines -> !lines.isEmpty())
                        .map(lines -> String.join("", lines))  // 合并为完整JSON
                        .flatMapIterable(this::parseGeminiStreamResponse)
                        .doOnError(error -> logger.error("Gemini API调用失败: {}", error.getMessage()))
                        .onErrorResume(error -> {
                            UnifiedAiStreamChunk errorChunk = new UnifiedAiStreamChunk();
                            errorChunk.setContent("抱歉，AI服务暂时不可用，请稍后再试。");
                            errorChunk.setAccumulatedContent("抱歉，AI服务暂时不可用，请稍后再试。");
                            errorChunk.setIsFinal(true);
                            return Flux.just(errorChunk);
                        });

            } catch (Exception e) {
                logger.error("构建Gemini请求失败", e);
                UnifiedAiStreamChunk errorChunk = new UnifiedAiStreamChunk();
                errorChunk.setContent("请求构建失败");
                errorChunk.setAccumulatedContent("请求构建失败");
                errorChunk.setIsFinal(true);
                return Flux.just(errorChunk);
            }
        });
    }

    private Map<String, Object> buildGeminiRequest(UnifiedAiRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // 构建消息列表
        List<Map<String, Object>> contents = new ArrayList<>();

        // 添加系统消息（如果有）
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
            Map<String, Object> systemContent = new HashMap<>();
            systemContent.put("role", "user");

            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", "System: " + request.getSystemPrompt());
            parts.add(part);

            systemContent.put("parts", parts);
            contents.add(systemContent);
        }

        // 添加历史对话
        if (request.getContextMessages() != null) {
            for (UnifiedAiRequest.ChatMessage contextMsg : request.getContextMessages()) {
                Map<String, Object> content = new HashMap<>();

                // Gemini角色映射
                String role = "user".equals(contextMsg.getRole()) ? "user" : "model";
                content.put("role", role);

                List<Map<String, Object>> parts = new ArrayList<>();
                Map<String, Object> part = new HashMap<>();
                part.put("text", contextMsg.getContent());
                parts.add(part);

                content.put("parts", parts);
                contents.add(content);
            }
        }

        // 添加当前用户消息
        if (request.getUserMessage() != null && !request.getUserMessage().trim().isEmpty()) {
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");

            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", request.getUserMessage());
            parts.add(part);

            userContent.put("parts", parts);
            contents.add(userContent);
        }

        requestBody.put("contents", contents);

        // 配置生成参数
        Map<String, Object> generationConfig = new HashMap<>();
        if (request.getModelConfig() != null) {
            if (request.getModelConfig().getTemperature() != null) {
                generationConfig.put("temperature", request.getModelConfig().getTemperature());
            }
            if (request.getModelConfig().getMaxTokens() != null) {
                generationConfig.put("maxOutputTokens", request.getModelConfig().getMaxTokens());
            }
        }

        // 默认参数
        generationConfig.putIfAbsent("temperature", 0.7);
        generationConfig.putIfAbsent("maxOutputTokens", 2048);

        requestBody.put("generationConfig", generationConfig);

        logger.debug("构建的Gemini请求: {}", requestBody);
        return requestBody;
    }

    private List<UnifiedAiStreamChunk> parseGeminiStreamResponse(String completeJson) {
        try {
            List<UnifiedAiStreamChunk> chunks = new ArrayList<>();

            // Gemini API 返回数组格式的多个响应块
            if (completeJson.startsWith("[")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> responseArray = objectMapper.readValue(completeJson, List.class);

                for (Map<String, Object> responseObj : responseArray) {
                    UnifiedAiStreamChunk chunk = parseGeminiSingleResponse(responseObj);
                    if (chunk != null) {
                        chunks.add(chunk);
                    }
                }
            } else {
                // 单个对象格式
                @SuppressWarnings("unchecked")
                Map<String, Object> responseObj = objectMapper.readValue(completeJson, Map.class);
                UnifiedAiStreamChunk chunk = parseGeminiSingleResponse(responseObj);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }

            return chunks;

        } catch (Exception e) {
            logger.error("解析Gemini流式响应失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private UnifiedAiStreamChunk parseGeminiSingleResponse(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> candidate = candidates.get(0);

            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");

            if (content == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            if (parts == null || parts.isEmpty()) {
                return null;
            }

            String text = (String) parts.get(0).get("text");
            if (text == null) {
                return null;
            }

            UnifiedAiStreamChunk chunk = new UnifiedAiStreamChunk();
            chunk.setContent(text);

            // 检查是否完成
            String finishReason = (String) candidate.get("finishReason");
            boolean isFinished = "STOP".equals(finishReason) || "MAX_TOKENS".equals(finishReason);
            chunk.setIsFinal(isFinished);

            // 注意：Gemini不提供accumulated内容，我们需要在上层处理
            // 暂时设置为当前内容
            chunk.setAccumulatedContent(text);

            logger.debug("解析Gemini响应成功: content={}, isFinished={}", text, isFinished);
            return chunk;

        } catch (Exception e) {
            logger.error("解析单个Gemini响应失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getProviderName() {
        return "Gemini";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("your-gemini-api-key");
    }
}