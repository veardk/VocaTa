package com.vocata.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类 - 所有数据库实体都应继承此类
 * 使用MyBatis-Plus自动填充功能
 */
public class BaseEntity implements Serializable {

    @TableField(fill = FieldFill.INSERT, value = "create_id")
    private Long createId;

    @TableField(fill = FieldFill.INSERT, value = "create_date")
    private LocalDateTime createDate;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "update_id")
    private Long updateId;

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "update_date")
    private LocalDateTime updateDate;

    @TableLogic
    @TableField(value = "is_delete")
    private Integer isDelete;

    public Long getCreateId() {
        return createId;
    }

    public void setCreateId(Long createId) {
        this.createId = createId;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public Long getUpdateId() {
        return updateId;
    }

    public void setUpdateId(Long updateId) {
        this.updateId = updateId;
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
}