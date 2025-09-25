package com.vocata.character.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 角色实体类
 * 对应数据库表：vocata_character
 * 注意：不继承BaseEntity，因为字段映射规则不同
 */
@TableName("vocata_character")
public class Character {

    /**
     * 角色ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 审计字段 - 映射到实际数据库字段
     */
    @TableField("create_id")
    private Long createId;

    @TableField("created_at")
    private LocalDateTime createDate;

    @TableField("updated_at")
    private LocalDateTime updateDate;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    /**
     * 角色唯一编码，用于URL等
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
     * 标签权重JSON：{"动漫":10,"治愈":8}
     */
    private String tagWeights;

    /**
     * 搜索关键词，用于提升搜索准确度
     */
    private String searchKeywords;

    /**
     * 主要语言
     */
    private String language;

    /**
     * 默认模型ID（关联vocata_llm_model）
     */
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
     * 是否官方角色：0=否 1=是
     */
    private Integer isOfficial;

    /**
     * 是否精选推荐：0=否 1=是
     */
    private Integer isFeatured;

    /**
     * 是否热门（自动计算）：0=否 1=是
     */
    private Integer isTrending;

    /**
     * 热度分数（每日更新）
     */
    private Integer trendingScore;

    /**
     * 总对话次数
     */
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
     * 创建者用户ID，官方默认NULL
     */
//    private Long createId;

    /**
     * 是否私有：false=公开 true=私有
     */
    private Boolean isPrivate;

    /**
     * 标签ID数组（新字段，支持数组查询）
     */
    @TableField("tag_ids")
    private Long[] tagIds;

    /**
     * 标签名称数组（冗余字段，提升查询性能）
     */
    @TableField("tag_names")
    private String[] tagNames;

    /**
     * 主要标签ID数组（核心标签，用于推荐算法）
     */
    @TableField("primary_tag_ids")
    private Long[] primaryTagIds;

    /**
     * 标签摘要（自动生成，用于搜索优化）
     */
    @TableField("tag_summary")
    private String tagSummary;


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

    public Long getCreateId() {
        return createId;
    }

    public void setCreateId(Long createId) {
        this.createId = createId;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    // 审计字段的getter和setter方法
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

    public Integer getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }

    // 新增字段的getter和setter方法
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