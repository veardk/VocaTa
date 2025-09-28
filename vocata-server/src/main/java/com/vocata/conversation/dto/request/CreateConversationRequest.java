package com.vocata.conversation.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 创建新对话请求
 */
public class CreateConversationRequest {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long characterId;

    /**
     * 对话标题（可选，如果不提供则由LLM生成）
     */
    private String title;

    // Getters and Setters

    public Long getCharacterId() {
        return characterId;
    }

    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}