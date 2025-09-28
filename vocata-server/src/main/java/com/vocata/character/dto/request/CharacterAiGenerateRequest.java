package com.vocata.character.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI角色生成请求DTO
 * 接收用户输入的基本角色信息，用于AI生成详细角色设定
 */
public class CharacterAiGenerateRequest {

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

    @Override
    public String toString() {
        return "CharacterAiGenerateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", greeting='" + greeting + '\'' +
                '}';
    }
}