package com.vocata.conversation.dto.response;

import java.time.LocalDateTime;

/**
 * 对话列表响应
 */
public class ConversationResponse {

    /**
     * 对话UUID（对外暴露的ID）
     */
    private String conversationUuid;

    /**
     * 角色ID（String类型，避免前端精度丢失）
     */
    private String characterId;

    /**
     * 角色名称
     */
    private String characterName;

    /**
     * 角色头像URL
     */
    private String characterAvatarUrl;

    /**
     * 对话标题
     */
    private String title;

    /**
     * 最新消息摘要
     */
    private String lastMessageSummary;

    /**
     * 会话状态（0: 活跃, 1: 已归档）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createDate;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateDate;

    // Getters and Setters

    public String getConversationUuid() {
        return conversationUuid;
    }

    public void setConversationUuid(String conversationUuid) {
        this.conversationUuid = conversationUuid;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public String getCharacterAvatarUrl() {
        return characterAvatarUrl;
    }

    public void setCharacterAvatarUrl(String characterAvatarUrl) {
        this.characterAvatarUrl = characterAvatarUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastMessageSummary() {
        return lastMessageSummary;
    }

    public void setLastMessageSummary(String lastMessageSummary) {
        this.lastMessageSummary = lastMessageSummary;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }
}