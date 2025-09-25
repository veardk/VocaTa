package com.vocata.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.ai.service.AiStreamingService;
import com.vocata.common.utils.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI聊天WebSocket处理器
 *
 * 处理实时语音对话的核心通道：
 * - 接收客户端音频流（二进制消息）
 * - 接收控制消息（文本消息）
 * - 发送LLM文本流和TTS音频流
 */
@Component
public class AiChatWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AiChatWebSocketHandler.class);

    @Autowired
    private AiStreamingService aiStreamingService;

    @Autowired
    private ObjectMapper objectMapper;

    // 管理活跃的WebSocket会话
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    // 管理音频数据流的Sink
    private final Map<String, Sinks.Many<byte[]>> audioSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        logger.info("WebSocket连接建立: {}", sessionId);

        // 从URI中提取conversation_uuid
        String path = session.getHandshakeInfo().getUri().getPath();
        String conversationUuid = extractConversationUuid(path);

        if (conversationUuid == null) {
            logger.error("无法从路径提取conversation_uuid: {}", path);
            return session.close();
        }

        logger.info("连接到对话: {}", conversationUuid);

        // 注册会话
        activeSessions.put(sessionId, session);

        // 创建音频数据流的Sink
        Sinks.Many<byte[]> audioSink = Sinks.many().multicast().onBackpressureBuffer();
        audioSinks.put(sessionId, audioSink);

        // 处理接收到的消息
        Flux<WebSocketMessage> receiveMessages = session.receive()
                .doOnNext(message -> handleIncomingMessage(session, message, conversationUuid, audioSink))
                .doOnError(error -> logger.error("处理接收消息时出错: {}", error.getMessage()))
                .onErrorResume(error -> Flux.empty());

        // 处理发送的消息（AI响应）
        Flux<WebSocketMessage> sendMessages = createOutgoingMessageStream(session, conversationUuid, audioSink);

        // 清理资源
        Mono<Void> cleanup = Mono.fromRunnable(() -> {
            logger.info("清理WebSocket会话: {}", sessionId);
            activeSessions.remove(sessionId);
            audioSinks.remove(sessionId);
        });

        // 合并接收和发送流
        return Mono.zip(
                receiveMessages.then(),
                sendMessages.then()
        ).then(cleanup);
    }

    /**
     * 处理收到的消息
     */
    private void handleIncomingMessage(WebSocketSession session, WebSocketMessage message,
                                     String conversationUuid, Sinks.Many<byte[]> audioSink) {
        try {
            if (message.getType() == WebSocketMessage.Type.BINARY) {
                // 处理音频数据
                handleAudioData(message, audioSink);
            } else if (message.getType() == WebSocketMessage.Type.TEXT) {
                // 处理控制消息
                handleControlMessage(session, message.getPayloadAsText(), conversationUuid, audioSink);
            }
        } catch (Exception e) {
            logger.error("处理消息时出错: {}", e.getMessage(), e);
            sendErrorMessage(session, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理音频数据
     */
    private void handleAudioData(WebSocketMessage message, Sinks.Many<byte[]> audioSink) {
        byte[] audioData = new byte[message.getPayload().readableByteCount()];
        message.getPayload().read(audioData);

        logger.debug("收到音频数据: {} bytes", audioData.length);

        // 发送到音频流
        Sinks.EmitResult result = audioSink.tryEmitNext(audioData);
        if (result.isFailure()) {
            logger.warn("音频数据发送失败: {}", result);
        }
    }

    /**
     * 处理控制消息（JSON格式）
     */
    private void handleControlMessage(WebSocketSession session, String messageText,
                                    String conversationUuid, Sinks.Many<byte[]> audioSink) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> controlMessage = objectMapper.readValue(messageText, Map.class);
            String messageType = (String) controlMessage.get("type");

            logger.debug("收到控制消息: {}", messageType);

            switch (messageType) {
                case "audio_start":
                    handleAudioStart(session, controlMessage);
                    break;
                case "audio_end":
                    handleAudioEnd(session, conversationUuid, audioSink);
                    break;
                case "ping":
                    handlePing(session);
                    break;
                default:
                    logger.warn("未知的控制消息类型: {}", messageType);
            }
        } catch (Exception e) {
            logger.error("解析控制消息失败: {}", e.getMessage());
            sendErrorMessage(session, "控制消息格式错误");
        }
    }

    /**
     * 处理音频开始
     */
    private void handleAudioStart(WebSocketSession session, Map<String, Object> message) {
        logger.info("开始音频录制");

        // 发送确认消息
        sendStatusMessage(session, "recording_started", "开始录制");
    }

    /**
     * 处理音频结束 - 触发AI处理链路
     */
    private void handleAudioEnd(WebSocketSession session, String conversationUuid,
                              Sinks.Many<byte[]> audioSink) {
        logger.info("结束音频录制，开始AI处理");

        // 完成音频流
        audioSink.tryEmitComplete();

        // 发送处理开始状态
        sendStatusMessage(session, "processing_started", "开始AI处理");
    }

    /**
     * 处理心跳
     */
    private void handlePing(WebSocketSession session) {
        sendControlMessage(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
    }

    /**
     * 创建输出消息流
     */
    private Flux<WebSocketMessage> createOutgoingMessageStream(WebSocketSession session,
                                                             String conversationUuid,
                                                             Sinks.Many<byte[]> audioSink) {
        UUID uuid = UUID.fromString(conversationUuid);
        Long userId = getUserIdFromSession(session);

        if (userId == null) {
            logger.error("无法获取用户ID");
            return Flux.just(session.textMessage("{\"type\":\"error\",\"message\":\"未认证用户\"}"));
        }

        // 创建音频流并处理AI响应
        Flux<byte[]> audioStream = audioSink.asFlux();

        return aiStreamingService.processAudioInput(uuid, audioStream, userId)
                .flatMap(response -> convertToWebSocketMessage(session, response))
                .doOnError(error -> logger.error("AI处理出错: {}", error.getMessage()))
                .onErrorResume(error -> {
                    String errorMsg = String.format("{\"type\":\"error\",\"message\":\"%s\"}", error.getMessage());
                    return Flux.just(session.textMessage(errorMsg));
                });
    }

    /**
     * 将AI响应转换为WebSocket消息
     */
    private Flux<WebSocketMessage> convertToWebSocketMessage(WebSocketSession session,
                                                           AiStreamingService.AiStreamingResponse response) {
        try {
            switch (response.getType()) {
                case STT_RESULT:
                    // STT识别结果
                    Map<String, Object> sttMessage = Map.of(
                        "type", "stt_result",
                        "payload", Map.of(
                            "text", response.getSttResult().getText(),
                            "confidence", response.getSttResult().getConfidence(),
                            "is_final", response.getSttResult().isFinal()
                        )
                    );
                    return Flux.just(session.textMessage(objectMapper.writeValueAsString(sttMessage)));

                case LLM_CHUNK:
                    // LLM文本流
                    Map<String, Object> llmMessage = Map.of(
                        "type", "llm_chunk",
                        "payload", Map.of(
                            "text", response.getLlmChunk().getContent() != null ? response.getLlmChunk().getContent() : "",
                            "accumulated_text", response.getLlmChunk().getAccumulatedContent() != null ?
                                              response.getLlmChunk().getAccumulatedContent() : "",
                            "is_final", response.getLlmChunk().getIsFinal() != null ? response.getLlmChunk().getIsFinal() : false
                        )
                    );
                    return Flux.just(session.textMessage(objectMapper.writeValueAsString(llmMessage)));

                case AUDIO_CHUNK:
                    // TTS音频流
                    return Flux.just(session.binaryMessage(factory -> factory.wrap(response.getAudioData())));

                case COMPLETE:
                    // 完成信号
                    Map<String, Object> completeMessage = Map.of("type", "complete", "message", "处理完成");
                    return Flux.just(session.textMessage(objectMapper.writeValueAsString(completeMessage)));

                case ERROR:
                    // 错误信息
                    Map<String, Object> errorMessage = Map.of("type", "error", "message", response.getError());
                    return Flux.just(session.textMessage(objectMapper.writeValueAsString(errorMessage)));

                default:
                    return Flux.empty();
            }
        } catch (Exception e) {
            logger.error("转换WebSocket消息失败: {}", e.getMessage());
            String errorMsg = String.format("{\"type\":\"error\",\"message\":\"消息转换失败: %s\"}", e.getMessage());
            return Flux.just(session.textMessage(errorMsg));
        }
    }

    /**
     * 发送状态消息
     */
    private void sendStatusMessage(WebSocketSession session, String status, String message) {
        Map<String, Object> statusMessage = Map.of(
            "type", "status",
            "status", status,
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        sendControlMessage(session, statusMessage);
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String error) {
        Map<String, Object> errorMessage = Map.of(
            "type", "error",
            "message", error,
            "timestamp", System.currentTimeMillis()
        );
        sendControlMessage(session, errorMessage);
    }

    /**
     * 发送控制消息
     */
    private void sendControlMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.send(Mono.just(session.textMessage(jsonMessage))).subscribe();
        } catch (Exception e) {
            logger.error("发送控制消息失败: {}", e.getMessage());
        }
    }

    /**
     * 从路径中提取conversation_uuid
     */
    private String extractConversationUuid(String path) {
        // 路径格式: /ws/chat/{conversation_uuid}
        String[] parts = path.split("/");
        if (parts.length >= 4 && "ws".equals(parts[1]) && "chat".equals(parts[2])) {
            return parts[3];
        }
        return null;
    }

    /**
     * 从会话中获取用户ID
     * TODO: 实现JWT token验证
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            // 临时实现：从查询参数中获取用户ID
            String query = session.getHandshakeInfo().getUri().getQuery();
            if (query != null && query.contains("userId=")) {
                String userId = query.split("userId=")[1].split("&")[0];
                return Long.parseLong(userId);
            }

            // 实际实现中应该从JWT token中解析用户ID
            // return UserContext.getUserId();

            return 1L; // 临时返回测试用户ID
        } catch (Exception e) {
            logger.error("获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }
}