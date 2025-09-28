package com.vocata.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.vocata.common.entity.BaseEntity;
import com.vocata.common.handler.UuidTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.util.Map;
import java.util.UUID;

/**
 * 消息实体类
 * 对应数据库表：vocata_messages
 *
 * 存储在一次会话中的所有具体对话内容
 */
@TableName(value = "vocata_messages", autoResultMap = true)
public class Message extends BaseEntity {

    /**
     * 消息主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 对外暴露的消息唯一ID
     */
    @TableField(typeHandler = UuidTypeHandler.class, jdbcType = JdbcType.OTHER)
    private UUID messageUuid;

    /**
     * 所属的对话ID
     */
    private Long conversationId;

    /**
     * 消息发送方 (1: USER, 2: CHARACTER)
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
     * JSON格式，存储所有过程诊断信息（性能、成本等）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getMessageUuid() {
        return messageUuid;
    }

    public void setMessageUuid(UUID messageUuid) {
        this.messageUuid = messageUuid;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
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
}