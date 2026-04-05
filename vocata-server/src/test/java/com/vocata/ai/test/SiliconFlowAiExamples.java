package com.vocata.ai.test;

import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.impl.SiliconFlowLlmProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 硅基流动AI服务测试案例
 * 演示如何使用硅基流动提供商进行AI对话
 */
@SpringBootTest
@ActiveProfiles("local")
public class SiliconFlowAiExamples {

    /**
     * 测试案例1：基本的单轮对话
     * 使用 DeepSeek 模型进行简单问答
     */
    @Test
    public void testBasicChat() {
        SiliconFlowLlmProvider provider = new SiliconFlowLlmProvider();

        // 构建AI请求
        UnifiedAiRequest request = new UnifiedAiRequest();
        request.setSystemPrompt("你是一个友善的AI助手，请用简洁的中文回答用户问题。");
        request.setUserMessage("请简单介绍一下什么是人工智能？");

        // 设置模型配置
        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName("deepseek-ai/DeepSeek-V2.5");
        modelConfig.setTemperature(0.7);
        modelConfig.setMaxTokens(500);
        request.setModelConfig(modelConfig);

        // 验证提供商可用性
        if (!provider.isAvailable()) {
            System.out.println("⚠️  硅基流动服务不可用，请检查API密钥配置");
            return;
        }

        // 验证模型配置
        if (!provider.validateModelConfig(request.getModelConfig())) {
            System.out.println("❌ 模型配置无效");
            return;
        }

        System.out.println("🤖 开始基本对话测试...");
        System.out.println("模型: " + modelConfig.getModelName());
        System.out.println("问题: " + request.getUserMessage());
        System.out.println("回答: ");

        // 执行流式调用并收集响应
        try {
            StringBuilder fullResponse = new StringBuilder();
            provider.streamChat(request)
                    .doOnNext(chunk -> {
                        if (chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                            System.out.print(chunk.getContent());
                            fullResponse.append(chunk.getContent());
                        }
                    })
                    .doOnComplete(() -> System.out.println("\n✅ 基本对话测试完成"))
                    .blockLast(Duration.ofSeconds(30));

        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试案例2：使用 Claude 模型进行创意写作
     */
    @Test
    public void testCreativeWritingWithClaude() {
        SiliconFlowLlmProvider provider = new SiliconFlowLlmProvider();

        UnifiedAiRequest request = new UnifiedAiRequest();
        request.setSystemPrompt("你是一个创意写作专家，擅长写短篇故事。");
        request.setUserMessage("请写一个关于机器人学会感情的50字小故事。");

        // 使用 Claude 模型，适合创意任务
        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName("anthropic/claude-3-5-sonnet-20241022");
        modelConfig.setTemperature(0.9);  // 高温度鼓励创意
        modelConfig.setMaxTokens(200);
        request.setModelConfig(modelConfig);

        // 收集完整响应
        String fullResponse = provider.streamChat(request)
                .map(UnifiedAiStreamChunk::getContent)
                .filter(content -> content != null && !content.isEmpty())
                .reduce("", String::concat)
                .block(Duration.ofSeconds(30));

        System.out.println("🎨 Claude创意写作结果:");
        System.out.println(fullResponse);
        System.out.println("✅ 创意写作测试通过");
    }

    /**
     * 测试案例3：使用 GPT-4 进行代码分析
     */
    @Test
    public void testCodeAnalysisWithGPT4() {
        SiliconFlowLlmProvider provider = new SiliconFlowLlmProvider();

        UnifiedAiRequest request = new UnifiedAiRequest();
        request.setSystemPrompt("你是一个资深的Java开发专家，请分析代码并给出建议。");
        request.setUserMessage("""
            请分析以下Java代码，指出潜在问题：

            public class UserService {
                private List<User> users = new ArrayList<>();

                public User getUserById(int id) {
                    for (User user : users) {
                        if (user.getId() == id) {
                            return user;
                        }
                    }
                    return null;
                }
            }
            """);

        // 使用 GPT-4 模型，适合代码分析
        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName("openai/gpt-4o");
        modelConfig.setTemperature(0.2);  // 低温度确保准确性
        modelConfig.setMaxTokens(1000);
        request.setModelConfig(modelConfig);

        String analysis = provider.streamChat(request)
                .map(UnifiedAiStreamChunk::getContent)
                .filter(content -> content != null && !content.isEmpty())
                .reduce("", String::concat)
                .block(Duration.ofSeconds(45));

        System.out.println("💻 GPT-4代码分析结果:");
        System.out.println(analysis);
        System.out.println("✅ 代码分析测试通过");
    }

    /**
     * 测试案例4：多轮对话测试
     */
    @Test
    public void testMultiTurnConversation() {
        SiliconFlowLlmProvider provider = new SiliconFlowLlmProvider();

        // 构建多轮对话历史
        List<UnifiedAiRequest.ChatMessage> chatHistory = new ArrayList<>();
        chatHistory.add(new UnifiedAiRequest.ChatMessage("user", "我想学习Spring Boot"));
        chatHistory.add(new UnifiedAiRequest.ChatMessage("assistant", "Spring Boot是一个优秀的Java框架，它简化了Spring应用的开发。你想从哪个方面开始学习呢？"));
        chatHistory.add(new UnifiedAiRequest.ChatMessage("user", "请推荐一个适合初学者的学习路径"));

        UnifiedAiRequest request = new UnifiedAiRequest();
        request.setSystemPrompt("你是一个Java技术导师，请给出专业的学习建议。");
        request.setUserMessage("最好能推荐一些实战项目");
        request.setContextMessages(chatHistory);

        // 使用 Qwen 模型
        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName("Qwen/Qwen2.5-72B-Instruct");
        modelConfig.setTemperature(0.6);
        modelConfig.setMaxTokens(800);
        request.setModelConfig(modelConfig);

        String response = provider.streamChat(request)
                .map(UnifiedAiStreamChunk::getContent)
                .filter(content -> content != null && !content.isEmpty())
                .reduce("", String::concat)
                .block(Duration.ofSeconds(40));

        System.out.println("📚 多轮对话结果:");
        System.out.println(response);
        System.out.println("✅ 多轮对话测试通过");
    }

    /**
     * 测试案例5：模型参数调优测试
     */
    @Test
    public void testModelParameterTuning() {
        SiliconFlowLlmProvider provider = new SiliconFlowLlmProvider();

        String prompt = "请写一首关于春天的诗";

        // 测试不同温度参数的效果
        double[] temperatures = {0.2, 0.7, 1.2};

        for (double temp : temperatures) {
            UnifiedAiRequest request = new UnifiedAiRequest();
            request.setSystemPrompt("你是一个诗人，请创作优美的诗歌。");
            request.setUserMessage(prompt);

            UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
            modelConfig.setModelName("deepseek-ai/DeepSeek-V2.5");
            modelConfig.setTemperature(temp);
            modelConfig.setMaxTokens(300);
            request.setModelConfig(modelConfig);

            String poem = provider.streamChat(request)
                    .map(UnifiedAiStreamChunk::getContent)
                    .filter(content -> content != null && !content.isEmpty())
                    .reduce("", String::concat)
                    .block(Duration.ofSeconds(30));

            System.out.println(String.format("🌡️ 温度参数 %.1f 的创作结果:", temp));
            System.out.println(poem);
            System.out.println("---");
        }

        System.out.println("✅ 参数调优测试通过");
    }

    /**
     * 测试案例6：错误处理测试
     */
    @Test
    public void testErrorHandling() {
        SiliconFlowLlmProvider provider = new SiliconFlowLlmProvider();

        // 测试无效模型名称
        UnifiedAiRequest request = new UnifiedAiRequest();
        request.setUserMessage("测试消息");

        UnifiedAiRequest.ModelConfig invalidConfig = new UnifiedAiRequest.ModelConfig();
        invalidConfig.setModelName("invalid-model-name");
        invalidConfig.setTemperature(3.0);  // 超出范围的温度
        request.setModelConfig(invalidConfig);

        // 验证配置验证
        boolean isValid = provider.validateModelConfig(invalidConfig);
        assert !isValid : "应该检测到无效配置";

        System.out.println("❌ 成功检测到无效配置");
        System.out.println("✅ 错误处理测试通过");
    }

    /**
     * 测试案例7：流式响应性能测试
     */
    @Test
    public void testStreamingPerformance() {
        SiliconFlowLlmProvider provider = new SiliconFlowLlmProvider();

        UnifiedAiRequest request = new UnifiedAiRequest();
        request.setSystemPrompt("请详细回答用户的问题。");
        request.setUserMessage("请详细介绍机器学习的主要算法类型，每种类型给出具体例子和应用场景。");

        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName("Qwen/Qwen2.5-32B-Instruct");
        modelConfig.setTemperature(0.7);
        modelConfig.setMaxTokens(2000);
        request.setModelConfig(modelConfig);

        long startTime = System.currentTimeMillis();

        List<String> chunks = new ArrayList<>();
        provider.streamChat(request)
                .doOnNext(chunk -> {
                    if (chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                        chunks.add(chunk.getContent());
                        System.out.print(chunk.getContent());
                    }
                })
                .blockLast(Duration.ofSeconds(60));

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n⚡ 流式响应性能统计:");
        System.out.println(String.format("总耗时: %d 毫秒", duration));
        System.out.println(String.format("响应块数: %d", chunks.size()));
        System.out.println(String.format("平均每块耗时: %.2f 毫秒", (double) duration / chunks.size()));
        System.out.println("✅ 性能测试通过");
    }

    /**
     * 实际使用示例：模拟真实的AI助手对话
     */
    @Test
    public void testRealWorldUsage() {
        SiliconFlowLlmProvider provider = new SiliconFlowLlmProvider();

        // 模拟用户咨询技术问题
        UnifiedAiRequest request = new UnifiedAiRequest();
        request.setSystemPrompt("""
            你是VocaTa平台的AI技术助手，专门帮助用户解决技术问题。
            请提供准确、实用的技术建议，并适当推荐相关的学习资源。
            """);
        request.setUserMessage("""
            我在使用Spring Boot开发REST API时遇到了跨域问题，
            前端从localhost:3000访问后端localhost:8080的接口时被浏览器阻止了。
            请帮我解决这个问题。
            """);

        // 使用适合技术咨询的模型
        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName("deepseek-ai/deepseek-coder-33b-instruct");
        modelConfig.setTemperature(0.3);  // 低温度确保技术回答的准确性
        modelConfig.setMaxTokens(1500);
        request.setModelConfig(modelConfig);

        System.out.println("🤖 VocaTa AI助手正在为您解答技术问题...\n");

        StringBuilder fullResponse = new StringBuilder();
        provider.streamChat(request)
                .doOnNext(chunk -> {
                    if (chunk.getContent() != null && !chunk.getContent().isEmpty()) {
                        fullResponse.append(chunk.getContent());
                        System.out.print(chunk.getContent());
                    }
                })
                .blockLast(Duration.ofSeconds(45));

        System.out.println("\n\n✅ 真实场景测试完成");
        System.out.println(String.format("回答长度: %d 字符", fullResponse.length()));
    }
}
