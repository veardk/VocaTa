package com.vocata.ai.tts;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * TTS (Text to Speech) 服务客户端
 * 支持流式语音合成
 */
public interface TtsClient {

    /**
     * 获取服务提供商名称
     */
    String getProviderName();

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();

    /**
     * 流式语音合成
     * 接收文本流并返回音频流
     *
     * @param textStream 文本数据流
     * @param config 合成配置
     * @return 音频数据流（二进制）
     */
    Flux<byte[]> streamSynthesize(Flux<String> textStream, TtsConfig config);

    /**
     * 批量语音合成
     * 处理完整文本并返回完整音频
     *
     * @param text 要合成的文本
     * @param config 合成配置
     * @return 合成结果（包含音频数据）
     */
    Mono<TtsResult> synthesize(String text, TtsConfig config);

    /**
     * 获取支持的语音列表
     */
    String[] getSupportedVoices();

    /**
     * 估算文本的音频时长（秒）
     */
    double estimateAudioDuration(String text);

    /**
     * 语音合成结果
     */
    class TtsResult {
        private byte[] audioData;         // 音频数据
        private String audioFormat;      // 音频格式
        private int sampleRate;          // 采样率
        private double durationSeconds;   // 音频时长（秒）
        private String voiceId;          // 使用的语音ID
        private Map<String, Object> metadata; // 额外元数据

        public TtsResult() {}

        public TtsResult(byte[] audioData, String audioFormat, double durationSeconds) {
            this.audioData = audioData;
            this.audioFormat = audioFormat;
            this.durationSeconds = durationSeconds;
        }

        // Getters and Setters
        public byte[] getAudioData() {
            return audioData;
        }

        public void setAudioData(byte[] audioData) {
            this.audioData = audioData;
        }

        public String getAudioFormat() {
            return audioFormat;
        }

        public void setAudioFormat(String audioFormat) {
            this.audioFormat = audioFormat;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public double getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(double durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public void setVoiceId(String voiceId) {
            this.voiceId = voiceId;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * TTS配置
     */
    class TtsConfig {
        private String voiceId;           // 语音ID
        private String language = "zh-CN"; // 语言
        private double speed = 1.0;       // 语速 (0.5-2.0)
        private double pitch = 1.0;       // 音调 (0.5-2.0)
        private double volume = 1.0;      // 音量 (0.0-1.0)
        private String audioFormat = "mp3"; // 音频格式
        private int sampleRate = 24000;   // 采样率
        private boolean streaming = false; // 是否流式合成

        public TtsConfig() {}

        public TtsConfig(String voiceId) {
            this.voiceId = voiceId;
        }

        public TtsConfig(String voiceId, String language) {
            this.voiceId = voiceId;
            this.language = language;
        }

        // Getters and Setters
        public String getVoiceId() {
            return voiceId;
        }

        public void setVoiceId(String voiceId) {
            this.voiceId = voiceId;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public double getSpeed() {
            return speed;
        }

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public double getPitch() {
            return pitch;
        }

        public void setPitch(double pitch) {
            this.pitch = pitch;
        }

        public double getVolume() {
            return volume;
        }

        public void setVolume(double volume) {
            this.volume = volume;
        }

        public String getAudioFormat() {
            return audioFormat;
        }

        public void setAudioFormat(String audioFormat) {
            this.audioFormat = audioFormat;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public boolean isStreaming() {
            return streaming;
        }

        public void setStreaming(boolean streaming) {
            this.streaming = streaming;
        }
    }
}