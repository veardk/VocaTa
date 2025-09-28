package com.vocata.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vocata.common.entity.BaseEntity;
import com.vocata.common.handler.UuidTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.util.UUID;

/**
 * 对话会话实体类
 * 对应数据库表：vocata_conversations
 *
 * 作为聊天记录的容器，连接了特定的用户和特定的角色
 */
@TableName("vocata_conversations")
public class Conversation extends BaseEntity {

    /**
     * 会话主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 对外暴露的对话唯一ID
     */
    @TableField(typeHandler = UuidTypeHandler.class, jdbcType = JdbcType.OTHER)
    private UUID conversationUuid;

    /**
     * 参与会话的用户ID
     */
    private Long userId;

    /**
     * 被聊天的角色ID
     */
    private Long characterId;

    /**
     * 对话标题，可由LLM生成首句摘要
     */
    private String title;

    /**
     * 最新消息摘要，用于会话列表展示
     */
    private String lastMessageSummary;

    /**
     * 会话状态 (0: 活跃, 1: 已归档)
     */
    private Integer status;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getConversationUuid() {
        return conversationUuid;
    }

    public void setConversationUuid(UUID conversationUuid) {
        this.conversationUuid = conversationUuid;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCharacterId() {
        return characterId;
    }

    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
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
}