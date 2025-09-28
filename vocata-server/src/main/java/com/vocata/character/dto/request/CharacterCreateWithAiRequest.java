package com.vocata.character.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 带AI生成的角色创建请求DTO
 * 用于创建角色时自动生成AI设定并异步更新到数据库
 */
public class CharacterCreateWithAiRequest {

    /**
     * 角色名称（必填）
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称不能超过100个字符")
    private String name;

    /**
     * 角色简短描述（必填）
     */
    @NotBlank(message = "角色描述不能为空")
    @Size(max = 500, message = "角色描述不能超过500个字符")
    private String description;

    /**
     * 角色打招呼语（必填）
     */
    @NotBlank(message = "角色打招呼语不能为空")
    @Size(max = 200, message = "角色打招呼语不能超过200个字符")
    private String greeting;

    /**
     * 角色头像URL（可选）
     */
    private String avatarUrl;

    /**
     * 是否私有（可选，默认false）
     */
    private Boolean isPrivate = false;

    // Getters and Setters
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

    @Override
    public String toString() {
        return "CharacterCreateWithAiRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", greeting='" + greeting + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", isPrivate=" + isPrivate +
                '}';
    }
}