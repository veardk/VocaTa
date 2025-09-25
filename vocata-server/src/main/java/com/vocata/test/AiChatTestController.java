package com.vocata.test;

import com.vocata.ai.service.AiStreamingService;
import com.vocata.conversation.dto.request.CreateConversationRequest;
import com.vocata.conversation.dto.response.ConversationResponse;
import com.vocata.conversation.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AI聊天功能测试控制器
 * 用于测试完整的AI对话功能链路
 */
@RestController
@RequestMapping("/api/test/ai-chat")
public class AiChatTestController {

    private static final Logger logger = LoggerFactory.getLogger(AiChatTestController.class);

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private AiStreamingService aiStreamingService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("message", "AI聊天功能服务正常");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 创建测试对话
     */
    @PostMapping("/create-test-conversation")
    public Map<String, Object> createTestConversation() {
        logger.info("创建测试对话");

        try {
            // 创建测试对话请求
            CreateConversationRequest request = new CreateConversationRequest();
            request.setCharacterId(1L); // 假设存在ID为1的角色
            request.setTitle("AI语音测试对话");

            // 使用测试用户ID
            Long testUserId = 1L;

            ConversationResponse conversation = conversationService.createConversation(testUserId, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("conversation_uuid", conversation.getConversationUuid());
            result.put("websocket_url", "ws://localhost:9009/ws/chat/" + conversation.getConversationUuid() + "?userId=" + testUserId);
            result.put("message", "测试对话创建成功");

            return result;
        } catch (Exception e) {
            logger.error("创建测试对话失败", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 获取WebSocket连接信息
     */
    @GetMapping("/websocket-info/{conversationUuid}")
    public Map<String, Object> getWebSocketInfo(@PathVariable String conversationUuid) {
        Map<String, Object> result = new HashMap<>();
        result.put("websocket_url", "ws://localhost:9009/ws/chat/" + conversationUuid + "?userId=1");
        result.put("protocols", new String[]{"audio", "text"});
        result.put("message_formats", Map.of(
            "control_message", Map.of(
                "audio_start", "{\"type\":\"audio_start\"}",
                "audio_end", "{\"type\":\"audio_end\"}",
                "ping", "{\"type\":\"ping\"}"
            ),
            "expected_responses", Map.of(
                "stt_result", "{\"type\":\"stt_result\",\"payload\":{\"text\":\"...\",\"confidence\":0.95}}",
                "llm_chunk", "{\"type\":\"llm_chunk\",\"payload\":{\"text\":\"...\",\"is_final\":false}}",
                "audio_chunk", "二进制音频数据",
                "complete", "{\"type\":\"complete\",\"message\":\"处理完成\"}"
            )
        ));
        return result;
    }

    /**
     * 模拟音频处理测试
     */
    @PostMapping("/simulate-audio-processing/{conversationUuid}")
    public Map<String, Object> simulateAudioProcessing(@PathVariable String conversationUuid) {
        logger.info("模拟音频处理测试: {}", conversationUuid);

        try {
            // 生成模拟音频数据
            byte[] mockAudioData = generateMockAudioData(1000); // 1KB音频数据

            UUID uuid = UUID.fromString(conversationUuid);
            Long testUserId = 1L;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "音频处理模拟完成");
            result.put("conversation_uuid", conversationUuid);
            result.put("audio_data_size", mockAudioData.length);
            result.put("processing_info", Map.of(
                "stt_provider", "MockSTT",
                "llm_provider", "OpenAI",
                "tts_provider", "MockTTS"
            ));

            return result;
        } catch (Exception e) {
            logger.error("模拟音频处理失败", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * 获取测试指令
     */
    @GetMapping("/test-instructions")
    public Map<String, Object> getTestInstructions() {
        Map<String, Object> instructions = new HashMap<>();

        instructions.put("步骤", new String[]{
            "1. 调用 POST /api/test/ai-chat/create-test-conversation 创建测试对话",
            "2. 从响应中获取 websocket_url",
            "3. 使用WebSocket客户端连接到该URL",
            "4. 发送控制消息: {\"type\":\"audio_start\"}",
            "5. 发送二进制音频数据（可以是任意二进制数据用于测试）",
            "6. 发送控制消息: {\"type\":\"audio_end\"}",
            "7. 监听服务器响应：STT结果、LLM文本流、TTS音频流"
        });

        instructions.put("测试命令示例", Map.of(
            "curl_create_conversation", "curl -X POST http://localhost:9009/api/test/ai-chat/create-test-conversation",
            "websocket_test", "使用WebSocket客户端（如wscat）: wscat -c \"ws://localhost:9009/ws/chat/{conversation_uuid}?userId=1\""
        ));

        instructions.put("预期响应", Map.of(
            "stt_result", "收到语音识别结果的JSON消息",
            "llm_chunk", "收到LLM生成的文本流JSON消息",
            "audio_chunk", "收到TTS合成的音频二进制数据",
            "complete", "收到处理完成的JSON消息"
        ));

        return instructions;
    }

    /**
     * 生成模拟音频数据
     */
    private byte[] generateMockAudioData(int size) {
        byte[] audioData = new byte[size];
        for (int i = 0; i < size; i++) {
            // 生成简单的音频波形数据
            audioData[i] = (byte) (Math.sin(2.0 * Math.PI * 440 * i / 44100.0) * 127);
        }
        return audioData;
    }
}