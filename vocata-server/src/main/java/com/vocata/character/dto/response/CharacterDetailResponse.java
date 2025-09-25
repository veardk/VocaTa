package com.vocata.character.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 角色详情响应DTO
 * 包含完整的角色信息，用于编辑和详情页面
 */
public class CharacterDetailResponse {

    /**
     * 角色ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 角色唯一编码
     */
    private String characterCode;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 一句话简介
     */
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
     * 性格特征标签JSON数组
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
     * 标签数组JSON
     */
    private String tags;

    /**
     * 标签权重JSON
     */
    private String tagWeights;

    /**
     * 搜索关键词
     */
    private String searchKeywords;

    /**
     * 主要语言
     */
    private String language;

    /**
     * 默认模型ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long defaultModelId;

    /**
     * 温度参数
     */
    private BigDecimal temperature;

    /**
     * 上下文轮数
     */
    private Integer contextWindow;

    /**
     * 状态：1=已发布 2=审核中 3=已下架
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 是否官方角色
     */
    private Integer isOfficial;

    /**
     * 是否精选推荐
     */
    private Integer isFeatured;

    /**
     * 是否热门
     */
    private Integer isTrending;

    /**
     * 热度分数
     */
    private Integer trendingScore;

    /**
     * 总对话次数
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long chatCount;

    /**
     * 今日对话次数
     */
    private Integer chatCountToday;

    /**
     * 本周对话次数
     */
    private Integer chatCountWeek;

    /**
     * 使用用户数
     */
    private Integer userCount;

    /**
     * 排序权重
     */
    private Integer sortWeight;

    /**
     * 是否私有
     */
    private Boolean isPrivate;

    /**
     * 创建者用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCharacterCode() {
        return characterCode;
    }

    public void setCharacterCode(String characterCode) {
        this.characterCode = characterCode;
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

    public String getTagWeights() {
        return tagWeights;
    }

    public void setTagWeights(String tagWeights) {
        this.tagWeights = tagWeights;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Integer getIsOfficial() {
        return isOfficial;
    }

    public void setIsOfficial(Integer isOfficial) {
        this.isOfficial = isOfficial;
    }

    public Integer getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Integer isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getIsTrending() {
        return isTrending;
    }

    public void setIsTrending(Integer isTrending) {
        this.isTrending = isTrending;
    }

    public Integer getTrendingScore() {
        return trendingScore;
    }

    public void setTrendingScore(Integer trendingScore) {
        this.trendingScore = trendingScore;
    }

    public Long getChatCount() {
        return chatCount;
    }

    public void setChatCount(Long chatCount) {
        this.chatCount = chatCount;
    }

    public Integer getChatCountToday() {
        return chatCountToday;
    }

    public void setChatCountToday(Integer chatCountToday) {
        this.chatCountToday = chatCountToday;
    }

    public Integer getChatCountWeek() {
        return chatCountWeek;
    }

    public void setChatCountWeek(Integer chatCountWeek) {
        this.chatCountWeek = chatCountWeek;
    }

    public Integer getUserCount() {
        return userCount;
    }

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
    }

    public Integer getSortWeight() {
        return sortWeight;
    }

    public void setSortWeight(Integer sortWeight) {
        this.sortWeight = sortWeight;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Long getCreateId() {
        return createId;
    }

    public void setCreateId(Long createId) {
        this.createId = createId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}