package com.vocata.ai.llm.impl;

import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.LlmProvider;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OpenAI LLM提供商实现类
 * 使用WebClient实现异步、非阻塞的API调用
 */
@Service
public class OpenAiLlmProvider implements LlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiLlmProvider.class);

    private final WebClient webClient;

    @Value("${openai.api.key:#{null}}")
    private String apiKey;

    @Value("${openai.api.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${openai.api.timeout:60}")
    private int timeoutSeconds;

    @Value("${openai.api.default-model:gpt-3.5-turbo}")
    private String defaultModel;

    // 支持的模型列表
    private static final String[] SUPPORTED_MODELS = {
        "gpt-3.5-turbo", "gpt-3.5-turbo-16k",
        "gpt-4", "gpt-4-turbo", "gpt-4o",
        "gpt-4-32k"
    };

    // 模型上下文长度映射
    private static final Map<String, Integer> MODEL_CONTEXT_LENGTHS = Map.of(
        "gpt-3.5-turbo", 4096,
        "gpt-3.5-turbo-16k", 16384,
        "gpt-4", 8192,
        "gpt-4-turbo", 128000,
        "gpt-4o", 128000,
        "gpt-4-32k", 32768
    );

    public OpenAiLlmProvider() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public Flux<UnifiedAiStreamChunk> streamChat(UnifiedAiRequest request) {
        if (!isAvailable()) {
            return Flux.error(new BizException(ApiCode.AI_SERVICE_UNAVAILABLE, "OpenAI API Key未配置"));
        }

        logger.info("开始OpenAI流式聊天，模型: {}", request.getModelConfig().getModelName());

        // 构建OpenAI API请求
        Map<String, Object> openAiRequest = buildOpenAiRequest(request);

        // 性能监控
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        AtomicInteger chunkIndex = new AtomicInteger(0);
        AtomicReference<String> accumulatedContent = new AtomicReference<>("");
        AtomicLong firstTokenTime = new AtomicLong(0);

        return webClient.post()
                .uri(baseUrl + "/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(openAiRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .onErrorMap(WebClientResponseException.class, ex -> {
                    logger.error("OpenAI API调用失败: {}", ex.getResponseBodyAsString());
                    return new BizException(ApiCode.AI_SERVICE_ERROR,
                        "AI服务调用失败: " + ex.getMessage());
                })
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6)) // 移除 "data: " 前缀
                .filter(data -> !data.trim().isEmpty() && !"[DONE]".equals(data.trim()))
                .flatMap(this::parseOpenAiResponse)
                .map(chunk -> {
                    // 更新累积内容
                    if (chunk.getContent() != null) {
                        String newAccumulated = accumulatedContent.get() + chunk.getContent();
                        accumulatedContent.set(newAccumulated);
                        chunk.setAccumulatedContent(newAccumulated);
                    }

                    // 记录首个token时间
                    if (firstTokenTime.get() == 0 && chunk.getContent() != null) {
                        firstTokenTime.set(System.currentTimeMillis());
                    }

                    // 设置性能指标
                    chunk.setChunkIndex(chunkIndex.getAndIncrement());
                    setPerformanceMetrics(chunk, startTime.get(), firstTokenTime.get());

                    return chunk;
                })
                .concatWith(Mono.fromCallable(() -> {
                    // 创建最终完成块
                    UnifiedAiStreamChunk finalChunk = new UnifiedAiStreamChunk();
                    finalChunk.setType(UnifiedAiStreamChunk.ChunkType.DONE);
                    finalChunk.setIsFinal(true);
                    finalChunk.setAccumulatedContent(accumulatedContent.get());
                    finalChunk.setChunkIndex(chunkIndex.get());
                    finalChunk.setFinishReason("stop");

                    // 设置最终的性能指标和Token统计
                    setFinalMetrics(finalChunk, startTime.get(), firstTokenTime.get(),
                                  accumulatedContent.get());

                    return finalChunk;
                }));
    }

    @Override
    public int getMaxContextLength() {
        return MODEL_CONTEXT_LENGTHS.getOrDefault(defaultModel, 4096);
    }

    @Override
    public String[] getSupportedModels() {
        return SUPPORTED_MODELS.clone();
    }

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：约4字符=1token（对于英文）
        return (int) Math.ceil(text.length() / 4.0);
    }

    @Override
    public boolean validateModelConfig(UnifiedAiRequest.ModelConfig config) {
        if (config == null) {
            return false;
        }

        String modelName = config.getModelName();
        if (modelName == null || !Arrays.asList(SUPPORTED_MODELS).contains(modelName)) {
            return false;
        }

        Double temperature = config.getTemperature();
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            return false;
        }

        Integer maxTokens = config.getMaxTokens();
        if (maxTokens != null && maxTokens <= 0) {
            return false;
        }

        return true;
    }

    /**
     * 构建OpenAI API请求
     */
    private Map<String, Object> buildOpenAiRequest(UnifiedAiRequest request) {
        Map<String, Object> openAiRequest = new HashMap<>();

        // 模型配置
        UnifiedAiRequest.ModelConfig config = request.getModelConfig();
        openAiRequest.put("model", config.getModelName() != null ?
            config.getModelName() : defaultModel);

        if (config.getTemperature() != null) {
            openAiRequest.put("temperature", config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            openAiRequest.put("max_tokens", config.getMaxTokens());
        }
        if (config.getTopP() != null) {
            openAiRequest.put("top_p", config.getTopP());
        }

        // 消息构建
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统提示词
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }

        // 上下文消息
        if (request.getContextMessages() != null) {
            for (UnifiedAiRequest.ChatMessage msg : request.getContextMessages()) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }

        // 用户消息
        if (request.getUserMessage() != null && !request.getUserMessage().isEmpty()) {
            messages.add(Map.of("role", "user", "content", request.getUserMessage()));
        }

        openAiRequest.put("messages", messages);
        openAiRequest.put("stream", true); // 启用流式响应

        return openAiRequest;
    }

    /**
     * 解析OpenAI响应
     */
    private Mono<UnifiedAiStreamChunk> parseOpenAiResponse(String jsonData) {
        try {
            // 这里应该使用JSON解析库，简化处理
            // 在实际项目中应该使用Jackson或Gson
            UnifiedAiStreamChunk chunk = new UnifiedAiStreamChunk();
            chunk.setType(UnifiedAiStreamChunk.ChunkType.CONTENT);

            // 简化的JSON解析（实际应该使用Jackson）
            if (jsonData.contains("\"content\"")) {
                // 提取content字段的值
                String content = extractJsonField(jsonData, "content");
                chunk.setContent(content);
            }

            if (jsonData.contains("\"finish_reason\"")) {
                String finishReason = extractJsonField(jsonData, "finish_reason");
                chunk.setFinishReason(finishReason);
                chunk.setIsFinal(!"null".equals(finishReason) && finishReason != null);
            }

            return Mono.just(chunk);
        } catch (Exception e) {
            logger.error("解析OpenAI响应失败: {}", e.getMessage());
            UnifiedAiStreamChunk errorChunk = new UnifiedAiStreamChunk();
            errorChunk.setType(UnifiedAiStreamChunk.ChunkType.ERROR);
            errorChunk.setContent("响应解析错误: " + e.getMessage());
            return Mono.just(errorChunk);
        }
    }

    /**
     * 简化的JSON字段提取（应该使用Jackson替代）
     */
    private String extractJsonField(String json, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\":\"";
            int startIndex = json.indexOf(pattern);
            if (startIndex == -1) {
                return null;
            }
            startIndex += pattern.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) {
                return null;
            }
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置性能指标
     */
    private void setPerformanceMetrics(UnifiedAiStreamChunk chunk, long startTime, long firstTokenTime) {
        long currentTime = System.currentTimeMillis();

        UnifiedAiStreamChunk.PerformanceMetrics metrics =
            new UnifiedAiStreamChunk.PerformanceMetrics();

        metrics.setLatencyMs(currentTime - startTime);

        if (firstTokenTime > 0) {
            metrics.setFirstTokenLatencyMs(firstTokenTime - startTime);
        }

        chunk.setPerformance(metrics);
    }

    /**
     * 设置最终指标
     */
    private void setFinalMetrics(UnifiedAiStreamChunk chunk, long startTime, long firstTokenTime,
                               String fullContent) {
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        UnifiedAiStreamChunk.PerformanceMetrics metrics =
            new UnifiedAiStreamChunk.PerformanceMetrics();

        metrics.setLatencyMs(totalTime);

        if (firstTokenTime > 0) {
            metrics.setFirstTokenLatencyMs(firstTokenTime - startTime);
        }

        // 计算生成速度
        int outputTokens = estimateTokens(fullContent);
        if (outputTokens > 0 && totalTime > 0) {
            metrics.setTokensPerSecond((double) outputTokens / (totalTime / 1000.0));
        }

        chunk.setPerformance(metrics);

        // 设置Token统计
        UnifiedAiStreamChunk.TokenUsage tokenUsage =
            new UnifiedAiStreamChunk.TokenUsage();
        tokenUsage.setOutputTokens(outputTokens);
        // 输入tokens需要根据请求计算，这里暂时省略

        chunk.setTokenUsage(tokenUsage);
    }
}