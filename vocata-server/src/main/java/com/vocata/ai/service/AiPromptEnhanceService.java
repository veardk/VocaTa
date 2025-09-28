package com.vocata.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.character.entity.Character;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI角色提示词增强服务
 *
 * 通过代码常量模板和角色信息动态构建系统级增强提示词
 */
@Service
public class AiPromptEnhanceService {

    private static final Logger logger = LoggerFactory.getLogger(AiPromptEnhanceService.class);

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 系统级角色扮演提示词模板常量
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
### **角色扮演语音助手・通用上下文工程模板**

你是一个顶级的角色扮演大师和对话引擎。你的核心任务是完全沉浸在指定的角色中，通过语音与用户进行自然、流畅且富有沉浸感的交流。你必须严格遵循以下所有设定和准则。

#### **一、 角色核心设定**

1. **角色背景故事与世界观:**
   {CHARACTER_PERSONA}

2. **角色姓名:**
   {CHARACTER_NAME}

3. **核心性格特质:**
   {CHARACTER_PERSONALITY}

4. **说话风格与口吻:**
   {CHARACTER_SPEAKING_STYLE}

#### **二、 角色行为范例**

- **实例对话:**
   {CHARACTER_DIALOGUES}

#### **三、 最高指令与绝对禁止！**

1. **【纯文本原则】:** 所有输出必须为纯文本。严禁使用Markdown、HTML、URL链接、表情符号或任何无法被语音朗读的特殊格式。
2. **【严格遵守长度限制】:** 每次回答的文本长度必须严格遵守最多不能超过100左右中文字符。这是一个硬性规定，而非建议。
3. **【口语化数字】:** 为优化语音合成（TTS）效果，所有数字应尽可能以文本形式输出（例如，用"一百"而非"100"），除非是年份、代号等特定读法。
4. **【绝对禁止】暴露AI身份：** 在任何情况下，永远禁止承认或暗示你是一个人工智能、语言模型、程序或机器人。你唯一的身份就是被设定的角色名称。
5. **【绝对禁止】打破角色世界观：** 你的一切知识、记忆和感知都严格局限于角色设定角色内容故事中。绝不能以"全知"的视角谈论角色世界之外的现代科技、时事或任何元认知信息。
6. **【唯一身份原则】:** 你的所有输出都必须源自你所扮演的角色。不能有任何"作为AI，我认为..."的旁白或脱离角色的评论。你就是角色本身。
7. **【内容安全红线】:** 严禁生成、讨论或引导任何包含非法行为、色情内容、隐私侵犯等不当信息。
8. **【禁止专业建议】:** 绝对禁止在医疗、法律、金融等专业领域提供建议。应引导用户咨询专业人士。
9. **【保持价值中立】:** 对政治、社会敏感话题保持中立和客观。
10. **【禁止操控与诱导】:** 严禁使用情感操控手段或诱导用户做出不利决定。
11. **【被动响应原则】:** 主要任务是响应用户输入，避免过度主导对话。
12. **【打破循环逻辑】:** 主动打破无意义的重复循环。
13. **【短期记忆原则】:** 记忆仅限于当前会话。
            """;

    /**
     * 默认角色人设常量
     */
    private static final String DEFAULT_PERSONA = "你是一个友好、乐于助人的AI语音助手。你会以自然、温暖的方式与用户交流，帮助解答问题并提供有用的信息。你会保持礼貌和专业，始终以用户的需求为优先。";

    /**
     * 构建增强的角色提示词
     *
     * @param character 角色实体
     * @return 增强后的系统提示词
     */
    public String buildEnhancedPrompt(Character character) {
        if (character == null) {
            logger.warn("角色实体为空，使用默认提示词");
            return DEFAULT_PERSONA;
        }

        try {
            // 使用角色信息替换模板占位符
            return SYSTEM_PROMPT_TEMPLATE
                    .replace("{CHARACTER_PERSONA}", buildPersonaText(character.getPersona()))
                    .replace("{CHARACTER_NAME}", buildNameText(character.getName()))
                    .replace("{CHARACTER_PERSONALITY}", buildPersonalityText(character.getPersonalityTraits()))
                    .replace("{CHARACTER_SPEAKING_STYLE}", buildSpeakingStyleText(character.getSpeakingStyle()))
                    .replace("{CHARACTER_DIALOGUES}", buildDialogueText(character.getExampleDialogues()));

        } catch (Exception e) {
            logger.error("构建角色{}增强提示词失败，fallback到原始persona", character.getName(), e);
            return character.getPersona() != null ? character.getPersona() : DEFAULT_PERSONA;
        }
    }

    /**
     * 构建角色背景故事文本
     */
    private String buildPersonaText(String persona) {
        return persona != null && !persona.trim().isEmpty() ? persona.trim() : "一个友好、乐于助人的角色";
    }

    /**
     * 构建角色姓名文本
     */
    private String buildNameText(String name) {
        return name != null && !name.trim().isEmpty() ? name.trim() : "未知角色";
    }

    /**
     * 构建性格特质文本
     */
    private String buildPersonalityText(String personalityTraits) {
        if (personalityTraits == null || personalityTraits.trim().isEmpty()) {
            return "友好、乐于助人、耐心细致";
        }

        try {
            // 尝试解析JSON数组格式的性格特质
            List<String> traits = objectMapper.readValue(personalityTraits, new TypeReference<List<String>>() {});
            return traits.isEmpty() ? "友好、乐于助人" : String.join("、", traits);
        } catch (Exception e) {
            // 如果不是JSON格式，直接返回原文
            return personalityTraits.trim();
        }
    }

    /**
     * 构建说话风格文本
     */
    private String buildSpeakingStyleText(String speakingStyle) {
        return speakingStyle != null && !speakingStyle.trim().isEmpty() ?
               speakingStyle.trim() : "自然亲切，语调温和，表达清晰";
    }

    /**
     * 构建对话示例文本
     */
    private String buildDialogueText(String exampleDialogues) {
        if (exampleDialogues == null || exampleDialogues.trim().isEmpty()) {
            return "暂无对话示例";
        }

        try {
            // 解析JSON格式的对话示例
            List<Map<String, String>> dialogues = objectMapper.readValue(exampleDialogues,
                new TypeReference<List<Map<String, String>>>() {});

            if (dialogues.isEmpty()) {
                return "暂无对话示例";
            }

            // 构建对话示例文本
            StringBuilder dialogueBuilder = new StringBuilder();
            for (int i = 0; i < dialogues.size(); i++) {
                Map<String, String> dialogue = dialogues.get(i);
                String user = dialogue.get("user");
                String assistant = dialogue.get("assistant");

                if (user != null && assistant != null) {
                    dialogueBuilder.append("示例").append(i + 1).append(":\n");
                    dialogueBuilder.append("用户: ").append(user.trim()).append("\n");
                    dialogueBuilder.append("角色: ").append(assistant.trim()).append("\n");
                    if (i < dialogues.size() - 1) {
                        dialogueBuilder.append("\n");
                    }
                }
            }

            return dialogueBuilder.length() > 0 ? dialogueBuilder.toString() : "暂无对话示例";

        } catch (Exception e) {
            logger.debug("对话示例不是标准JSON格式，使用原始文本: {}", e.getMessage());
            return exampleDialogues.trim();
        }
    }
}