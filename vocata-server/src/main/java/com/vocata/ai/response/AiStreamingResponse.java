package com.vocata.ai.response;

import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.stt.SttClient;
import com.vocata.ai.tts.TtsClient;

import java.util.Map;

/**
 * AI流式响应封装类
 */
public class AiStreamingResponse {
    private ResponseType type;
    private SttClient.SttResult sttResult;
    private UnifiedAiStreamChunk llmChunk;
    private byte[] audioData;
    private TtsClient.TtsResult ttsResult;
    private String error;
    private Map<String, Object> metadata;

    public enum ResponseType {
        STT_RESULT,    // STT识别结果
        LLM_CHUNK,     // LLM文本流块
        AUDIO_CHUNK,   // TTS音频流块
        TTS_RESULT,    // TTS结果（同时包含音频和文字）
        ERROR,         // 错误信息
        COMPLETE       // 完成信号
    }

    // Getters and Setters
    public ResponseType getType() {
        return type;
    }

    public void setType(ResponseType type) {
        this.type = type;
    }

    public SttClient.SttResult getSttResult() {
        return sttResult;
    }

    public void setSttResult(SttClient.SttResult sttResult) {
        this.sttResult = sttResult;
    }

    public UnifiedAiStreamChunk getLlmChunk() {
        return llmChunk;
    }

    public void setLlmChunk(UnifiedAiStreamChunk llmChunk) {
        this.llmChunk = llmChunk;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }

    public TtsClient.TtsResult getTtsResult() {
        return ttsResult;
    }

    public void setTtsResult(TtsClient.TtsResult ttsResult) {
        this.ttsResult = ttsResult;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}