package com.vocata.ai.llm;

import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import reactor.core.publisher.Flux;

/**
 * LLM Provider接口
 * 定义统一的AI模型调用标准，使用最合适的设计模式：策略模式
 *
 * 支持多种LLM提供商：OpenAI、Anthropic、Local Models等
 */
public interface LlmProvider {

    /**
     * 获取提供商名称
     */
    String getProviderName();

    /**
     * 检查提供商是否可用
     */
    boolean isAvailable();

    /**
     * 流式聊天接口
     *
     * @param request 统一的AI请求格式
     * @return 响应式流，包含逐个返回的文本块
     */
    Flux<UnifiedAiStreamChunk> streamChat(UnifiedAiRequest request);

    /**
     * 同步聊天接口（基于流式接口实现）
     *
     * @param request 统一的AI请求格式
     * @return 完整的响应结果
     */
    default UnifiedAiStreamChunk chat(UnifiedAiRequest request) {
        return streamChat(request)
                .filter(chunk -> chunk.getIsFinal() != null && chunk.getIsFinal())
                .blockFirst();
    }

    /**
     * 获取模型支持的最大上下文长度
     */
    int getMaxContextLength();

    /**
     * 获取支持的模型列表
     */
    String[] getSupportedModels();

    /**
     * 估算Token数量
     */
    int estimateTokens(String text);

    /**
     * 验证模型配置是否有效
     */
    boolean validateModelConfig(UnifiedAiRequest.ModelConfig config);
}