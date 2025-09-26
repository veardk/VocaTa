package com.vocata.ai.llm.impl;

import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * 七牛云 AI LLM 提供者实现
 * 支持 x-ai/grok-4-fast 模型
 * API文档：https://developer.qiniu.com/aitokenapi/12884/how-to-get-api-key
 */
@Component("qiniuLlmProvider")
public class QiniuLlmProvider implements LlmProvider, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(QiniuLlmProvider.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${qiniu-ai.ai.api-key}")
    private String apiKey;

    @Value("${qiniu-ai.ai.base-url:https://openai.qiniu.com/v1}")
    private String baseUrl;

    @Value("${qiniu-ai.ai.default-model:x-ai/grok-4-fast}")
    private String defaultModel;

    @Value("${qiniu-ai.ai.timeout:60}")
    private int timeoutSeconds;

    private WebClient webClient;

    @Override
    public int getMaxContextLength() {
        // grok-4-fast 支持 128K tokens 上下文
        return 128000;
    }

    @Override
    public String[] getSupportedModels() {
        return new String[]{
            "x-ai/grok-4-fast",
            "x-ai/grok-4"
        };
    }

    @Override
    public int estimateTokens(String text) {
        // 简单估算：1个token大约3个字符（中英文混合）
        return text == null ? 0 : (text.length() / 3);
    }

    @Override
    public boolean validateModelConfig(UnifiedAiRequest.ModelConfig config) {
        if (config == null) return true;

        // 验证模型名称
        if (config.getModelName() != null) {
            boolean isValidModel = Arrays.asList(getSupportedModels()).contains(config.getModelName());
            if (!isValidModel) return false;
        }

        // 验证温度参数 (0.0 - 2.0)
        if (config.getTemperature() != null) {
            double temp = config.getTemperature();
            if (temp < 0.0 || temp > 2.0) return false;
        }

        // 验证最大token数
        if (config.getMaxTokens() != null) {
            int maxTokens = config.getMaxTokens();
            if (maxTokens < 1 || maxTokens > 4096) return false;
        }

        return true;
    }

    @Override
    public void afterPropertiesSet() {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        logger.info("七牛云 AI LLM Provider initialized with model: {}", defaultModel);
    }

    @Override
    public Flux<UnifiedAiStreamChunk> streamChat(UnifiedAiRequest request) {
        return Flux.defer(() -> {
            try {
                Map<String, Object> requestBody = buildQiniuRequest(request);
                String model = request.getModelConfig() != null && request.getModelConfig().getModelName() != null
                    ? request.getModelConfig().getModelName()
                    : defaultModel;

                logger.debug("发送七牛云AI请求，模型: {}", model);

                return webClient
                        .post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                        .map(line -> line.substring(6)) // 移除 "data: " 前缀
                        .filter(data -> !data.trim().isEmpty())
                        .flatMap(this::parseQiniuStreamChunk)
                        .collectList() // 收集所有chunk
                        .flatMapMany(chunks -> {
                            // 手动处理累积内容
                            StringBuilder accumulated = new StringBuilder();
                            List<UnifiedAiStreamChunk> updatedChunks = new ArrayList<>();

                            for (UnifiedAiStreamChunk chunk : chunks) {
                                if (chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                                    accumulated.append(chunk.getContent());
                                }
                                chunk.setAccumulatedContent(accumulated.toString());
                                updatedChunks.add(chunk);
                            }

                            logger.info("七牛云AI响应解析完成，生成{}个chunk，总内容长度: {}",
                                updatedChunks.size(), accumulated.length());

                            return Flux.fromIterable(updatedChunks);
                        })
                        .doOnError(error -> logger.error("七牛云AI API调用失败: {}", error.getMessage()))
                        .onErrorResume(error -> {
                            UnifiedAiStreamChunk errorChunk = new UnifiedAiStreamChunk();
                            errorChunk.setContent("抱歉，AI服务暂时不可用，请稍后再试。");
                            errorChunk.setAccumulatedContent("抱歉，AI服务暂时不可用，请稍后再试。");
                            errorChunk.setIsFinal(true);
                            return Flux.just(errorChunk);
                        });

            } catch (Exception e) {
                logger.error("构建七牛云AI请求失败", e);
                UnifiedAiStreamChunk errorChunk = new UnifiedAiStreamChunk();
                errorChunk.setContent("请求构建失败");
                errorChunk.setAccumulatedContent("请求构建失败");
                errorChunk.setIsFinal(true);
                return Flux.just(errorChunk);
            }
        });
    }

    private Map<String, Object> buildQiniuRequest(UnifiedAiRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // 设置模型 - 使用OpenAI兼容格式
        String model = request.getModelConfig() != null && request.getModelConfig().getModelName() != null
            ? request.getModelConfig().getModelName()
            : defaultModel;
        requestBody.put("model", model);

        // 构建消息列表 - OpenAI格式
        List<Map<String, Object>> messages = new ArrayList<>();

        // 添加系统消息（如果有）
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", request.getSystemPrompt());
            messages.add(systemMessage);
        }

        // 添加历史对话
        if (request.getContextMessages() != null) {
            for (UnifiedAiRequest.ChatMessage contextMsg : request.getContextMessages()) {
                Map<String, Object> message = new HashMap<>();
                message.put("role", contextMsg.getRole());
                message.put("content", contextMsg.getContent());
                messages.add(message);
            }
        }

        // 添加当前用户消息
        if (request.getUserMessage() != null && !request.getUserMessage().trim().isEmpty()) {
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getUserMessage());
            messages.add(userMessage);
        }

        requestBody.put("messages", messages);

        // 配置生成参数 - OpenAI兼容格式
        requestBody.put("stream", true); // 启用流式响应

        if (request.getModelConfig() != null) {
            if (request.getModelConfig().getTemperature() != null) {
                requestBody.put("temperature", request.getModelConfig().getTemperature());
            }
            if (request.getModelConfig().getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getModelConfig().getMaxTokens());
            }
        }

        // 默认参数
        requestBody.putIfAbsent("temperature", 0.7);
        requestBody.putIfAbsent("max_tokens", 2048);

        logger.debug("构建的七牛云AI请求: {}", requestBody);
        return requestBody;
    }

    private Flux<UnifiedAiStreamChunk> parseQiniuStreamChunk(String jsonData) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonData);

            // 检查是否有错误
            if (jsonNode.has("error")) {
                JsonNode errorNode = jsonNode.get("error");
                String errorMessage = errorNode.has("message") ? errorNode.get("message").asText() : "Unknown error";
                logger.error("七牛云AI API返回错误: {}", errorMessage);

                UnifiedAiStreamChunk errorChunk = new UnifiedAiStreamChunk();
                errorChunk.setContent("服务暂时不可用: " + errorMessage);
                errorChunk.setAccumulatedContent("服务暂时不可用: " + errorMessage);
                errorChunk.setIsFinal(true);
                return Flux.just(errorChunk);
            }

            // 解析OpenAI兼容格式响应
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode choice = choices.get(0);
                JsonNode delta = choice.get("delta");

                if (delta != null && delta.has("content")) {
                    String content = delta.get("content").asText();

                    UnifiedAiStreamChunk chunk = new UnifiedAiStreamChunk();
                    chunk.setContent(content);

                    // 检查是否完成
                    String finishReason = choice.has("finish_reason") && !choice.get("finish_reason").isNull()
                        ? choice.get("finish_reason").asText() : null;
                    boolean isFinished = "stop".equals(finishReason) || "length".equals(finishReason);
                    chunk.setIsFinal(isFinished);

                    logger.debug("解析七牛云AI响应: content={}, isFinished={}", content, isFinished);
                    return Flux.just(chunk);
                }
            }

            // 空响应或无有效内容
            return Flux.empty();

        } catch (Exception e) {
            logger.error("解析七牛云AI流式响应失败: {}", e.getMessage());
            logger.debug("失败的响应内容: {}", jsonData);
            return Flux.empty();
        }
    }

    @Override
    public String getProviderName() {
        return "Qiniu AI";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("your-qiniu-ai-api-key");
    }
}