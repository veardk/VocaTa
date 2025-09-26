package com.vocata.voice.service;

import com.vocata.voice.entity.VoiceProfile;
import java.util.Optional;

/**
 * 音色配置服务接口
 */
public interface VoiceProfileService {

    /**
     * 根据音色ID和提供商获取音色配置
     */
    Optional<VoiceProfile> getByVoiceIdAndProvider(String voiceId, String provider);

    /**
     * 根据音色ID获取提供商音色参数
     * 这是TTS服务最常用的方法
     */
    String getProviderVoiceId(String voiceId, String provider);
}