package com.vocata.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * 统一的AI服务请求格式
 */
public class UnifiedAiRequest {

    /**
     * 系统提示词（角色人设）
     */
    private String systemPrompt;

    /**
     * 用户输入的消息
     */
    private String userMessage;

    /**
     * 对话历史上下文
     */
    private List<ChatMessage> contextMessages;

    /**
     * 模型配置参数
     */
    private ModelConfig modelConfig;

    /**
     * 额外的元数据
     */
    private Map<String, Object> metadata;

    // Getters and Setters

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public List<ChatMessage> getContextMessages() {
        return contextMessages;
    }

    public void setContextMessages(List<ChatMessage> contextMessages) {
        this.contextMessages = contextMessages;
    }

    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    public void setModelConfig(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * 对话消息
     */
    public static class ChatMessage {
        private String role; // "user", "assistant", "system"
        private String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 模型配置
     */
    public static class ModelConfig {
        private String modelName;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Integer contextWindow;

        public ModelConfig() {}

        public ModelConfig(String modelName, Double temperature) {
            this.modelName = modelName;
            this.temperature = temperature;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Double getTopP() {
            return topP;
        }

        public void setTopP(Double topP) {
            this.topP = topP;
        }

        public Integer getContextWindow() {
            return contextWindow;
        }

        public void setContextWindow(Integer contextWindow) {
            this.contextWindow = contextWindow;
        }
    }
}