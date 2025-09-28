package com.vocata.voice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.vocata.common.entity.BaseEntity;

/**
 * 音色配置表（精简版）
 */
@TableName("vocata_voice_profile")
public class VoiceProfile extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 业务音色ID（如：voice-en-harry）
     */
    private String voiceId;

    /**
     * 音色名称（如：哈利波特）
     */
    private String voiceName;

    /**
     * TTS提供商（如：xunfei）
     */
    private String provider;

    /**
     * 提供商真实音色参数（如：aisjiuxu）
     */
    private String providerVoiceId;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderVoiceId() {
        return providerVoiceId;
    }

    public void setProviderVoiceId(String providerVoiceId) {
        this.providerVoiceId = providerVoiceId;
    }
}