package com.vocata.ai.service;

import com.vocata.ai.stt.SttClient;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.conversation.entity.Conversation;
import com.vocata.conversation.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * STT测试专用服务
 * 简化版AI服务，专门用于测试音频 -> STT -> 文字转换
 *
 * 功能：
 * 1. 接收WebSocket音频流
 * 2. 通过STT服务转换为文字
 * 3. 将识别结果输出到控制台
 * 4. 返回WebSocket响应给前端
 */
@Service
public class SttTestService {

    private static final Logger logger = LoggerFactory.getLogger(SttTestService.class);

    @Autowired
    private SttClient sttClient;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private CharacterMapper characterMapper;

    /**
     * 处理WebSocket音频流的STT转换
     * 音频 -> STT -> 文字输出到控制台
     *
     * @param conversationUuid 对话UUID字符串
     * @param userId 用户ID字符串
     * @param audioStream 音频数据流
     * @return WebSocket格式的STT响应流
     */
    public Flux<Map<String, Object>> processAudioToText(String conversationUuid,
                                                        String userId,
                                                        Flux<byte[]> audioStream) {
        logger.info("🎤【STT测试】开始处理音频转文字 - 对话UUID: {}, 用户ID: {}", conversationUuid, userId);

        try {
            UUID uuid = UUID.fromString(conversationUuid);
            Long userIdLong = Long.parseLong(userId);

            // 验证对话权限
            if (!conversationService.validateConversationOwnership(uuid, userIdLong)) {
                logger.error("🎤【STT测试】权限验证失败 - 用户{}无权访问对话{}", userIdLong, uuid);
                Map<String, Object> errorResponse = createErrorResponse("无权限访问此对话");
                return Flux.just(errorResponse);
            }

            Conversation conversation = conversationService.getConversationByUuid(uuid);
            if (conversation == null) {
                logger.error("🎤【STT测试】对话不存在 - UUID: {}", uuid);
                Map<String, Object> errorResponse = createErrorResponse("对话不存在");
                return Flux.just(errorResponse);
            }

            // 获取角色信息用于STT配置
            Character character = characterMapper.selectById(conversation.getCharacterId());
            if (character == null) {
                logger.error("🎤【STT测试】角色不存在 - ID: {}", conversation.getCharacterId());
                Map<String, Object> errorResponse = createErrorResponse("角色不存在");
                return Flux.just(errorResponse);
            }

            logger.info("🎤【STT测试】开始STT识别 - 角色: {}, 语言: {}", character.getName(), character.getLanguage());

            // 配置STT - 使用OGG格式提高兼容性
            SttClient.SttConfig sttConfig = new SttClient.SttConfig(character.getLanguage());
            sttConfig.setAudioFormat("ogg"); // 使用OGG格式，比webm兼容性更好

            return processAudioStreamWithStt(audioStream, sttConfig, character.getName());

        } catch (Exception e) {
            logger.error("🎤【STT测试】参数解析失败", e);
            Map<String, Object> errorResponse = createErrorResponse("无效的参数: " + e.getMessage());
            return Flux.just(errorResponse);
        }
    }

    /**
     * 核心STT处理逻辑
     */
    private Flux<Map<String, Object>> processAudioStreamWithStt(Flux<byte[]> audioStream,
                                                               SttClient.SttConfig sttConfig,
                                                               String characterName) {
        return sttClient.streamRecognize(audioStream, sttConfig)
                .filter(sttResult -> sttResult.getText() != null && !sttResult.getText().trim().isEmpty())
                .doOnNext(sttResult -> {
                    // 🎯 核心功能：将STT识别结果输出到控制台
                    String recognizedText = sttResult.getText();
                    double confidence = sttResult.getConfidence();
                    boolean isFinal = sttResult.isFinal();

                    // 控制台输出格式化日志
                    System.out.println("========================================");
                    System.out.println("🎤 STT识别结果:");
                    System.out.println("📝 识别文字: " + recognizedText);
                    System.out.println("📊 置信度: " + String.format("%.2f", confidence));
                    System.out.println("✅ 是否最终: " + (isFinal ? "是" : "否"));
                    System.out.println("🎭 角色: " + characterName);
                    System.out.println("⏰ 时间: " + java.time.LocalDateTime.now());
                    System.out.println("========================================");

                    // 同时记录到日志文件
                    logger.info("🎤【STT识别结果】文字: '{}', 置信度: {}, 最终: {}, 角色: {}",
                               recognizedText, confidence, isFinal, characterName);
                })
                .map(sttResult -> {
                    // 返回WebSocket响应给前端
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "stt_result");
                    response.put("timestamp", System.currentTimeMillis());

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("text", sttResult.getText());
                    payload.put("confidence", sttResult.getConfidence());
                    payload.put("is_final", sttResult.isFinal());
                    payload.put("character_name", characterName);

                    response.put("payload", payload);
                    return response;
                })
                .concatWith(
                    // 发送完成信号
                    Flux.just(createCompleteResponse("STT处理完成"))
                )
                .onErrorResume(error -> {
                    logger.error("🎤【STT测试】STT处理失败", error);
                    System.err.println("❌ STT处理错误: " + error.getMessage());
                    Map<String, Object> errorResponse = createErrorResponse("STT处理失败: " + error.getMessage());
                    return Flux.just(errorResponse);
                });
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("timestamp", System.currentTimeMillis());
        response.put("error", errorMessage);
        return response;
    }

    /**
     * 创建完成响应
     */
    private Map<String, Object> createCompleteResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "complete");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", message);
        return response;
    }

    /**
     * 单独测试STT功能 - 不依赖对话和角色
     * 用于纯STT功能测试
     */
    public Flux<Map<String, Object>> testSttOnly(Flux<byte[]> audioStream, String language) {
        logger.info("🎤【纯STT测试】开始测试，语言: {}", language);

        SttClient.SttConfig sttConfig = new SttClient.SttConfig(language != null ? language : "zh-CN");
        sttConfig.setAudioFormat("ogg"); // 使用OGG格式，比webm兼容性更好

        return sttClient.streamRecognize(audioStream, sttConfig)
                .filter(sttResult -> sttResult.getText() != null && !sttResult.getText().trim().isEmpty())
                .doOnNext(sttResult -> {
                    // 控制台输出
                    System.out.println("========================================");
                    System.out.println("🎤 纯STT测试结果:");
                    System.out.println("📝 识别文字: " + sttResult.getText());
                    System.out.println("📊 置信度: " + String.format("%.2f", sttResult.getConfidence()));
                    System.out.println("✅ 是否最终: " + (sttResult.isFinal() ? "是" : "否"));
                    System.out.println("🌐 语言: " + language);
                    System.out.println("⏰ 时间: " + java.time.LocalDateTime.now());
                    System.out.println("========================================");

                    logger.info("🎤【纯STT测试】识别: '{}', 置信度: {}",
                               sttResult.getText(), sttResult.getConfidence());
                })
                .map(sttResult -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "stt_test_result");
                    response.put("timestamp", System.currentTimeMillis());

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("text", sttResult.getText());
                    payload.put("confidence", sttResult.getConfidence());
                    payload.put("is_final", sttResult.isFinal());
                    payload.put("language", language);

                    response.put("payload", payload);
                    return response;
                })
                .concatWith(
                    Flux.just(createCompleteResponse("纯STT测试完成"))
                )
                .onErrorResume(error -> {
                    logger.error("🎤【纯STT测试】失败", error);
                    System.err.println("❌ 纯STT测试错误: " + error.getMessage());
                    return Flux.just(createErrorResponse("纯STT测试失败: " + error.getMessage()));
                });
    }
}