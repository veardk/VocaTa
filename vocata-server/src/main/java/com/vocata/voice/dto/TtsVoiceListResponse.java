package com.vocata.voice.dto;

/**
 * TTS音色列表项响应（仅包含id和name）
 */
public class TtsVoiceListResponse {

    private String id;

    private String name;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}