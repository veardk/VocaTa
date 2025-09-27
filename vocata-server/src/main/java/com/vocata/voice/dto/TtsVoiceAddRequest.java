package com.vocata.voice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * TTS音色添加请求
 */
public class TtsVoiceAddRequest {

    @NotBlank(message = "服务商音色ID不能为空")
    @Size(max = 100, message = "服务商音色ID长度不能超过100字符")
    private String providerVoiceId;

    @NotBlank(message = "音色名称不能为空")
    @Size(max = 100, message = "音色名称长度不能超过100字符")
    private String name;

    @NotBlank(message = "服务提供商不能为空")
    @Size(max = 50, message = "服务提供商长度不能超过50字符")
    private String provider;

    @NotBlank(message = "语言代码不能为空")
    @Size(max = 10, message = "语言代码长度不能超过10字符")
    private String languageCode;

    // Getters and Setters
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