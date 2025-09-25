package com.vocata.ai.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一的AI服务流式响应块
 */
public class UnifiedAiStreamChunk {

    /**
     * 块的类型
     */
    private ChunkType type;

    /**
     * 块的索引（在整个流中的序号）
     */
    private Integer chunkIndex;

    /**
     * 本块的文本内容
     */
    private String content;

    /**
     * 累积的文本内容
     */
    private String accumulatedContent;

    /**
     * 完成原因（仅在最后一块中有值）
     */
    private String finishReason;

    /**
     * 是否为最终块
     */
    private Boolean isFinal;

    /**
     * Token使用统计
     */
    private TokenUsage tokenUsage;

    /**
     * 性能指标
     */
    private PerformanceMetrics performance;

    /**
     * 额外的元数据
     */
    private Map<String, Object> metadata;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    public UnifiedAiStreamChunk() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters

    public ChunkType getType() {
        return type;
    }

    public void setType(ChunkType type) {
        this.type = type;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAccumulatedContent() {
        return accumulatedContent;
    }

    public void setAccumulatedContent(String accumulatedContent) {
        this.accumulatedContent = accumulatedContent;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public Boolean getIsFinal() {
        return isFinal;
    }

    public void setIsFinal(Boolean isFinal) {
        this.isFinal = isFinal;
    }

    public TokenUsage getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(TokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    public PerformanceMetrics getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceMetrics performance) {
        this.performance = performance;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 块类型枚举
     */
    public enum ChunkType {
        /**
         * 文本内容块
         */
        CONTENT,

        /**
         * 系统状态块
         */
        STATUS,

        /**
         * 错误块
         */
        ERROR,

        /**
         * 完成块
         */
        DONE
    }

    /**
     * Token使用统计
     */
    public static class TokenUsage {
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;

        public TokenUsage() {}

        public TokenUsage(Integer inputTokens, Integer outputTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.totalTokens = (inputTokens != null && outputTokens != null)
                ? inputTokens + outputTokens : null;
        }

        public Integer getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(Integer inputTokens) {
            this.inputTokens = inputTokens;
            updateTotalTokens();
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(Integer outputTokens) {
            this.outputTokens = outputTokens;
            updateTotalTokens();
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        private void updateTotalTokens() {
            if (inputTokens != null && outputTokens != null) {
                this.totalTokens = inputTokens + outputTokens;
            }
        }
    }

    /**
     * 性能指标
     */
    public static class PerformanceMetrics {
        private Long latencyMs;           // 延迟（毫秒）
        private Double tokensPerSecond;   // 生成速度（tokens/秒）
        private Long firstTokenLatencyMs; // 首个token延迟
        private Double qualityScore;      // 质量评分

        public PerformanceMetrics() {}

        public Long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Double getTokensPerSecond() {
            return tokensPerSecond;
        }

        public void setTokensPerSecond(Double tokensPerSecond) {
            this.tokensPerSecond = tokensPerSecond;
        }

        public Long getFirstTokenLatencyMs() {
            return firstTokenLatencyMs;
        }

        public void setFirstTokenLatencyMs(Long firstTokenLatencyMs) {
            this.firstTokenLatencyMs = firstTokenLatencyMs;
        }

        public Double getQualityScore() {
            return qualityScore;
        }

        public void setQualityScore(Double qualityScore) {
            this.qualityScore = qualityScore;
        }
    }
}