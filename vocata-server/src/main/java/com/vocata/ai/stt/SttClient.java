package com.vocata.ai.stt;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * STT (Speech to Text) 服务客户端
 * 支持实时语音识别
 */
public interface SttClient {

    /**
     * 获取服务提供商名称
     */
    String getProviderName();

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();

    /**
     * 流式语音识别
     * 接收音频流并返回实时识别结果
     *
     * @param audioStream 音频数据流（二进制）
     * @param config 识别配置
     * @return 识别结果流
     */
    Flux<SttResult> streamRecognize(Flux<byte[]> audioStream, SttConfig config);

    /**
     * 批量语音识别
     * 处理完整音频文件
     *
     * @param audioData 完整音频数据
     * @param config 识别配置
     * @return 完整识别结果
     */
    Mono<SttResult> recognize(byte[] audioData, SttConfig config);

    /**
     * 语音识别结果
     */
    class SttResult {
        private String text;              // 识别的文本
        private double confidence;        // 置信度 (0.0-1.0)
        private boolean isFinal;         // 是否为最终结果
        private long startTimeMs;        // 开始时间（毫秒）
        private long endTimeMs;          // 结束时间（毫秒）
        private Map<String, Object> metadata; // 额外元数据

        public SttResult() {}

        public SttResult(String text, double confidence, boolean isFinal) {
            this.text = text;
            this.confidence = confidence;
            this.isFinal = isFinal;
        }

        // Getters and Setters
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public boolean isFinal() {
            return isFinal;
        }

        public void setFinal(boolean aFinal) {
            isFinal = aFinal;
        }

        public long getStartTimeMs() {
            return startTimeMs;
        }

        public void setStartTimeMs(long startTimeMs) {
            this.startTimeMs = startTimeMs;
        }

        public long getEndTimeMs() {
            return endTimeMs;
        }

        public void setEndTimeMs(long endTimeMs) {
            this.endTimeMs = endTimeMs;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * STT配置
     */
    class SttConfig {
        private String language = "zh-CN";    // 识别语言
        private String model;                 // 使用的模型
        private int sampleRate = 16000;       // 采样率
        private String audioFormat = "webm";  // 音频格式
        private boolean enableVAD = true;     // 启用语音活动检测
        private boolean enablePunctuation = true; // 启用标点符号

        public SttConfig() {}

        public SttConfig(String language) {
            this.language = language;
        }

        // Getters and Setters
        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public String getAudioFormat() {
            return audioFormat;
        }

        public void setAudioFormat(String audioFormat) {
            this.audioFormat = audioFormat;
        }

        public boolean isEnableVAD() {
            return enableVAD;
        }

        public void setEnableVAD(boolean enableVAD) {
            this.enableVAD = enableVAD;
        }

        public boolean isEnablePunctuation() {
            return enablePunctuation;
        }

        public void setEnablePunctuation(boolean enablePunctuation) {
            this.enablePunctuation = enablePunctuation;
        }
    }
}