package com.vocata.character.dto.response;

import java.util.List;

/**
 * AI角色生成响应DTO
 * 返回AI生成的详细角色设定内容
 */
public class CharacterAiGenerateResponse {

    /**
     * 原始输入的角色名称
     */
    private String name;

    /**
     * 原始输入的角色描述
     */
    private String description;

    /**
     * 原始输入的角色打招呼语
     */
    private String greeting;

    /**
     * AI生成的完整角色设定内容
     */
    private String generatedContent;

    /**
     * 提取出的人设prompt（给LLM的核心指令）
     */
    private String persona;

    /**
     * AI生成的性格特征标签（最多两个）
     */
    private List<String> personalityTraits;

    /**
     * AI生成的说话风格描述
     */
    private String speakingStyle;

    /**
     * AI生成的示例对话（5个以内）
     */
    private List<DialogueExample> exampleDialogues;

    /**
     * AI生成的标签（最多两个）
     */
    private List<String> tags;

    /**
     * AI生成的搜索关键字
     */
    private String searchKeywords;

    /**
     * AI生成的耗时（毫秒）
     */
    private Long generationTime;

    /**
     * 使用的AI模型信息
     */
    private String modelUsed;

    /**
     * 示例对话内部类
     */
    public static class DialogueExample {
        private String user;
        private String assistant;

        public DialogueExample() {}

        public DialogueExample(String user, String assistant) {
            this.user = user;
            this.assistant = assistant;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getAssistant() {
            return assistant;
        }

        public void setAssistant(String assistant) {
            this.assistant = assistant;
        }
    }

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

    public String getGeneratedContent() {
        return generatedContent;
    }

    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public List<String> getPersonalityTraits() {
        return personalityTraits;
    }

    public void setPersonalityTraits(List<String> personalityTraits) {
        this.personalityTraits = personalityTraits;
    }

    public String getSpeakingStyle() {
        return speakingStyle;
    }

    public void setSpeakingStyle(String speakingStyle) {
        this.speakingStyle = speakingStyle;
    }

    public List<DialogueExample> getExampleDialogues() {
        return exampleDialogues;
    }

    public void setExampleDialogues(List<DialogueExample> exampleDialogues) {
        this.exampleDialogues = exampleDialogues;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public Long getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(Long generationTime) {
        this.generationTime = generationTime;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    @Override
    public String toString() {
        return "CharacterAiGenerateResponse{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", greeting='" + greeting + '\'' +
                ", generatedContent='" + (generatedContent != null ? generatedContent.substring(0, Math.min(100, generatedContent.length())) + "..." : null) + '\'' +
                ", persona='" + (persona != null ? persona.substring(0, Math.min(100, persona.length())) + "..." : null) + '\'' +
                ", personalityTraits=" + personalityTraits +
                ", speakingStyle='" + speakingStyle + '\'' +
                ", exampleDialogues=" + (exampleDialogues != null ? exampleDialogues.size() : 0) + " dialogues" +
                ", tags=" + tags +
                ", searchKeywords='" + searchKeywords + '\'' +
                ", generationTime=" + generationTime +
                ", modelUsed='" + modelUsed + '\'' +
                '}';
    }
}