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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * 硅基流动 AI LLM 提供者实现
 * 支持多种主流AI模型，包括 Claude、GPT、Llama 等
 */
@Component("siliconFlowLlmProvider")
public class SiliconFlowLlmProvider implements LlmProvider, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SiliconFlowLlmProvider.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${siliconflow.ai.api-key}")
    private String apiKey;

    @Value("${siliconflow.ai.base-url:https://api.siliconflow.cn/v1}")
    private String baseUrl;

    @Value("${siliconflow.ai.default-model:Qwen/Qwen3-8B}")
    private String defaultModel;

    @Value("${siliconflow.ai.timeout:120}")
    private int timeoutSeconds;

    private WebClient webClient;

    @Override
    public String getProviderName() {
        return "SiliconFlow AI";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("your-siliconflow-api-key");
    }

    @Override
    public int getMaxContextLength() {
        // 根据不同模型返回不同的上下文长度
        // 这里返回一个通用的较大值，具体模型可以在后续优化
        return 128000;
    }

    @Override
    public String[] getSupportedModels() {
        return new String[]{
            // Claude 系列
            "anthropic/claude-3-5-sonnet-20241022",
            "anthropic/claude-3-5-haiku-20241022",
            "anthropic/claude-3-haiku-20240307",

            // GPT 系列
            "openai/gpt-4o",
            "openai/gpt-4o-mini",
            "openai/gpt-4-turbo",
            "openai/gpt-3.5-turbo",

            // DeepSeek 系列
            "deepseek-ai/DeepSeek-V2.5",
            "deepseek-ai/deepseek-llm-67b-chat",
            "deepseek-ai/deepseek-coder-33b-instruct",
            "deepseek-ai/DeepSeek-V3",
            "deepseek-ai/DeepSeek-R1",

            // Qwen 系列 (免费)
            "Qwen/Qwen3-8B",             // 免费模型 - 最新
            "Qwen/Qwen2.5-7B-Instruct",  // 免费模型
            "Qwen/Qwen2.5-14B-Instruct", // 免费模型
            "Qwen/Qwen2.5-32B-Instruct",
            "Qwen/Qwen2.5-72B-Instruct",
            "Qwen/Qwen2-VL-72B-Instruct",

            // Llama 系列
            "meta-llama/Meta-Llama-3.1-405B-Instruct",
            "meta-llama/Meta-Llama-3.1-70B-Instruct",
            "meta-llama/Meta-Llama-3.1-8B-Instruct",
            "meta-llama/Llama-3.2-90B-Vision-Instruct",
            "meta-llama/Llama-3.2-11B-Vision-Instruct",

            // Yi 系列
            "01-ai/Yi-1.5-34B-Chat-16K",
            "01-ai/Yi-1.5-9B-Chat-16K",

            // 其他热门模型
            "google/gemma-2-27b-it",
            "google/gemma-2-9b-it",
            "mistralai/Mistral-7B-Instruct-v0.3",
            "microsoft/Phi-3.5-mini-instruct"
        };
    }

    @Override
    public int estimateTokens(String text) {
        // 中英文混合文本的token估算
        return text == null ? 0 : (text.length() / 3);
    }

    @Override
    public boolean validateModelConfig(UnifiedAiRequest.ModelConfig config) {
        if (config == null) return true;

        // 验证模型名称
        if (config.getModelName() != null) {
            boolean isValidModel = Arrays.asList(getSupportedModels()).contains(config.getModelName());
            if (!isValidModel) {
                logger.warn("不支持的模型: {}", config.getModelName());
                return false;
            }
        }

        // 验证温度参数 (0.0 - 2.0)
        if (config.getTemperature() != null) {
            double temp = config.getTemperature();
            if (temp < 0.0 || temp > 2.0) {
                logger.warn("温度参数超出范围: {}", temp);
                return false;
            }
        }

        // 验证最大token数
        if (config.getMaxTokens() != null) {
            int maxTokens = config.getMaxTokens();
            if (maxTokens < 1 || maxTokens > 32768) {
                logger.warn("最大token数超出范围: {}", maxTokens);
                return false;
            }
        }

        // 验证top_p参数
        if (config.getTopP() != null) {
            double topP = config.getTopP();
            if (topP < 0.0 || topP > 1.0) {
                logger.warn("top_p参数超出范围: {}", topP);
                return false;
            }
        }

        return true;
    }

    @Override
    public void afterPropertiesSet() {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();

        logger.info("硅基流动 AI LLM Provider 初始化完成，默认模型: {}", defaultModel);
    }

    @Override
    public Flux<UnifiedAiStreamChunk> streamChat(UnifiedAiRequest request) {
        return Flux.defer(() -> {
            try {
                Map<String, Object> requestBody = buildSiliconFlowRequest(request);
                String model = request.getModelConfig() != null && request.getModelConfig().getModelName() != null
                    ? request.getModelConfig().getModelName()
                    : defaultModel;

                logger.debug("发送硅基流动AI请求，模型: {}", model);

                return webClient
                        .post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "text/event-stream")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToFlux(DataBuffer.class)
                        .map(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            return new String(bytes, StandardCharsets.UTF_8);
                        })
                        .buffer(Duration.ofMillis(50)) // 减少缓冲时间，加快响应
                        .flatMap(lines -> {
                            String combined = String.join("", lines);
                            return Flux.fromArray(combined.split("\n"))
                                    .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
                                    .map(line -> line.substring(6).trim())
                                    .filter(data -> !data.isEmpty());
                        })
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .flatMap(this::parseSiliconFlowStreamChunk)
                        .filter(chunk -> chunk.getContent() != null)  // 过滤null内容
                        .filter(chunk -> !chunk.getContent().trim().isEmpty())  // 过滤空内容
                        .filter(chunk -> !"null".equals(chunk.getContent()))  // 过滤字符串"null"
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

                            logger.info("硅基流动AI响应解析完成，生成{}个chunk，总内容长度: {}",
                                updatedChunks.size(), accumulated.length());

                            return Flux.fromIterable(updatedChunks);
                        })
                        .doOnError(error -> logger.error("硅基流动AI API调用失败: {}", error.getMessage()))
                        .onErrorResume(error -> {
                            UnifiedAiStreamChunk errorChunk = new UnifiedAiStreamChunk();
                            errorChunk.setContent("抱歉，AI服务暂时不可用，请稍后再试。");
                            errorChunk.setAccumulatedContent("抱歉，AI服务暂时不可用，请稍后再试。");
                            errorChunk.setIsFinal(true);
                            return Flux.just(errorChunk);
                        });

            } catch (Exception e) {
                logger.error("构建硅基流动AI请求失败", e);
                UnifiedAiStreamChunk errorChunk = new UnifiedAiStreamChunk();
                errorChunk.setContent("请求构建失败");
                errorChunk.setAccumulatedContent("请求构建失败");
                errorChunk.setIsFinal(true);
                return Flux.just(errorChunk);
            }
        });
    }

    private Map<String, Object> buildSiliconFlowRequest(UnifiedAiRequest request) {
        Map<String, Object> requestBody = new HashMap<>();

        // 设置模型
        String model = request.getModelConfig() != null && request.getModelConfig().getModelName() != null
            ? request.getModelConfig().getModelName()
            : defaultModel;
        requestBody.put("model", model);

        // 构建消息列表 - OpenAI兼容格式
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

        // 配置生成参数
        requestBody.put("stream", true); // 启用流式响应

        if (request.getModelConfig() != null) {
            if (request.getModelConfig().getTemperature() != null) {
                requestBody.put("temperature", request.getModelConfig().getTemperature());
            }
            if (request.getModelConfig().getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getModelConfig().getMaxTokens());
            }
            if (request.getModelConfig().getTopP() != null) {
                requestBody.put("top_p", request.getModelConfig().getTopP());
            }
        }

        // 默认参数
        requestBody.putIfAbsent("temperature", 0.7);
        requestBody.putIfAbsent("max_tokens", 4096);
        requestBody.putIfAbsent("top_p", 0.9);

        logger.debug("构建的硅基流动AI请求: model={}, messages_count={}", model, messages.size());
        return requestBody;
    }

    private Flux<UnifiedAiStreamChunk> parseSiliconFlowStreamChunk(String jsonData) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonData);

            // 检查是否有错误
            if (jsonNode.has("error")) {
                JsonNode errorNode = jsonNode.get("error");
                String errorMessage = errorNode.has("message") ? errorNode.get("message").asText() : "Unknown error";
                String errorCode = errorNode.has("code") ? errorNode.get("code").asText() : "";
                logger.error("硅基流动AI API返回错误: {} (code: {})", errorMessage, errorCode);

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

                    // 过滤无效内容
                    if (content == null || content.trim().isEmpty() || "null".equals(content)) {
                        return Flux.empty();
                    }

                    UnifiedAiStreamChunk chunk = new UnifiedAiStreamChunk();
                    chunk.setContent(content);

                    // 检查是否完成
                    String finishReason = choice.has("finish_reason") && !choice.get("finish_reason").isNull()
                        ? choice.get("finish_reason").asText() : null;
                    boolean isFinished = "stop".equals(finishReason) || "length".equals(finishReason) || "content_filter".equals(finishReason);
                    chunk.setIsFinal(isFinished);

                    logger.debug("解析硅基流动AI响应: content={}, isFinished={}", content, isFinished);
                    return Flux.just(chunk);
                }
            }

            // 空响应或无有效内容
            return Flux.empty();

        } catch (Exception e) {
            logger.error("解析硅基流动AI流式响应失败: {}", e.getMessage());
            logger.debug("失败的响应内容: {}", jsonData);
            return Flux.empty();
        }
    }
}