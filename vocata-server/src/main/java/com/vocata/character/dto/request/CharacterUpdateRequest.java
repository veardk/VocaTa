package com.vocata.character.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新角色请求DTO
 */
public class CharacterUpdateRequest {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /**
     * 角色名称
     */
    @Size(max = 100, message = "角色名称不能超过100个字符")
    private String name;

    /**
     * 一句话简介
     */
    @Size(max = 500, message = "简介不能超过500个字符")
    private String description;

    /**
     * 开场白
     */
    private String greeting;

    /**
     * 人设prompt（给LLM的核心指令）
     */
    private String persona;

    /**
     * 性格特征标签JSON数组：["温柔","智慧","幽默"]
     */
    private String personalityTraits;

    /**
     * 说话风格描述
     */
    private String speakingStyle;

    /**
     * 示例对话JSON
     */
    private String exampleDialogues;

    /**
     * 角色头像URL
     */
    private String avatarUrl;

    /**
     * 语音ID（TTS服务）
     */
    private String voiceId;

    /**
     * 标签数组JSON：["动漫","治愈","女友"]
     */
    private String tags;

    /**
     * 搜索关键词，用于提升搜索准确度
     */
    private String searchKeywords;

    /**
     * 主要语言
     */
    @Pattern(regexp = "^(zh-CN|en-US|ja-JP|ko-KR)$", message = "不支持的语言")
    private String language;

    /**
     * 默认模型ID
     */
    private Long defaultModelId;

    /**
     * 温度参数
     */
    @DecimalMin(value = "0.0", message = "温度参数最小为0.0")
    @DecimalMax(value = "2.0", message = "温度参数最大为2.0")
    private BigDecimal temperature;

    /**
     * 上下文轮数
     */
    private Integer contextWindow;

    /**
     * 是否私有：false=公开 true=私有
     */
    private Boolean isPrivate;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public String getPersonalityTraits() {
        return personalityTraits;
    }

    public void setPersonalityTraits(String personalityTraits) {
        this.personalityTraits = personalityTraits;
    }

    public String getSpeakingStyle() {
        return speakingStyle;
    }

    public void setSpeakingStyle(String speakingStyle) {
        this.speakingStyle = speakingStyle;
    }

    public String getExampleDialogues() {
        return exampleDialogues;
    }

    public void setExampleDialogues(String exampleDialogues) {
        this.exampleDialogues = exampleDialogues;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Long getDefaultModelId() {
        return defaultModelId;
    }

    public void setDefaultModelId(Long defaultModelId) {
        this.defaultModelId = defaultModelId;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public Integer getContextWindow() {
        return contextWindow;
    }

    public void setContextWindow(Integer contextWindow) {
        this.contextWindow = contextWindow;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}