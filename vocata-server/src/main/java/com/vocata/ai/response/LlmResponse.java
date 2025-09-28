package com.vocata.ai.response;

/**
 * LLM响应封装类
 */
public class LlmResponse {
    private String text;
    private String characterName;
    private boolean isComplete;

    public LlmResponse(String text, String characterName, boolean isComplete) {
        this.text = text;
        this.characterName = characterName;
        this.isComplete = isComplete;
    }

    // Getters
    public String getText() {
        return text;
    }

    public String getCharacterName() {
        return characterName;
    }

    public boolean isComplete() {
        return isComplete;
    }
}