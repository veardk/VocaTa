package com.vocata.character.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 标签统计实体类
 * 对应数据库表：vocata_tag_stats
 */
@TableName("vocata_tag_stats")
public class TagStats {

    /**
     * 统计记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 标签ID
     */
    @TableField("tag_id")
    private Long tagId;

    /**
     * 统计日期
     */
    @TableField("stat_date")
    private LocalDate statDate;

    /**
     * 关联角色数量
     */
    @TableField("character_count")
    private Integer characterCount;

    /**
     * 新增角色数量（当日）
     */
    @TableField("new_character_count")
    private Integer newCharacterCount;

    /**
     * 移除角色数量（当日）
     */
    @TableField("removed_character_count")
    private Integer removedCharacterCount;

    /**
     * 搜索次数
     */
    @TableField("search_count")
    private Long searchCount;

    /**
     * 点击次数
     */
    @TableField("click_count")
    private Long clickCount;

    /**
     * 筛选次数
     */
    @TableField("filter_count")
    private Long filterCount;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Long viewCount;

    /**
     * 对话开始次数
     */
    @TableField("chat_start_count")
    private Long chatStartCount;

    /**
     * 收藏次数
     */
    @TableField("favorite_count")
    private Integer favoriteCount;

    /**
     * 分享次数
     */
    @TableField("share_count")
    private Integer shareCount;

    /**
     * 日热度分数
     */
    @TableField("daily_score")
    private BigDecimal dailyScore;

    /**
     * 周热度分数
     */
    @TableField("weekly_score")
    private BigDecimal weeklyScore;

    /**
     * 月热度分数
     */
    @TableField("monthly_score")
    private BigDecimal monthlyScore;

    /**
     * 趋势分数（综合算法）
     */
    @TableField("trending_score")
    private BigDecimal trendingScore;

    /**
     * 日增长率（%）
     */
    @TableField("daily_growth_rate")
    private BigDecimal dailyGrowthRate;

    /**
     * 周增长率（%）
     */
    @TableField("weekly_growth_rate")
    private BigDecimal weeklyGrowthRate;

    /**
     * 平均角色评分
     */
    @TableField("avg_character_rating")
    private BigDecimal avgCharacterRating;

    /**
     * 平均会话时长（分钟）
     */
    @TableField("avg_session_duration")
    private Integer avgSessionDuration;

    /**
     * 平均消息数量
     */
    @TableField("avg_message_count")
    private Integer avgMessageCount;

    /**
     * 用户偏好分数
     */
    @TableField("user_preference_score")
    private BigDecimal userPreferenceScore;

    /**
     * 男性用户占比
     */
    @TableField("male_user_ratio")
    private BigDecimal maleUserRatio;

    /**
     * 女性用户占比
     */
    @TableField("female_user_ratio")
    private BigDecimal femaleUserRatio;

    /**
     * 年龄分布统计（JSON格式）
     */
    @TableField("age_distribution")
    private String ageDistribution;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
    }

    public Integer getNewCharacterCount() {
        return newCharacterCount;
    }

    public void setNewCharacterCount(Integer newCharacterCount) {
        this.newCharacterCount = newCharacterCount;
    }

    public Integer getRemovedCharacterCount() {
        return removedCharacterCount;
    }

    public void setRemovedCharacterCount(Integer removedCharacterCount) {
        this.removedCharacterCount = removedCharacterCount;
    }

    public Long getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(Long searchCount) {
        this.searchCount = searchCount;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public Long getFilterCount() {
        return filterCount;
    }

    public void setFilterCount(Long filterCount) {
        this.filterCount = filterCount;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getChatStartCount() {
        return chatStartCount;
    }

    public void setChatStartCount(Long chatStartCount) {
        this.chatStartCount = chatStartCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Integer getShareCount() {
        return shareCount;
    }

    public void setShareCount(Integer shareCount) {
        this.shareCount = shareCount;
    }

    public BigDecimal getDailyScore() {
        return dailyScore;
    }

    public void setDailyScore(BigDecimal dailyScore) {
        this.dailyScore = dailyScore;
    }

    public BigDecimal getWeeklyScore() {
        return weeklyScore;
    }

    public void setWeeklyScore(BigDecimal weeklyScore) {
        this.weeklyScore = weeklyScore;
    }

    public BigDecimal getMonthlyScore() {
        return monthlyScore;
    }

    public void setMonthlyScore(BigDecimal monthlyScore) {
        this.monthlyScore = monthlyScore;
    }

    public BigDecimal getTrendingScore() {
        return trendingScore;
    }

    public void setTrendingScore(BigDecimal trendingScore) {
        this.trendingScore = trendingScore;
    }

    public BigDecimal getDailyGrowthRate() {
        return dailyGrowthRate;
    }

    public void setDailyGrowthRate(BigDecimal dailyGrowthRate) {
        this.dailyGrowthRate = dailyGrowthRate;
    }

    public BigDecimal getWeeklyGrowthRate() {
        return weeklyGrowthRate;
    }

    public void setWeeklyGrowthRate(BigDecimal weeklyGrowthRate) {
        this.weeklyGrowthRate = weeklyGrowthRate;
    }

    public BigDecimal getAvgCharacterRating() {
        return avgCharacterRating;
    }

    public void setAvgCharacterRating(BigDecimal avgCharacterRating) {
        this.avgCharacterRating = avgCharacterRating;
    }

    public Integer getAvgSessionDuration() {
        return avgSessionDuration;
    }

    public void setAvgSessionDuration(Integer avgSessionDuration) {
        this.avgSessionDuration = avgSessionDuration;
    }

    public Integer getAvgMessageCount() {
        return avgMessageCount;
    }

    public void setAvgMessageCount(Integer avgMessageCount) {
        this.avgMessageCount = avgMessageCount;
    }

    public BigDecimal getUserPreferenceScore() {
        return userPreferenceScore;
    }

    public void setUserPreferenceScore(BigDecimal userPreferenceScore) {
        this.userPreferenceScore = userPreferenceScore;
    }

    public BigDecimal getMaleUserRatio() {
        return maleUserRatio;
    }

    public void setMaleUserRatio(BigDecimal maleUserRatio) {
        this.maleUserRatio = maleUserRatio;
    }

    public BigDecimal getFemaleUserRatio() {
        return femaleUserRatio;
    }

    public void setFemaleUserRatio(BigDecimal femaleUserRatio) {
        this.femaleUserRatio = femaleUserRatio;
    }

    public String getAgeDistribution() {
        return ageDistribution;
    }

    public void setAgeDistribution(String ageDistribution) {
        this.ageDistribution = ageDistribution;
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