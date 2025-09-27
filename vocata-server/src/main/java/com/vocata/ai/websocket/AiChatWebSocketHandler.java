package com.vocata.ai.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.vocata.ai.service.AiStreamingService;
import com.vocata.ai.service.SttTestService;
import com.vocata.conversation.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI语音对话WebSocket处理器 - STT测试模式
 * 专门用于测试: 音频数据接收 -> STT -> 文字输出到控制台
 * 跳过LLM和TTS处理，专注于STT功能验证
 */
@Component
public class AiChatWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AiChatWebSocketHandler.class);

    @Autowired
    private AiStreamingService aiStreamingService;

    @Autowired
    private SttTestService sttTestService;

    @Autowired
    private ConversationService conversationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储每个会话的音频流
    private final Map<String, Sinks.Many<byte[]>> audioSinks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("AI语音WebSocket连接建立: {}", session.getId());

        // 验证用户身份
        String authenticatedUserId = authenticateUser(session);
        if (authenticatedUserId == null) {
            logger.error("WebSocket连接验证失败，关闭连接: {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("身份验证失败"));
            return;
        }

        // 将认证的用户ID存储到session中
        session.getAttributes().put("authenticatedUserId", authenticatedUserId);
        logger.info("WebSocket用户认证成功: {} - 用户ID: {}", session.getId(), authenticatedUserId);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "status",
                "message", "WebSocket连接已建立",
                "timestamp", System.currentTimeMillis()
        ))));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
        try {
            if (message instanceof BinaryMessage) {
                handleBinaryMessage(session, (BinaryMessage) message);
            } else if (message instanceof TextMessage) {
                handleTextMessage(session, (TextMessage) message);
            }
        } catch (IOException e) {
            logger.error("处理WebSocket消息失败: {}", e.getMessage(), e);
            try {
                sendErrorMessage(session, "消息处理失败: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        String sessionId = session.getId();
        byte[] audioData = message.getPayload().array();

        logger.debug("接收音频数据: {} bytes", audioData.length);

        // 从URI中提取对话UUID
        String uri = session.getUri().toString();
        String conversationUuid = extractConversationUuid(uri);

        // 使用认证的用户ID，不信任URL参数
        String authenticatedUserId = (String) session.getAttributes().get("authenticatedUserId");

        if (conversationUuid != null && authenticatedUserId != null) {
            // 实时处理音频数据 - 流式STT处理
            processAudioStreamRealTime(session, conversationUuid, authenticatedUserId, audioData);
        } else {
            sendErrorMessage(session, "无效的请求URI或身份验证失败");
        }
    }

    /**
     * 实时处理音频流 - 仅STT转文字测试
     */
    private void processAudioStreamRealTime(WebSocketSession session, String conversationUuid, String userId, byte[] audioData) {
        try {
            logger.info("🎤【STT测试模式】开始音频转文字 - 会话: {}, 用户: {}, 音频大小: {} bytes",
                       conversationUuid, userId, audioData.length);

            // 参数验证
            try {
                java.util.UUID.fromString(conversationUuid);
                Long.parseLong(userId);
            } catch (Exception e) {
                logger.error("【参数错误】UUID或用户ID格式错误: conversationUuid={}, userId={}", conversationUuid, userId);
                sendErrorMessage(session, "参数格式错误");
                return;
            }

            // 创建音频流
            Sinks.Many<byte[]> audioSink = Sinks.many().unicast().onBackpressureBuffer();
            audioSink.tryEmitNext(audioData);
            audioSink.tryEmitComplete();

            logger.info("🎤【STT测试模式】开始纯STT识别，跳过LLM和TTS处理");

//             音频 → STT → LLM → TTS → 完整对话
//           TODO需要时去除 aiStreamingService.processVoiceMessage(conversationUuid, userId, audioSink.asFlux())

            // 使用SttTestService的专用方法，只做STT转文字

            sttTestService.processAudioToText(conversationUuid, userId, audioSink.asFlux())
                    .subscribe(
                            response -> {
                                try {
                                    String responseType = (String) response.get("type");

                                    if ("stt_result".equals(responseType)) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                        if (payload != null) {
                                            String recognizedText = (String) payload.get("text");
                                            Boolean isFinal = (Boolean) payload.get("is_final");
                                            Double confidence = (Double) payload.get("confidence");

                                            logger.info("🎤【STT测试结果】识别文本: '{}', 最终: {}, 置信度: {}",
                                                       recognizedText, isFinal, confidence);

                                            // 发送STT结果到前端
                                            sendSttTestResult(session, payload);
                                        }

                                    } else if ("complete".equals(responseType)) {
                                        logger.info("🎤【STT测试完成】音频转文字处理完成");
                                        sendStatusMessage(session, "STT测试完成");

                                    } else if ("error".equals(responseType)) {
                                        String errorMessage = (String) response.get("error");
                                        logger.error("🎤【STT测试错误】: {}", errorMessage);
                                        sendErrorMessage(session, "STT测试失败: " + errorMessage);
                                    }

                                } catch (IOException e) {
                                    logger.error("【发送错误】发送STT测试响应失败", e);
                                }
                            },
                            error -> {
                                logger.error("🎤【STT测试失败】音频转文字失败: {}", error.getMessage(), error);
                                try {
                                    sendErrorMessage(session, "STT测试失败: " + error.getMessage());
                                } catch (IOException e) {
                                    logger.error("【发送错误】无法发送错误消息", e);
                                }
                            },
                            () -> {
                                logger.info("🎤【STT测试完成】音频转文字链路完成");
                            }
                    );

        } catch (Exception e) {
            logger.error("🎤【STT测试异常】音频处理异常: {}", e.getMessage(), e);
            try {
                sendErrorMessage(session, "STT测试异常: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("【发送异常】无法发送异常消息", ex);
            }
        }
    }

    /**
     * 发送STT测试结果
     */
    private void sendSttTestResult(WebSocketSession session, Map<String, Object> payload) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "stt_test_result");
            response.put("text", payload.getOrDefault("text", ""));
            response.put("isFinal", payload.getOrDefault("is_final", false));
            response.put("confidence", payload.getOrDefault("confidence", 0.0));
            response.put("character_name", payload.getOrDefault("character_name", "测试角色"));
            response.put("timestamp", System.currentTimeMillis());

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

            logger.info("🎤【发送到前端】STT测试结果: {}", response);
        } catch (IOException e) {
            logger.error("发送STT测试结果失败", e);
        }
    }

    /**
     * 发送STT识别结果（从payload中提取）
     */
    private void sendSttResultFromPayload(WebSocketSession session, Map<String, Object> payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "stt_result",
                    "text", payload.getOrDefault("text", ""),
                    "isFinal", payload.getOrDefault("is_final", false),
                    "confidence", payload.getOrDefault("confidence", 0.0),
                    "timestamp", System.currentTimeMillis()
            ))));
        } catch (IOException e) {
            logger.error("发送STT结果失败", e);
        }
    }

    /**
     * 发送LLM文本流 - 确保流式响应
     */
    private void sendLlmTextStream(WebSocketSession session, String content, boolean isComplete) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "llm_text_stream",
                    "text", content,
                    "characterName", "AI助手",
                    "isComplete", isComplete,
                    "timestamp", System.currentTimeMillis()
            ))));
        } catch (IOException e) {
            logger.error("发送LLM文本流失败", e);
        }
    }

    /**
     * 发送TTS音频流 - 使用二进制分片传输避免64KB限制
     */
    private void sendTtsAudioStream(WebSocketSession session, byte[] audioData) {
        try {
            logger.info("【TTS输出】发送音频数据到前端 - 大小: {} bytes", audioData.length);

            // 先发送音频元数据（JSON格式）
            Map<String, Object> audioMeta = Map.of(
                    "type", "tts_audio_meta",
                    "audioSize", audioData.length,
                    "format", "mp3", // 科大讯飞TTS返回MP3格式
                    "sampleRate", 24000, // 科大讯飞采样率
                    "channels", 1, // 单声道
                    "bitDepth", 16, // 16位深度
                    "timestamp", System.currentTimeMillis()
            );
            String audioMetaJson = objectMapper.writeValueAsString(audioMeta);
            logger.info("【TTS输出】发送音频元数据: {}", audioMetaJson);
            session.sendMessage(new TextMessage(audioMetaJson));

            // 检查音频数据大小，如果超过50KB则分片传输
            final int MAX_CHUNK_SIZE = 50 * 1024; // 50KB每片，留有余量

            if (audioData.length <= MAX_CHUNK_SIZE) {
                // 小于50KB，直接发送二进制消息
                session.sendMessage(new BinaryMessage(audioData));
                logger.info("【TTS输出】音频数据一次性发送完成 - {} bytes", audioData.length);
            } else {
                // 大于50KB，分片发送
                int totalChunks = (int) Math.ceil((double) audioData.length / MAX_CHUNK_SIZE);
                logger.info("【TTS输出】音频数据过大，分{}片发送 - 总大小: {} bytes", totalChunks, audioData.length);

                for (int i = 0; i < totalChunks; i++) {
                    int start = i * MAX_CHUNK_SIZE;
                    int end = Math.min(start + MAX_CHUNK_SIZE, audioData.length);
                    byte[] chunk = java.util.Arrays.copyOfRange(audioData, start, end);

                    // 发送分片数据
                    session.sendMessage(new BinaryMessage(chunk));
                    logger.info("【TTS输出】发送音频分片 {}/{} - {} bytes", i + 1, totalChunks, chunk.length);

                    // 分片间短暂延迟，避免网络拥塞
                    Thread.sleep(10);
                }
                logger.info("【TTS输出】音频分片发送完成 - 共{}片", totalChunks);
            }

        } catch (Exception e) {
            logger.error("【TTS输出】发送TTS音频流失败", e);
        }
    }

    /**
     * 发送TTS同步结果流 - 先发送文字元数据，再分片传输音频数据
     */
    private void sendTtsResultStream(WebSocketSession session, byte[] audioData, String correspondingText) {
        try {
            logger.info("【TTS同步输出】发送音频和文字数据到前端 - 音频: {} bytes, 文字: '{}'",
                audioData.length, correspondingText != null ? correspondingText : "");

            // 先发送文字和音频元数据（JSON格式）
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "tts_sync_meta",
                    "text", correspondingText != null ? correspondingText : "",
                    "audioSize", audioData.length,
                    "format", "mp3", // 科大讯飞TTS返回MP3格式
                    "sampleRate", 24000, // 科大讯飞采样率
                    "channels", 1, // 单声道
                    "bitDepth", 16, // 16位深度
                    "timestamp", System.currentTimeMillis()
            ))));

            // 然后分片发送音频数据
            sendTtsAudioStream(session, audioData);

            logger.info("【TTS同步输出】音频和文字数据发送完成 - 前端可同步显示和播放");

        } catch (Exception e) {
            logger.error("【TTS同步输出】发送TTS同步结果流失败", e);
        }
    }

    /**
     * 发送TTS音频响应 - 处理payload中的音频数据
     */
    private void sendTtsAudioResponse(WebSocketSession session, Map<String, Object> payload) throws IOException {
        if (payload == null) {
            logger.warn("【TTS输出】音频响应payload为空");
            return;
        }

        Object audioDataObj = payload.get("audio_data");
        if (audioDataObj instanceof byte[]) {
            byte[] audioData = (byte[]) audioDataObj;
            logger.info("【TTS输出】发送音频响应到前端 - 大小: {} bytes", audioData.length);

            // 调用现有的sendTtsAudioStream方法
            sendTtsAudioStream(session, audioData);
        } else {
            logger.error("【TTS输出】无效的音频数据类型: {}",
                audioDataObj != null ? audioDataObj.getClass().getSimpleName() : "null");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            logger.info("收到文本消息: {}", message.getPayload());
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");
            String sessionId = session.getId();

            logger.info("解析消息类型: {}, 会话ID: {}", type, sessionId);

            switch (type) {
                case "audio_start":
                    handleAudioStart(session);
                    break;
                case "audio_end":
                    handleAudioEnd(session, data);
                    break;
                case "text_message":
                    handleTextInput(session, data);
                    break;
                case "ping":
                    sendPongMessage(session);
                    break;
                default:
                    logger.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            logger.error("处理文本消息失败: {}", e.getMessage(), e);
            try {
                sendErrorMessage(session, "消息处理失败: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    private void handleAudioStart(WebSocketSession session) throws IOException {
        String sessionId = session.getId();
        logger.info("开始音频录制: {}", sessionId);

        // 创建音频数据流
        Sinks.Many<byte[]> audioSink = Sinks.many().unicast().onBackpressureBuffer();
        audioSinks.put(sessionId, audioSink);

        sendStatusMessage(session, "开始接收音频数据");
    }

    private void handleAudioEnd(WebSocketSession session, Map<String, Object> data) throws IOException {
        String sessionId = session.getId();
        logger.info("结束音频录制: {}", sessionId);

        Sinks.Many<byte[]> audioSink = audioSinks.remove(sessionId);
        if (audioSink != null) {
            audioSink.tryEmitComplete();

            // 从URI中提取对话UUID
            String uri = session.getUri().toString();
            String conversationUuid = extractConversationUuid(uri);

            // 使用认证的用户ID，不信任URL参数
            String authenticatedUserId = (String) session.getAttributes().get("authenticatedUserId");

            if (conversationUuid != null && authenticatedUserId != null) {
                logger.info("🎤【STT测试模式】处理语音消息结束，会话: {}, 用户: {}", conversationUuid, authenticatedUserId);

                // ====== STT测试模式：仅控制台输出，跳过完整AI处理 ======
                logger.info("🎤【STT测试模式】音频录制结束，跳过完整AI流程");

                // 控制台输出
                System.out.println("========================================");
                System.out.println("🎤 音频录制结束（STT测试模式）");
                System.out.println("🆔 会话UUID: " + conversationUuid);
                System.out.println("👤 用户ID: " + authenticatedUserId);
                System.out.println("⏰ 时间: " + java.time.LocalDateTime.now());
                System.out.println("🎯 模式: 仅STT测试，跳过LLM+TTS");
                System.out.println("========================================");

                // 发送确认响应
                sendStatusMessage(session, "STT测试模式：音频录制结束");

                // ====== 完整AI模式代码（已注释，需要时取消注释） ======
                /*
                // 异步处理音频流
                aiStreamingService.processVoiceMessage(conversationUuid, authenticatedUserId, audioSink.asFlux())
                        .subscribe(
                                response -> {
                                    try {
                                        // 处理 Map<String, Object> 格式的响应
                                        String responseType = (String) response.get("type");

                                        if ("stt_result".equals(responseType)) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                            if (payload != null) {
                                                sendSttResultFromPayload(session, payload);
                                            }
                                        } else if ("llm_chunk".equals(responseType)) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                            if (payload != null) {
                                                String text = (String) payload.get("text");
                                                Boolean isFinal = (Boolean) payload.get("is_final");
                                                sendLlmTextStream(session, text != null ? text : "",
                                                        isFinal != null && isFinal);
                                            }
                                        } else if ("audio_chunk".equals(responseType)) {
                                            byte[] audioData = (byte[]) response.get("audio_data");
                                            if (audioData != null) {
                                                sendTtsAudioStream(session, audioData);
                                            }
                                        } else if ("complete".equals(responseType)) {
                                            sendStatusMessage(session, "语音处理完成");
                                        }
                                    } catch (IOException e) {
                                        logger.error("发送响应失败", e);
                                    }
                                },
                                error -> {
                                    logger.error("处理语音消息失败", error);
                                    try {
                                        sendErrorMessage(session, "语音处理失败: " + error.getMessage());
                                    } catch (IOException e) {
                                        logger.error("发送错误消息失败", e);
                                    }
                                },
                                () -> {
                                    logger.info("语音消息处理完成: {}", sessionId);
                                    try {
                                        sendStatusMessage(session, "语音处理完成");
                                    } catch (IOException e) {
                                        logger.error("发送完成消息失败", e);
                                    }
                                }
                        );
                */
            } else {
                sendErrorMessage(session, "无效的请求URI");
            }
        } else {
            sendErrorMessage(session, "未找到音频流");
        }
    }

    private void sendStatusMessage(WebSocketSession session, String message) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "status",
                "message", message,
                "timestamp", System.currentTimeMillis()
        ))));
    }

    private void sendErrorMessage(WebSocketSession session, String error) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "error",
                "error", error,
                "timestamp", System.currentTimeMillis()
        ))));
    }

    private void sendPongMessage(WebSocketSession session) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "pong",
                "timestamp", System.currentTimeMillis()
        ))));
    }

    /**
     * 验证WebSocket用户身份
     * 从URL参数或Header中获取token，验证并返回用户ID
     */
    private String authenticateUser(WebSocketSession session) {
        try {
            // 尝试从URL参数中获取token
            String uri = session.getUri().toString();
            logger.info("【认证调试】WebSocket URI: {}", uri);
            String token = null;

            if (uri.contains("token=")) {
                String query = uri.split("\\?")[1];
                logger.info("【认证调试】查询参数: {}", query);
                String[] params = query.split("&");
                for (String param : params) {
                    logger.info("【认证调试】处理参数: {}", param);
                    if (param.startsWith("token=")) {
                        token = param.substring("token=".length());
                        // URL解码token
                        token = java.net.URLDecoder.decode(token, "UTF-8");
                        logger.info("【认证调试】从URL参数提取token: {}...", token.substring(0, Math.min(token.length(), 20)));
                        break;
                    }
                }
            }

            // 如果URL参数中没有token，尝试从handshake headers中获取
            if (token == null) {
                logger.info("【认证调试】URL参数中未找到token，尝试从headers获取");
                token = session.getHandshakeHeaders().getFirst("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    logger.info("【认证调试】从Authorization header提取token: {}...", token.substring(0, Math.min(token.length(), 20)));
                }
            }

            if (token == null) {
                logger.error("【认证失败】WebSocket连接缺少认证token");
                return null;
            }

            // 使用Sa-Token验证token
            logger.info("【认证调试】开始验证token...");
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                logger.error("【认证失败】无效的WebSocket认证token: {}...", token.substring(0, Math.min(token.length(), 20)));
                return null;
            }

            logger.info("【认证成功】用户ID: {}", loginId);
            return loginId.toString();
        } catch (Exception e) {
            logger.error("【认证异常】WebSocket用户认证异常", e);
            return null;
        }
    }

    private String extractConversationUuid(String uri) {
        // 从URI中提取对话标识符: /ws/chat/{conversation_uuid}?userId=1
        try {
            String path = uri.split("\\?")[0]; // 去掉查询参数
            String[] parts = path.split("/");
            if (parts.length >= 3 && "chat".equals(parts[parts.length - 2])) {
                return parts[parts.length - 1]; // conversation_uuid
            }
        } catch (Exception e) {
            logger.error("提取对话UUID失败: {}", uri, e);
        }
        return null;
    }

    private String extractUserId(String uri) {
        // 从查询参数中提取用户ID: ?userId=1
        try {
            if (uri.contains("userId=")) {
                String query = uri.split("\\?")[1];
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("userId=")) {
                        return param.substring("userId=".length());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("提取用户ID失败: {}", uri, e);
        }
        return null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        String sessionId = session.getId();
        logger.info("AI语音WebSocket连接关闭: {}, 状态: {}", sessionId, status);

        // 清理资源
        Sinks.Many<byte[]> audioSink = audioSinks.remove(sessionId);
        if (audioSink != null) {
            audioSink.tryEmitComplete();
        }
    }

    /**
     * 处理文字输入消息
     * 用户可以发送文字消息，AI将返回双重响应（文字+语音）
     */
    private void handleTextInput(WebSocketSession session, Map<String, Object> data) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> messageData = (Map<String, Object>) data.get("data");

        if (messageData == null) {
            sendErrorMessage(session, "缺少data字段");
            return;
        }

        String text = (String) messageData.get("message");
        if (text == null || text.trim().isEmpty()) {
            sendErrorMessage(session, "文字内容不能为空");
            return;
        }

        // 从URI中提取对话UUID
        String uri = session.getUri().toString();
        String conversationUuidStr = extractConversationUuid(uri);

        if (conversationUuidStr == null) {
            sendErrorMessage(session, "无效的请求URI，缺少对话UUID");
            return;
        }

        // 使用认证的用户ID，不信任URL参数
        String authenticatedUserId = (String) session.getAttributes().get("authenticatedUserId");
        if (authenticatedUserId == null) {
            sendErrorMessage(session, "用户身份验证失败");
            return;
        }

        logger.info("【文字输入处理】开始处理 - 会话UUID: {}, 认证用户: {}, 文字内容: '{}'",
                conversationUuidStr, authenticatedUserId, text);

        try {
            // 🎤【STT测试模式】文本消息处理 - 仅输出到控制台，不调用LLM和TTS
            logger.info("🎤【文本消息测试】收到文本: '{}', 会话: {}, 用户: {}", text, conversationUuidStr, authenticatedUserId);

            // 控制台输出格式化显示
            System.out.println("========================================");
            System.out.println("📝 收到文本消息:");
            System.out.println("💬 内容: " + text);
            System.out.println("🆔 会话UUID: " + conversationUuidStr);
            System.out.println("👤 用户ID: " + authenticatedUserId);
            System.out.println("⏰ 时间: " + java.time.LocalDateTime.now());
            System.out.println("🎯 模式: STT测试模式 - 跳过LLM+TTS处理");
            System.out.println("========================================");

            // 发送简单的确认响应给前端
            sendStatusMessage(session, "STT测试模式：已收到文本消息");

            // TODO====== 完整AI模式代码（已注释，需要时取消注释） ======
            /*
            // 调用AiStreamingService处理文字消息
            // 这里直接跳过STT步骤，直接使用文字进行LLM+TTS处理
            aiStreamingService.processTextMessage(conversationUuidStr, authenticatedUserId, text)
                    .subscribe(
                            response -> {
                                try {
                                    String responseType = (String) response.get("type");
                                    logger.info("【WebSocket响应】收到响应类型: {}", responseType);

                                    if ("text_chunk".equals(responseType)) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                        if (payload != null) {
                                            String responseText = (String) payload.get("text");
                                            Boolean isFinal = (Boolean) payload.get("is_final");

                                            logger.info("【LLM阶段】流式文本响应 - 内容: '{}', 是否完整: {}",
                                                    responseText, isFinal);

                                            // 发送流式文本响应
                                            sendLlmTextStream(session, responseText != null ? responseText : "",
                                                    isFinal != null && isFinal);
                                        }

                                    } else if ("audio_chunk".equals(responseType)) {
                                        byte[] audioData = (byte[]) response.get("audio_data");
                                        if (audioData != null) {
                                            logger.info("【TTS阶段】收到音频块，大小: {} bytes", audioData.length);

                                            // 发送语音响应
                                            sendTtsAudioStream(session, audioData);
                                        }

                                    } else if ("audio_complete".equals(responseType)) {
                                        logger.info("【TTS阶段】音频合成完成");
                                        // 可以发送音频完成标志
                                        sendStatusMessage(session, "音频合成完成");

                                    } else if ("complete".equals(responseType)) {
                                        logger.info("【处理完成】文字消息处理链路完成");
                                        sendStatusMessage(session, "处理完成");

                                    } else if ("error".equals(responseType)) {
                                        // 错误响应的字段可能是 "error" 或 "message"
                                        String errorMessage = (String) response.get("error");
                                        if (errorMessage == null) {
                                            errorMessage = (String) response.get("message");
                                        }
                                        logger.error("【处理错误】: {}", errorMessage);
                                        sendErrorMessage(session, errorMessage != null ? errorMessage : "处理失败");
                                    }

                                } catch (Exception e) {
                                    logger.error("【响应处理错误】: {}", e.getMessage(), e);
                                    try {
                                        sendErrorMessage(session, "响应处理失败");
                                    } catch (IOException ex) {
                                        logger.error("发送错误消息失败", ex);
                                    }
                                }
                            },
                            error -> {
                                logger.error("【文字消息处理失败】: {}", error.getMessage(), error);
                                try {
                                    sendErrorMessage(session, "文字消息处理失败: " + error.getMessage());
                                } catch (IOException ex) {
                                    logger.error("发送错误消息失败", ex);
                                }
                            }
                    );
            */

        } catch (Exception e) {
            logger.error("【参数错误】UUID或用户ID格式错误: conversationUuid={}, userId={}", conversationUuidStr, authenticatedUserId);
            sendErrorMessage(session, "参数格式错误");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        logger.error("WebSocket传输错误: {}", session.getId(), exception);

        // 清理资源
        String sessionId = session.getId();
        Sinks.Many<byte[]> audioSink = audioSinks.remove(sessionId);
        if (audioSink != null) {
            audioSink.tryEmitComplete();
        }
    }
}