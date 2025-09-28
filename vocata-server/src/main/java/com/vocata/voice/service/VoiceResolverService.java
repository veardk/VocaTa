package com.vocata.voice.service;

/**
 * 音色解析服务
 * 负责将业务音色ID解析为具体TTS提供商的音色参数
 */
public interface VoiceResolverService {

    /**
     * 解析音色ID为指定TTS提供商的音色参数
     *
     * @param voiceId 业务音色ID（如：voice-en-harry）
     * @param provider TTS提供商（如：xunfei）
     * @return TTS提供商的具体音色参数（如：aisjiuxu）
     */
    String resolveVoiceId(String voiceId, String provider);
}