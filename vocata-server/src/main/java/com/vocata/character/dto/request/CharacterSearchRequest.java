package com.vocata.character.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.vocata.common.config.LongDeserializer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * 角色搜索请求DTO
 */
public class CharacterSearchRequest {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 角色状态：1=已发布 2=审核中 3=已下架
     */
    private Integer status;

    /**
     * 是否精选：0=否 1=是
     */
    private Integer isFeatured;

    /**
     * 是否热门：0=否 1=是
     */
    private Integer isTrending;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 语言
     */
    private String language;

    /**
     * 创建者ID（仅管理员和用户查看自己的角色时使用）
     */
    @JsonDeserialize(using = LongDeserializer.class)
    private Long createId;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量最小为1")
    @Max(value = 100, message = "每页数量最大为100")
    private Integer pageSize = 10;

    /**
     * 排序字段：created_at, updated_at, chat_count, trending_score, sort_weight
     */
    private String orderBy = "chat_count";

    /**
     * 排序方向：asc, desc
     */
    private String orderDirection = "desc";

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Long getCreateId() {
        return createId;
    }

    public void setCreateId(Long createId) {
        this.createId = createId;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(String orderDirection) {
        this.orderDirection = orderDirection;
    }
}