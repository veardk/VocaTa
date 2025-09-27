package com.vocata.voice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.vocata.common.entity.BaseEntity;

/**
 * TTS音色实体类
 * 对应数据库表 vocata_tts_voices
 */
@TableName("vocata_tts_voices")
public class TtsVoice extends BaseEntity {

    /**
     * 音色主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * TTS服务商提供的声音ID (例如 ElevenLabs 的 "21m00Tcm4TlvDq8ikWAM")
     */
    private String providerVoiceId;

    /**
     * 声音的人类可读名称 (例如 "艾拉-温柔女声")
     */
    private String name;

    /**
     * TTS服务提供商 (例如 "ElevenLabs", "Azure", "OpenAI")
     */
    private String provider;

    /**
     * 主要支持的语言代码 (例如 "zh-CN", "en-US")
     */
    private String languageCode;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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