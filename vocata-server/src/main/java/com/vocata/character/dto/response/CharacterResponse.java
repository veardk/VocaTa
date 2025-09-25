package com.vocata.character.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 角色响应DTO
 */
public class CharacterResponse {

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
     * 角色头像URL
     */
    private String avatarUrl;

    /**
     * 标签数组JSON：["动漫","治愈","女友"]
     */
    private String tags;

    /**
     * 主要语言
     */
    private String language;

    /**
     * 状态：1=已发布 2=审核中 3=已下架
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 是否官方角色：0=否 1=是
     */
    private Integer isOfficial;

    /**
     * 是否精选推荐：0=否 1=是
     */
    private Integer isFeatured;

    /**
     * 是否热门：0=否 1=是
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
     * 使用用户数
     */
    private Integer userCount;

    /**
     * 是否私有：false=公开 true=私有
     */
    private Boolean isPrivate;

    // ========== 新增标签相关字段 ==========

    /**
     * 标签ID数组（新增字段）
     */
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private Long[] tagIds;

    /**
     * 标签名称数组（新增字段）
     */
    private String[] tagNames;

    /**
     * 主要标签ID数组（新增字段）
     */
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private Long[] primaryTagIds;

    /**
     * 标签摘要（新增字段）
     */
    private String tagSummary;

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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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

    public Integer getUserCount() {
        return userCount;
    }

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
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

    // ========== 新增字段的getter和setter方法 ==========

    public Long[] getTagIds() {
        return tagIds;
    }

    public void setTagIds(Long[] tagIds) {
        this.tagIds = tagIds;
    }

    public String[] getTagNames() {
        return tagNames;
    }

    public void setTagNames(String[] tagNames) {
        this.tagNames = tagNames;
    }

    public Long[] getPrimaryTagIds() {
        return primaryTagIds;
    }

    public void setPrimaryTagIds(Long[] primaryTagIds) {
        this.primaryTagIds = primaryTagIds;
    }

    public String getTagSummary() {
        return tagSummary;
    }

    public void setTagSummary(String tagSummary) {
        this.tagSummary = tagSummary;
    }
}