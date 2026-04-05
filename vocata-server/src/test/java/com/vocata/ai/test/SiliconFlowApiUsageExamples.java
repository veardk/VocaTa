package com.vocata.ai.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 硅基流动AI API调用示例
 * 演示如何通过REST接口使用硅基流动的各种模型
 */
@SpringBootTest
@ActiveProfiles("local")
public class SiliconFlowApiUsageExamples {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 示例1：获取所有可用的AI模型
     * GET /api/client/ai/models
     */
    @Test
    public void exampleGetAvailableModels() {
        System.out.println("📋 获取可用模型列表");
        System.out.println("请求: GET /api/client/ai/models");
        System.out.println();

        // 模拟响应数据
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("providers", Arrays.asList(
            Map.of(
                "providerName", "SiliconFlow AI",
                "beanName", "siliconFlowLlmProvider",
                "isAvailable", true,
                "maxContextLength", 128000,
                "supportedModels", Arrays.asList(
                    "anthropic/claude-3-5-sonnet-20241022",
                    "openai/gpt-4o",
                    "deepseek-ai/DeepSeek-V2.5",
                    "Qwen/Qwen2.5-72B-Instruct"
                )
            )
        ));

        try {
            String jsonResponse = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(mockResponse);
            System.out.println("响应示例:");
            System.out.println(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n✅ 模型列表获取示例完成\n");
    }

    /**
     * 示例2：使用Claude模型进行创意写作
     * POST /api/client/ai/chat
     */
    @Test
    public void exampleClaudeCreativeWriting() {
        System.out.println("🎨 使用Claude进行创意写作");
        System.out.println("请求: POST /api/client/ai/chat");
        System.out.println();

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("providerName", "siliconFlowLlmProvider");
        requestBody.put("modelName", "anthropic/claude-3-5-sonnet-20241022");
        requestBody.put("systemPrompt", "你是一个富有想象力的作家，擅长创作引人入胜的故事。");
        requestBody.put("userMessage", "请写一个关于时间旅行者的短篇科幻故事，大约200字。");
        requestBody.put("temperature", 0.9);
        requestBody.put("maxTokens", 800);

        try {
            String jsonRequest = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(requestBody);
            System.out.println("请求体:");
            System.out.println(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n响应示例:");
        System.out.println("\"在2045年的实验室里，物理学家林博士激活了时间机器...\"");
        System.out.println("\n✅ Claude创意写作示例完成\n");
    }

    /**
     * 示例3：使用GPT-4进行代码审查
     * POST /api/client/ai/chat
     */
    @Test
    public void exampleGPT4CodeReview() {
        System.out.println("💻 使用GPT-4进行代码审查");
        System.out.println("请求: POST /api/client/ai/chat");
        System.out.println();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("providerName", "siliconFlowLlmProvider");
        requestBody.put("modelName", "openai/gpt-4o");
        requestBody.put("systemPrompt", "你是一个资深的软件工程师，请对代码进行专业的review。");
        requestBody.put("userMessage", """
            请审查以下Spring Boot控制器代码：

            @RestController
            @RequestMapping("/api/users")
            public class UserController {
                @Autowired
                private UserService userService;

                @GetMapping("/{id}")
                public User getUser(@PathVariable Long id) {
                    return userService.findById(id);
                }
            }

            指出潜在问题并给出改进建议。
            """);
        requestBody.put("temperature", 0.3);
        requestBody.put("maxTokens", 1200);

        try {
            String jsonRequest = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(requestBody);
            System.out.println("请求体:");
            System.out.println(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n✅ GPT-4代码审查示例完成\n");
    }

    /**
     * 示例4：使用DeepSeek进行技术答疑
     * POST /api/client/ai/chat
     */
    @Test
    public void exampleDeepSeekTechnicalQA() {
        System.out.println("🔍 使用DeepSeek进行技术答疑");
        System.out.println("请求: POST /api/client/ai/chat");
        System.out.println();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("providerName", "siliconFlowLlmProvider");
        requestBody.put("modelName", "deepseek-ai/DeepSeek-V2.5");
        requestBody.put("systemPrompt", "你是VocaTa平台的技术专家，请用专业且易懂的方式回答技术问题。");
        requestBody.put("userMessage", "什么是微服务架构？它相比单体架构有什么优势和挑战？");
        requestBody.put("temperature", 0.6);
        requestBody.put("maxTokens", 1000);

        try {
            String jsonRequest = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(requestBody);
            System.out.println("请求体:");
            System.out.println(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n✅ DeepSeek技术答疑示例完成\n");
    }

    /**
     * 示例5：多轮对话场景
     * POST /api/client/ai/chat
     */
    @Test
    public void exampleMultiTurnConversation() {
        System.out.println("💬 多轮对话示例");
        System.out.println("请求: POST /api/client/ai/chat");
        System.out.println();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("providerName", "siliconFlowLlmProvider");
        requestBody.put("modelName", "Qwen/Qwen2.5-72B-Instruct");
        requestBody.put("systemPrompt", "你是一个耐心的编程导师，请循序渐进地指导学习者。");
        requestBody.put("userMessage", "请推荐一些具体的练习项目");

        // 添加对话历史
        requestBody.put("messages", Arrays.asList(
            Map.of("role", "user", "content", "我想学习Spring Boot，应该从哪里开始？"),
            Map.of("role", "assistant", "content", "建议从Spring Boot基础概念开始，然后学习依赖注入、Web开发、数据访问等核心功能。"),
            Map.of("role", "user", "content", "我已经了解了基础概念，想要实践")
        ));

        requestBody.put("temperature", 0.7);
        requestBody.put("maxTokens", 800);

        try {
            String jsonRequest = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(requestBody);
            System.out.println("请求体:");
            System.out.println(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n✅ 多轮对话示例完成\n");
    }

    /**
     * 示例6：流式调用示例
     * POST /api/client/ai/stream-chat
     */
    @Test
    public void exampleStreamingChat() {
        System.out.println("🌊 流式调用示例");
        System.out.println("请求: POST /api/client/ai/stream-chat");
        System.out.println("Content-Type: application/json");
        System.out.println("Accept: text/event-stream");
        System.out.println();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("providerName", "siliconFlowLlmProvider");
        requestBody.put("modelName", "anthropic/claude-3-5-haiku-20241022");
        requestBody.put("systemPrompt", "你是一个专业的技术写作助手。");
        requestBody.put("userMessage", "请详细解释什么是RESTful API设计原则，并给出实际例子。");
        requestBody.put("temperature", 0.5);
        requestBody.put("maxTokens", 1500);

        try {
            String jsonRequest = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(requestBody);
            System.out.println("请求体:");
            System.out.println(jsonRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n响应流示例:");
        System.out.println("data: RESTful");
        System.out.println("data: API");
        System.out.println("data: 是一种");
        System.out.println("data: 软件架构风格...");
        System.out.println();

        System.out.println("✅ 流式调用示例完成\n");
    }

    /**
     * 示例7：错误处理示例
     */
    @Test
    public void exampleErrorHandling() {
        System.out.println("❌ 错误处理示例");
        System.out.println();

        // 无效提供商示例
        System.out.println("1. 无效提供商错误:");
        Map<String, Object> invalidProviderRequest = new HashMap<>();
        invalidProviderRequest.put("providerName", "nonexistent-provider");
        invalidProviderRequest.put("modelName", "some-model");
        invalidProviderRequest.put("userMessage", "测试消息");

        System.out.println("响应: HTTP 400 Bad Request");
        System.out.println("{\"code\": 400, \"message\": \"未找到指定的AI提供商: nonexistent-provider\"}");
        System.out.println();

        // 无效模型示例
        System.out.println("2. 无效模型配置错误:");
        Map<String, Object> invalidModelRequest = new HashMap<>();
        invalidModelRequest.put("providerName", "siliconFlowLlmProvider");
        invalidModelRequest.put("modelName", "invalid-model-name");
        invalidModelRequest.put("temperature", 3.0); // 超出范围
        invalidModelRequest.put("userMessage", "测试消息");

        System.out.println("响应: HTTP 400 Bad Request");
        System.out.println("{\"code\": 400, \"message\": \"模型配置无效，请检查模型名称和参数\"}");
        System.out.println();

        System.out.println("✅ 错误处理示例完成\n");
    }

    /**
     * 完整的使用说明
     */
    @Test
    public void printUsageGuide() {
        System.out.println("📖 硅基流动AI服务使用指南");
        System.out.println("================================");
        System.out.println();

        System.out.println("1. 配置API密钥");
        System.out.println("   在 application-local.yml 中设置:");
        System.out.println("   siliconflow.ai.api-key: your-api-key");
        System.out.println();

        System.out.println("2. 可用模型列表");
        System.out.println("   • Claude: anthropic/claude-3-5-sonnet-20241022 (创意写作)");
        System.out.println("   • GPT-4: openai/gpt-4o (通用智能)");
        System.out.println("   • DeepSeek: deepseek-ai/DeepSeek-V2.5 (技术问答)");
        System.out.println("   • Qwen: Qwen/Qwen2.5-72B-Instruct (中文对话)");
        System.out.println("   • Llama: meta-llama/Meta-Llama-3.1-70B-Instruct (开源)");
        System.out.println();

        System.out.println("3. 参数调优建议");
        System.out.println("   • 创意任务: temperature=0.8-1.2");
        System.out.println("   • 技术问答: temperature=0.2-0.5");
        System.out.println("   • 日常对话: temperature=0.6-0.8");
        System.out.println("   • 代码生成: temperature=0.1-0.3");
        System.out.println();

        System.out.println("4. API端点");
        System.out.println("   • GET  /api/client/ai/models - 获取模型列表");
        System.out.println("   • POST /api/client/ai/chat - 同步对话");
        System.out.println("   • POST /api/client/ai/stream-chat - 流式对话");
        System.out.println();

        System.out.println("5. 认证要求");
        System.out.println("   需要在请求头中包含:");
        System.out.println("   Authorization: Bearer <your-vocata-token>");
        System.out.println();

        System.out.println("✅ 使用指南完成");
    }
}
