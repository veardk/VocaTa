package com.vocata.ai.websocket;

import com.vocata.ai.service.AiStreamingService;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI语音对话WebSocket处理器
 * 处理语音实时对话: 音频数据接收 -> STT -> LLM -> TTS -> 返回结果
 */
@Component
public class AiChatWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AiChatWebSocketHandler.class);

    @Autowired
    private AiStreamingService aiStreamingService;

    @Autowired
    private ConversationService conversationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储每个会话的音频流
    private final Map<String, Sinks.Many<byte[]>> audioSinks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("AI语音WebSocket连接建立: {}", session.getId());
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

        // 从URI中提取对话ID和用户ID
        String uri = session.getUri().toString();
        String conversationUuid = extractConversationUuid(uri);
        String userId = extractUserId(uri);

        if (conversationUuid != null && userId != null) {
            // 实时处理音频数据 - 流式STT处理
            processAudioStreamRealTime(session, conversationUuid, userId, audioData);
        } else {
            sendErrorMessage(session, "无效的请求URI");
        }
    }

    /**
     * 实时处理音频流 - STT→LLM→TTS流式处理
     */
    private void processAudioStreamRealTime(WebSocketSession session, String conversationUuid, String userId, byte[] audioData) {
        try {
            logger.info("【实时语音处理】开始处理 - 会话: {}, 用户: {}, 音频大小: {} bytes", conversationUuid, userId, audioData.length);

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

            logger.info("【STT阶段】开始完整的流式语音处理");

            // 使用AiStreamingService的完整流式方法
            aiStreamingService.processVoiceMessage(conversationUuid, userId, audioSink.asFlux())
                    .subscribe(
                            response -> {
                                try {
                                    // 处理 Map<String, Object> 格式的响应
                                    String responseType = (String) response.get("type");

                                    if ("stt_result".equals(responseType)) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                        if (payload != null) {
                                            logger.info("【STT阶段】识别结果 - 文本: '{}', 是否最终: {}",
                                                    payload.get("text"), payload.get("is_final"));
                                            sendSttResultFromPayload(session, payload);
                                        }

                                    } else if ("llm_chunk".equals(responseType)) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                        if (payload != null) {
                                            logger.info("【LLM阶段】流式文本响应 - 内容: '{}', 是否完整: {}",
                                                    payload.get("text"), payload.get("is_final"));

                                            String text = (String) payload.get("text");
                                            Boolean isFinal = (Boolean) payload.get("is_final");
                                            // 发送流式文本响应
                                            sendLlmTextStream(session, text != null ? text : "",
                                                    isFinal != null && isFinal);
                                        }

                                    } else if ("audio_chunk".equals(responseType)) {
                                        byte[] audioBytes = (byte[]) response.get("audio_data");
                                        if (audioBytes != null) {
                                            logger.info("【TTS阶段】语音合成完成 - 音频大小: {} bytes", audioBytes.length);
                                            sendTtsAudioStream(session, audioBytes);
                                        }

                                    } else if ("complete".equals(responseType)) {
                                        logger.info("【完整流程】STT→LLM→TTS处理全部完成");
                                        sendStatusMessage(session, "语音处理完成");
                                    } else if ("error".equals(responseType)) {
                                        String errorMessage = (String) response.get("error");
                                        logger.error("【流程错误】AI服务错误: {}", errorMessage);
                                        sendErrorMessage(session, "语音处理失败: " + errorMessage);
                                    }

                                } catch (IOException e) {
                                    logger.error("【发送错误】发送响应到前端失败", e);
                                }
                            },
                            error -> {
                                logger.error("【流程错误】实时语音处理失败 - 错误: {}", error.getMessage(), error);
                                try {
                                    sendErrorMessage(session, "语音处理失败: " + error.getMessage());
                                } catch (IOException e) {
                                    logger.error("【发送错误】无法发送错误消息到前端", e);
                                }
                            },
                            () -> {
                                logger.info("【流程完成】语音处理链路全部完成");
                            }
                    );

        } catch (Exception e) {
            logger.error("【异常捕获】实时音频处理异常 - 错误: {}", e.getMessage(), e);
            try {
                sendErrorMessage(session, "音频处理异常: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("【发送异常】无法发送异常消息到前端", ex);
            }
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
     * 发送TTS音频流 - 同时发送音频数据和元数据（确保流式）
     */
    private void sendTtsAudioStream(WebSocketSession session, byte[] audioData) {
        try {
            logger.info("【TTS输出】发送音频数据到前端 - 大小: {} bytes, 格式: PCM/WAV", audioData.length);

            // 1. 发送音频元数据（先发送元数据，让前端准备接收）
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "tts_audio_meta",
                    "audioSize", audioData.length,
                    "format", "wav", // 火山引擎TTS返回WAV格式
                    "sampleRate", 16000, // 标准采样率
                    "channels", 1, // 单声道
                    "bitDepth", 16, // 16位深度
                    "timestamp", System.currentTimeMillis()
            ))));

            // 2. 发送原始音频数据作为二进制消息（流式音频数据）
            session.sendMessage(new BinaryMessage(audioData));

            logger.info("【TTS输出】音频数据发送完成 - 前端可直接播放");

        } catch (IOException e) {
            logger.error("【TTS输出】发送TTS音频流失败", e);
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

            // 从URI中提取对话ID和用户ID
            String uri = session.getUri().toString();
            String conversationUuid = extractConversationUuid(uri);
            String userId = extractUserId(uri);

            if (conversationUuid != null && userId != null) {
                logger.info("开始处理语音消息，会话: {}, 用户: {}", conversationUuid, userId);

                // 异步处理音频流
                aiStreamingService.processVoiceMessage(conversationUuid, userId, audioSink.asFlux())
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

    private String extractConversationUuid(String uri) {
        // 从URI中提取对话UUID: /ws/chat/{uuid}?userId=1
        try {
            String path = uri.split("\\?")[0]; // 去掉查询参数
            String[] parts = path.split("/");
            if (parts.length >= 3 && "chat".equals(parts[parts.length - 2])) {
                return parts[parts.length - 1]; // 最后一部分是UUID
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
        String text = (String) data.get("text");
        if (text == null || text.trim().isEmpty()) {
            sendErrorMessage(session, "文字内容不能为空");
            return;
        }

        // 从URI中提取对话ID和用户ID
        String uri = session.getUri().toString();
        String conversationUuid = extractConversationUuid(uri);
        String userId = extractUserId(uri);

        if (conversationUuid == null || userId == null) {
            sendErrorMessage(session, "无效的请求URI");
            return;
        }

        logger.info("【文字输入处理】开始处理 - 会话: {}, 用户: {}, 文字内容: '{}'",
                conversationUuid, userId, text);

        try {
            // 参数验证
            UUID.fromString(conversationUuid);
            Long.parseLong(userId);

            // 调用AiStreamingService处理文字消息
            // 这里直接跳过STT步骤，直接使用文字进行LLM+TTS处理
            aiStreamingService.processTextMessage(conversationUuid, userId, text)
                    .subscribe(
                            response -> {
                                try {
                                    String responseType = (String) response.get("type");

                                    if ("llm_chunk".equals(responseType)) {
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

                                    } else if ("tts_audio".equals(responseType)) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> payload = (Map<String, Object>) response.get("payload");
                                        if (payload != null) {
                                            logger.info("【TTS阶段】语音合成完成，音频大小: {} bytes",
                                                    ((byte[]) payload.get("audio_data")).length);

                                            // 发送语音响应
                                            sendTtsAudioResponse(session, payload);
                                        }

                                    } else if ("error".equals(responseType)) {
                                        String errorMessage = (String) response.get("message");
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

        } catch (Exception e) {
            logger.error("【参数错误】UUID或用户ID格式错误: conversationUuid={}, userId={}", conversationUuid, userId);
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