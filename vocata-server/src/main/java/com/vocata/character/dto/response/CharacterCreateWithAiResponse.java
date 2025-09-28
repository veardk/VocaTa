package com.vocata.character.dto.response;

/**
 * 带AI生成的角色创建响应DTO
 * 返回新创建的角色基本信息，AI生成的详细信息会异步更新
 */
public class CharacterCreateWithAiResponse {

    /**
     * 新创建的角色ID
     */
    private String characterId;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色问候语
     */
    private String greeting;

    /**
     * 角色头像URL
     */
    private String avatarUrl;

    /**
     * 是否私有
     */
    private Boolean isPrivate;

    /**
     * 角色状态（1=已发布 2=审核中 3=已下架）
     */
    private Integer status;

    /**
     * AI生成任务状态说明
     */
    private String aiGenerationStatus;

    // Getters and Setters
    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAiGenerationStatus() {
        return aiGenerationStatus;
    }

    public void setAiGenerationStatus(String aiGenerationStatus) {
        this.aiGenerationStatus = aiGenerationStatus;
    }

    @Override
    public String toString() {
        return "CharacterCreateWithAiResponse{" +
                "characterId='" + characterId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", greeting='" + greeting + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", isPrivate=" + isPrivate +
                ", status=" + status +
                ", aiGenerationStatus='" + aiGenerationStatus + '\'' +
                '}';
    }
}