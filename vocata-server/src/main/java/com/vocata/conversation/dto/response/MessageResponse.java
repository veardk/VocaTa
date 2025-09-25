package com.vocata.conversation.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息响应DTO
 */
public class MessageResponse {

    /**
     * 消息UUID（对外暴露的ID）
     */
    private String messageUuid;

    /**
     * 发送方类型 (1: USER, 2: CHARACTER)
     */
    private Integer senderType;

    /**
     * 内容类型 (1: TEXT, 2: IMAGE, 3: AUDIO)
     */
    private Integer contentType;

    /**
     * 消息的文本内容
     */
    private String textContent;

    /**
     * 消息的语音文件URL
     */
    private String audioUrl;

    /**
     * 生成此条回复所用的LLM模型ID
     */
    private String llmModelId;

    /**
     * 生成此条回复所用的TTS声音ID
     */
    private String ttsVoiceId;

    /**
     * JSON格式的元数据（性能、成本等信息）
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    // Getters and Setters

    public String getMessageUuid() {
        return messageUuid;
    }

    public void setMessageUuid(String messageUuid) {
        this.messageUuid = messageUuid;
    }

    public Integer getSenderType() {
        return senderType;
    }

    public void setSenderType(Integer senderType) {
        this.senderType = senderType;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getLlmModelId() {
        return llmModelId;
    }

    public void setLlmModelId(String llmModelId) {
        this.llmModelId = llmModelId;
    }

    public String getTtsVoiceId() {
        return ttsVoiceId;
    }

    public void setTtsVoiceId(String ttsVoiceId) {
        this.ttsVoiceId = ttsVoiceId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }
}