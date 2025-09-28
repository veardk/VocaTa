package com.vocata.voice.dto;

/**
 * TTS音色响应
 */
public class TtsVoiceResponse {

    private String id;

    private String providerVoiceId;

    private String name;

    private String provider;

    private String languageCode;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderVoiceId() {
        return providerVoiceId;
    }

    public void setProviderVoiceId(String providerVoiceId) {
        this.providerVoiceId = providerVoiceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}