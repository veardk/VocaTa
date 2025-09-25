package com.vocata.character.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 角色标签关联实体类
 * 对应数据库表：vocata_character_tag
 */
@TableName("vocata_character_tag")
public class CharacterTag {

    /**
     * 关联ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 角色ID
     */
    @TableField("character_id")
    private Long characterId;

    /**
     * 标签ID
     */
    @TableField("tag_id")
    private Long tagId;

    /**
     * 标签权重：1.0-10.0，影响推荐算法
     */
    @TableField("tag_weight")
    private BigDecimal tagWeight;

    /**
     * 是否主要标签：0=否 1=是（最多3-5个）
     */
    @TableField("is_primary")
    private Integer isPrimary;

    /**
     * 是否已验证：0=未验证 1=已验证（管理员审核）
     */
    @TableField("is_verified")
    private Integer isVerified;

    /**
     * 标签来源：manual=手动添加, auto=自动生成, import=导入
     */
    private String source;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 添加者用户ID
     */
    @TableField("added_by")
    private Long addedBy;

    /**
     * 添加者类型：user=用户, admin=管理员, system=系统
     */
    @TableField("added_type")
    private String addedType;

    /**
     * 备注信息
     */
    private String notes;

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

    public Long getCharacterId() {
        return characterId;
    }

    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public BigDecimal getTagWeight() {
        return tagWeight;
    }

    public void setTagWeight(BigDecimal tagWeight) {
        this.tagWeight = tagWeight;
    }

    public Integer getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Integer isPrimary) {
        this.isPrimary = isPrimary;
    }

    public Integer getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Integer isVerified) {
        this.isVerified = isVerified;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(Long addedBy) {
        this.addedBy = addedBy;
    }

    public String getAddedType() {
        return addedType;
    }

    public void setAddedType(String addedType) {
        this.addedType = addedType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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