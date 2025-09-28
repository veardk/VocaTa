package com.vocata.ai.service;

import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.LlmProvider;
import com.vocata.ai.response.AiStreamingResponse;
import com.vocata.ai.response.SttResult;
import com.vocata.ai.response.LlmResponse;
import com.vocata.ai.service.AiPromptEnhanceService;
import com.vocata.ai.stt.SttClient;
import com.vocata.ai.tts.TtsClient;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.character.service.CharacterChatCountService;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.conversation.constants.ContentType;
import com.vocata.conversation.constants.SenderType;
import com.vocata.conversation.entity.Conversation;
import com.vocata.conversation.entity.Message;
import com.vocata.conversation.mapper.ConversationMapper;
import com.vocata.conversation.mapper.MessageMapper;
import com.vocata.conversation.service.ConversationService;
import com.vocata.file.service.FileService;
import com.vocata.file.dto.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI流式编排服务 - 核心编排服务
 *
 * 实现STT -> LLM -> TTS的完整链路处理
 * 使用响应式编程模式，支持实时数据流处理
 */
@Service
public class AiStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(AiStreamingService.class);

    @Autowired
    private LlmProvider llmProvider;

    @Value("${qiniu.ai.default-model:x-ai/grok-4-fast}")
    private String defaultLlmModel;

    @Autowired
    private SttClient sttClient;

    @Autowired
    private TtsClient ttsClient;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private CharacterMapper characterMapper;

    @Autowired
    private CharacterChatCountService characterChatCountService;

    @Autowired
    private FileService fileService;

    @Autowired
    private AiPromptEnhanceService aiPromptEnhanceService;


    /**
     * 处理音频输入的完整AI对话链路
     * STT -> LLM -> TTS
     *
     * @param conversationUuid 对话UUID
     * @param audioStream 音频数据流
     * @param userId 用户ID
     * @return 包含文本流和音频流的混合响应
     */
    public Flux<AiStreamingResponse> processAudioInput(UUID conversationUuid,
                                                      Flux<byte[]> audioStream,
                                                      Long userId) {
        logger.info("开始处理用户{}的音频输入，对话UUID: {}", userId, conversationUuid);

        return Mono.fromCallable(() -> {
            // 验证对话权限
            if (!conversationService.validateConversationOwnership(conversationUuid, userId)) {
                throw new RuntimeException("无权限访问此对话");
            }
            return conversationService.getConversationByUuid(conversationUuid);
        })
        .flatMapMany(conversation -> {
            // 获取角色信息
            Character character = characterMapper.selectById(conversation.getCharacterId());
            if (character == null) {
                return Flux.error(new RuntimeException("角色不存在"));
            }

            return processAudioWithCharacter(conversation, character, audioStream, userId);
        })
        .doOnError(error -> logger.error("AI流式处理失败", error))
        .onErrorResume(error -> {
            // 返回错误响应
            AiStreamingResponse errorResponse = new AiStreamingResponse();
            errorResponse.setType(AiStreamingResponse.ResponseType.ERROR);
            errorResponse.setError("处理失败: " + error.getMessage());
            return Flux.just(errorResponse);
        });
    }

    /**
     * 使用指定角色处理音频输入
     */
    private Flux<AiStreamingResponse> processAudioWithCharacter(Conversation conversation,
                                                              Character character,
                                                              Flux<byte[]> audioStream,
                                                              Long userId) {
        logger.info("使用角色{}处理音频输入", character.getName());

        // 第一步：STT语音识别
        SttClient.SttConfig sttConfig = new SttClient.SttConfig(character.getLanguage());

        // 共享同一条STT识别流，避免对单播音频流重复订阅
        Flux<SttClient.SttResult> sttFlux = sttClient.streamRecognize(audioStream, sttConfig)
                .replay()
                .autoConnect(1);

        Flux<AiStreamingResponse> streamingStt = sttFlux
                .filter(this::isValidSttResult)
                .doOnNext(sttResult -> logger.debug("STT识别: {}", sttResult.getText()))
                .map(sttResult -> {
                    AiStreamingResponse response = new AiStreamingResponse();
                    response.setType(AiStreamingResponse.ResponseType.STT_RESULT);
                    response.setSttResult(sttResult);
                    return response;
                });

        Flux<AiStreamingResponse> llmAndTts = sttFlux
                .filter(result -> result.isFinal() && isValidSttResult(result))
                .take(1)
                .flatMap(finalSttResult -> processLlmWithTts(conversation, character,
                        finalSttResult.getText(), userId));

        return streamingStt.concatWith(llmAndTts);
    }

    private boolean isValidSttResult(SttClient.SttResult result) {
        if (result == null) {
            return false;
        }
        if (result.getMetadata() != null && result.getMetadata().containsKey("error")) {
            logger.warn("忽略STT错误结果: {}", result.getMetadata().get("error"));
            return false;
        }
        String text = result.getText();
        return text != null && !text.trim().isEmpty();
    }

    /**
     * 处理LLM和TTS链路
     */
    private Flux<AiStreamingResponse> processLlmWithTts(Conversation conversation,
                                                       Character character,
                                                       String userText,
                                                       Long userId) {
        logger.info("开始LLM处理，用户输入: {}", userText);

        // 保存用户消息
        Mono<Message> saveUserMessage = saveMessage(conversation.getId(), userText,
                                                   SenderType.USER, userId)
                .doOnSuccess(msg -> logger.debug("已保存用户消息: {}", msg.getId()));

        // 构建LLM请求
        UnifiedAiRequest llmRequest = buildLlmRequest(conversation, character, userText);

        return saveUserMessage.thenMany(
            llmProvider.streamChat(llmRequest)
                    .replay()
                    .autoConnect(1)
                    .publish(llmFlux -> {
                        Flux<AiStreamingResponse> llmStream = llmFlux
                                .doOnNext(chunk -> logger.debug("LLM响应块: {}", chunk.getContent()))
                                .map(chunk -> {
                                    AiStreamingResponse response = new AiStreamingResponse();
                                    response.setType(AiStreamingResponse.ResponseType.LLM_CHUNK);
                                    response.setLlmChunk(chunk);
                                    return response;
                                });

                        Flux<AiStreamingResponse> ttsStream = llmFlux
                                .filter(chunk -> chunk.getIsFinal() != null && chunk.getIsFinal())
                                .take(1)
                                .flatMap(finalChunk -> processTtsResponse(conversation.getId(),
                                                                        character,
                                                                        finalChunk.getAccumulatedContent(),
                                                                        userId));

                        return llmStream.concatWith(ttsStream);
                    })
        );
    }

    /**
     * 处理TTS响应
     */
    private Flux<AiStreamingResponse> processTtsResponse(Long conversationId,
                                                        Character character,
                                                        String aiText,
                                                        Long userId) {
        logger.info("开始TTS处理，AI回复: {}", aiText);

        // 保存AI消息
        Mono<Message> saveAiMessage = saveMessage(conversationId, aiText,
                                                SenderType.CHARACTER, userId)
                .doOnSuccess(msg -> logger.debug("已保存AI消息: {}", msg.getId()));

        // 配置TTS
        TtsClient.TtsConfig ttsConfig = new TtsClient.TtsConfig(character.getVoiceId(),
                                                               character.getLanguage());

        return saveAiMessage.thenMany(
            ttsClient.streamSynthesizeWithText(Flux.just(aiText), ttsConfig)
                    .doOnNext(ttsResult -> logger.debug("生成TTS结果: {} bytes音频, 文字: {}",
                        ttsResult.getAudioData().length, ttsResult.getCorrespondingText()))
                    .map(ttsResult -> {
                        // 返回包含音频和文字的TTS结果流
                        AiStreamingResponse response = new AiStreamingResponse();
                        response.setType(AiStreamingResponse.ResponseType.TTS_RESULT);
                        response.setTtsResult(ttsResult);
                        // 保留原有的audioData字段以向后兼容
                        response.setAudioData(ttsResult.getAudioData());
                        return response;
                    })
                    .concatWith(Mono.fromCallable(() -> {
                        // 发送完成信号
                        AiStreamingResponse response = new AiStreamingResponse();
                        response.setType(AiStreamingResponse.ResponseType.COMPLETE);
                        return response;
                    }))
        );
    }

    /**
     * 构建LLM请求
     */
    private UnifiedAiRequest buildLlmRequest(Conversation conversation, Character character, String userText) {
        UnifiedAiRequest request = new UnifiedAiRequest();

        // 使用系统级提示词增强构建增强的角色人设
        String enhancedSystemPrompt = aiPromptEnhanceService.buildEnhancedPrompt(character);
        request.setSystemPrompt(enhancedSystemPrompt);

        // 设置用户消息
        request.setUserMessage(userText);

        // 获取历史对话上下文 - 限制查询最近20条消息
        List<Message> recentMessages = messageMapper.findRecentMessagesByConversationId(conversation.getId(), 20);
        Collections.reverse(recentMessages);
        List<UnifiedAiRequest.ChatMessage> contextMessages = new ArrayList<>();

        // 限制上下文长度
        int contextWindow = character.getContextWindow() != null ? character.getContextWindow() : 10;
        int startIndex = Math.max(0, recentMessages.size() - contextWindow);

        for (int i = startIndex; i < recentMessages.size(); i++) {
            Message msg = recentMessages.get(i);
            String role = (msg.getSenderType() == SenderType.USER.getCode()) ? "user" : "assistant";
            contextMessages.add(new UnifiedAiRequest.ChatMessage(role, msg.getTextContent()));
        }

        request.setContextMessages(contextMessages);

        // 设置模型配置
        UnifiedAiRequest.ModelConfig modelConfig = new UnifiedAiRequest.ModelConfig();
        modelConfig.setModelName(defaultLlmModel); // 使用配置的LLM模型
        modelConfig.setTemperature(character.getTemperature() != null ?
                                  character.getTemperature().doubleValue() : 0.7);
        modelConfig.setContextWindow(contextWindow);

        request.setModelConfig(modelConfig);

        return request;
    }

    /**
     * 保存消息到数据库
     */
    private Mono<Message> saveMessage(Long conversationId, String content, SenderType senderType, Long userId) {
        return Mono.fromCallable(() -> {
            Message message = new Message();
            message.setMessageUuid(UUID.randomUUID());
            message.setConversationId(conversationId);
            message.setSenderType(senderType.getCode());
            message.setContentType(ContentType.TEXT.getCode());
            message.setTextContent(content);
            message.setCreateId(userId);
            message.setUpdateId(userId);
            message.setCreateDate(LocalDateTime.now());
            message.setUpdateDate(LocalDateTime.now());

            // 添加处理元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("processing_timestamp", LocalDateTime.now().toString());
            metadata.put("ai_provider", llmProvider.getProviderName());
            metadata.put("stt_provider", sttClient.getProviderName());
            metadata.put("tts_provider", ttsClient.getProviderName());
            message.setMetadata(metadata);

            messageMapper.insert(message);

            // 如果是用户消息，则增加角色聊天计数
            if (senderType == SenderType.USER) {
                try {
                    // 获取对话信息以确定角色ID
                    Conversation conversation = conversationMapper.selectById(conversationId);
                    if (conversation != null && conversation.getCharacterId() != null) {
                        Long newCount = characterChatCountService.incrementChatCount(conversation.getCharacterId());
                        logger.debug("用户{}与角色{}的聊天计数已增加至: {}", userId, conversation.getCharacterId(), newCount);
                    }
                } catch (Exception e) {
                    logger.error("增加角色聊天计数失败，对话ID: {}, 用户ID: {}", conversationId, userId, e);
                    // 不影响主流程，继续执行
                }
            }

            return message;
        });
    }


    /**
     * 实时处理单个音频块 - STT识别
     * 用于WebSocket实时语音处理
     */
    public Mono<SttResult> processAudioChunkToText(String conversationUuid, String userId, byte[] audioData) {
        try {
            UUID uuid = UUID.fromString(conversationUuid);
            Long userIdLong = Long.parseLong(userId);

            // 验证对话权限
            if (!conversationService.validateConversationOwnership(uuid, userIdLong)) {
                return Mono.error(new RuntimeException("无权限访问此对话"));
            }

            Conversation conversation = conversationService.getConversationByUuid(uuid);
            Character character = characterMapper.selectById(conversation.getCharacterId());

            if (character == null) {
                return Mono.error(new RuntimeException("角色不存在"));
            }

            // 配置STT
            SttClient.SttConfig sttConfig = new SttClient.SttConfig(character.getLanguage());

            // 处理单个音频块
            return sttClient.streamRecognize(Flux.just(audioData), sttConfig)
                    .filter(result -> result.getText() != null && !result.getText().trim().isEmpty())
                    .next() // 获取第一个结果
                    .map(sttClientResult -> new SttResult(
                            sttClientResult.getText(),
                            sttClientResult.isFinal(),
                            sttClientResult.getConfidence()
                    ))
                    .doOnNext(result -> logger.debug("音频块STT识别: {}", result.getText()));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("音频块处理失败: " + e.getMessage()));
        }
    }

    /**
     * 处理文本到角色回复 - LLM处理
     */
    public Mono<LlmResponse> processTextToCharacterResponse(String conversationUuid, String userId, String text) {
        try {
            UUID uuid = UUID.fromString(conversationUuid);
            Long userIdLong = Long.parseLong(userId);

            Conversation conversation = conversationService.getConversationByUuid(uuid);
            Character character = characterMapper.selectById(conversation.getCharacterId());

            // 保存用户消息
            saveMessage(conversation.getId(), text, SenderType.USER, userIdLong)
                    .subscribe(msg -> logger.debug("已保存用户消息: {}", msg.getId()));

            // 构建LLM请求
            UnifiedAiRequest llmRequest = buildLlmRequest(conversation, character, text);

            // 调用LLM并收集完整响应
            return llmProvider.streamChat(llmRequest)
                    .reduce("", (accumulated, chunk) -> accumulated + chunk.getContent())
                    .map(fullResponse -> {
                        // 保存AI消息
                        saveMessage(conversation.getId(), fullResponse, SenderType.CHARACTER, userIdLong)
                                .subscribe(msg -> logger.debug("已保存AI消息: {}", msg.getId()));

                        return new LlmResponse(fullResponse, character.getName(), true);
                    })
                    .doOnNext(response -> logger.debug("LLM完整回复: {}", response.getText()));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("LLM处理失败: " + e.getMessage()));
        }
    }

    /**
     * 处理文本到语音 - TTS处理
     */
    public Mono<byte[]> processTextToSpeech(String text) {
        // 使用默认TTS配置
        TtsClient.TtsConfig ttsConfig = new TtsClient.TtsConfig("default", "zh-CN");

        return ttsClient.streamSynthesize(Flux.just(text), ttsConfig)
                .reduce(new byte[0], (accumulated, chunk) -> {
                    byte[] combined = new byte[accumulated.length + chunk.length];
                    System.arraycopy(accumulated, 0, combined, 0, accumulated.length);
                    System.arraycopy(chunk, 0, combined, accumulated.length, chunk.length);
                    return combined;
                })
                .doOnNext(audioData -> logger.debug("TTS生成音频: {} bytes", audioData.length));
    }


    /**
     * WebSocket专用：处理文字消息的完整链路
     * 跳过STT步骤，直接执行 LLM → TTS 处理，返回双重响应（文字流 + 音频流）
     *
     * @param conversationUuidStr 对话UUID字符串（统一使用conversation_uuid）
     * @param userId 用户ID字符串
     * @param textMessage 用户输入的文字消息
     * @return WebSocket格式的响应流（包含文字流和音频流）
     */
    public Flux<Map<String, Object>> processTextMessage(String conversationUuidStr,
                                                        String userId,
                                                        String textMessage) {
        logger.info("【文字消息处理】开始处理 - 对话UUID: {}, 用户: {}, 文字: {}", conversationUuidStr, userId, textMessage);

        try {
            Long userIdLong = Long.parseLong(userId);

            // 统一使用conversation_uuid查询 - 只支持标准UUID格式
            UUID conversationUuid;
            try {
                conversationUuid = UUID.fromString(conversationUuidStr);
                logger.info("使用标准UUID格式查询对话: {}", conversationUuid);
            } catch (IllegalArgumentException e) {
                logger.error("无效的对话UUID格式: {}", conversationUuidStr);
                Map<String, Object> errorResponse = Map.of(
                    "type", "error",
                    "error", "无效的对话UUID格式，请提供标准UUID格式",
                    "timestamp", System.currentTimeMillis()
                );
                return Flux.just(errorResponse);
            }

            Conversation conversation = conversationService.getConversationByUuid(conversationUuid);

            if (conversation == null) {
                logger.error("【错误】未找到对话记录: {}", conversationUuid);
                Map<String, Object> errorResponse = Map.of(
                    "type", "error",
                    "error", "对话不存在",
                    "timestamp", System.currentTimeMillis()
                );
                return Flux.just(errorResponse);
            }

            logger.info("找到对话记录: ID={}, 用户ID={}, 角色ID={}",
                conversation.getId(), conversation.getUserId(), conversation.getCharacterId());

            // 验证对话权限
            if (!conversation.getUserId().equals(userIdLong)) {
                logger.error("【权限错误】用户{}尝试访问用户{}的对话{}",
                    userIdLong, conversation.getUserId(), conversationUuid);
                Map<String, Object> errorResponse = Map.of(
                    "type", "error",
                    "error", "无权限访问此对话，对话属于用户" + conversation.getUserId() + "，当前用户" + userIdLong,
                    "timestamp", System.currentTimeMillis()
                );
                return Flux.just(errorResponse);
            }

            Character character = characterMapper.selectById(conversation.getCharacterId());

            logger.info("角色查询结果: 角色ID={}, 角色对象={}",
                conversation.getCharacterId(), character != null ? character.getName() : "null");

            if (character == null) {
                logger.error("【错误】角色不存在: ID={}", conversation.getCharacterId());
                Map<String, Object> errorResponse = Map.of(
                    "type", "error",
                    "error", "角色不存在，ID: " + conversation.getCharacterId(),
                    "timestamp", System.currentTimeMillis()
                );
                return Flux.just(errorResponse);
            }

            // 创建final引用供lambda使用
            final Conversation finalConversation = conversation;
            final Long finalUserIdLong = userIdLong;

            logger.info("【LLM阶段】开始处理用户文字消息: {}", textMessage);

            // 保存用户消息
            saveMessage(finalConversation.getId(), textMessage, SenderType.USER, finalUserIdLong)
                .subscribe(msg -> logger.debug("已保存用户文字消息: {}", msg.getId()));

            // 构建LLM请求
            UnifiedAiRequest llmRequest = buildLlmRequest(finalConversation, character, textMessage);

            // 收集完整的LLM响应用于TTS
            StringBuilder fullResponseBuilder = new StringBuilder();

            return llmProvider.streamChat(llmRequest)
                .doOnNext(chunk -> {
                    logger.debug("【LLM阶段】收到文字流块: {}", chunk.getContent());
                    fullResponseBuilder.append(chunk.getContent());
                })
                .map(chunk -> {
                    // 实时返回文字流
                    Map<String, Object> textResponse = new HashMap<>();
                    textResponse.put("type", "text_chunk");
                    textResponse.put("timestamp", System.currentTimeMillis());
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("text", chunk.getContent());
                    payload.put("accumulated_text", chunk.getAccumulatedContent());
                    payload.put("is_final", chunk.getIsFinal());
                    payload.put("character_name", character.getName());
                    textResponse.put("payload", payload);
                    return textResponse;
                })
                .concatWith(
                    // LLM完成后，处理TTS
                    Mono.fromCallable(() -> fullResponseBuilder.toString())
                        .filter(fullText -> !fullText.trim().isEmpty())
                        .doOnNext(fullText -> {
                            logger.info("【TTS阶段】开始处理完整回复: {}", fullText);
                            // 保存AI消息
                            saveMessage(finalConversation.getId(), fullText, SenderType.CHARACTER, finalUserIdLong)
                                .subscribe(msg -> logger.debug("已保存AI回复消息: {}", msg.getId()));
                        })
                        .flatMapMany(fullText -> {
                            // TTS流式处理 - 正确的架构
                            TtsClient.TtsConfig ttsConfig = new TtsClient.TtsConfig(
                                character.getVoiceId(), character.getLanguage());

                            logger.info("【TTS阶段】开始流式语音合成，语音ID: {}", character.getVoiceId());

                            // 直接返回TTS音频流，不收集不上传
                            return ttsClient.streamSynthesize(Flux.just(fullText), ttsConfig)
                                .doOnNext(audioData -> logger.debug("【TTS阶段】生成音频块: {} bytes", audioData.length))
                                .map(audioData -> {
                                    Map<String, Object> audioResponse = new HashMap<>();
                                    audioResponse.put("type", "audio_chunk");
                                    audioResponse.put("timestamp", System.currentTimeMillis());
                                    audioResponse.put("audio_data", audioData);
                                    return audioResponse;
                                })
                                .doOnComplete(() -> {
                                    logger.info("【TTS阶段】流式语音合成完成");
                                })
                                .concatWith(Mono.fromCallable(() -> {
                                    // 发送音频完成标志
                                    Map<String, Object> completeResponse = new HashMap<>();
                                    completeResponse.put("type", "audio_complete");
                                    completeResponse.put("timestamp", System.currentTimeMillis());
                                    return completeResponse;
                                }));
                        })
                )
                .concatWith(Mono.fromCallable(() -> {
                    // 发送最终完成信号
                    Map<String, Object> finalCompleteResponse = new HashMap<>();
                    finalCompleteResponse.put("type", "complete");
                    finalCompleteResponse.put("timestamp", System.currentTimeMillis());
                    finalCompleteResponse.put("message", "处理完成");
                    logger.info("【处理完成】文字消息处理链路完成");
                    return finalCompleteResponse;
                }))
                .onErrorResume(error -> {
                    logger.error("文字消息处理失败", error);
                    Map<String, Object> errorResponse = Map.of(
                        "type", "error",
                        "error", error.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    );
                    return Flux.just(errorResponse);
                });

        } catch (Exception e) {
            logger.error("文字消息参数解析失败", e);
            Map<String, Object> errorResponse = Map.of(
                "type", "error",
                "error", "无效的参数: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return Flux.just(errorResponse);
        }
    }

    /**
     * WebSocket专用：处理语音消息的完整链路
     * 接收音频流，执行STT → LLM → TTS处理，返回WebSocket格式的响应
     *
     * @param conversationUuid 对话UUID字符串
     * @param userId 用户ID字符串
     * @param audioStream 音频数据流
     * @return WebSocket格式的响应流
     */
    public Flux<Map<String, Object>> processVoiceMessage(String conversationUuid,
                                                         String userId,
                                                         Flux<byte[]> audioStream) {
        logger.info("WebSocket处理语音消息，对话: {}, 用户: {}", conversationUuid, userId);

        try {
            UUID uuid = UUID.fromString(conversationUuid);
            Long userIdLong = Long.parseLong(userId);

            return processAudioInput(uuid, audioStream, userIdLong)
                    .map(this::convertToWebSocketResponse)
                    .onErrorResume(error -> {
                        logger.error("语音处理失败", error);
                        Map<String, Object> errorResponse = Map.of(
                            "type", "error",
                            "error", error.getMessage(),
                            "timestamp", System.currentTimeMillis()
                        );
                        return Flux.just(errorResponse);
                    });
        } catch (Exception e) {
            logger.error("参数解析失败", e);
            Map<String, Object> errorResponse = Map.of(
                "type", "error",
                "error", "无效的参数: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return Flux.just(errorResponse);
        }
    }

    /**
     * 将内部AI响应转换为WebSocket响应格式
     */
    private Map<String, Object> convertToWebSocketResponse(AiStreamingResponse response) {
        Map<String, Object> webSocketResponse = new HashMap<>();
        webSocketResponse.put("timestamp", System.currentTimeMillis());

        switch (response.getType()) {
            case STT_RESULT:
                webSocketResponse.put("type", "stt_result");
                Map<String, Object> sttPayload = new HashMap<>();
                sttPayload.put("text", response.getSttResult().getText());
                sttPayload.put("confidence", response.getSttResult().getConfidence());
                sttPayload.put("is_final", response.getSttResult().isFinal());
                webSocketResponse.put("payload", sttPayload);
                break;

            case LLM_CHUNK:
                webSocketResponse.put("type", "llm_chunk");
                Map<String, Object> llmPayload = new HashMap<>();
                llmPayload.put("text", response.getLlmChunk().getContent());
                llmPayload.put("accumulated_text", response.getLlmChunk().getAccumulatedContent());
                llmPayload.put("is_final", response.getLlmChunk().getIsFinal());
                webSocketResponse.put("payload", llmPayload);
                break;

            case AUDIO_CHUNK:
                webSocketResponse.put("type", "audio_chunk");
                webSocketResponse.put("audio_data", response.getAudioData());
                break;

            case TTS_RESULT:
                webSocketResponse.put("type", "tts_result");
                if (response.getTtsResult() != null) {
                    Map<String, Object> ttsResultMap = new HashMap<>();
                    ttsResultMap.put("audioData", response.getTtsResult().getAudioData());
                    ttsResultMap.put("correspondingText", response.getTtsResult().getCorrespondingText());
                    ttsResultMap.put("audioFormat", response.getTtsResult().getAudioFormat());
                    ttsResultMap.put("sampleRate", response.getTtsResult().getSampleRate());
                    ttsResultMap.put("voiceId", response.getTtsResult().getVoiceId());
                    ttsResultMap.put("startTime", response.getTtsResult().getStartTime());
                    ttsResultMap.put("endTime", response.getTtsResult().getEndTime());
                    webSocketResponse.put("tts_result", ttsResultMap);
                }
                break;

            case ERROR:
                webSocketResponse.put("type", "error");
                webSocketResponse.put("error", response.getError());
                break;

            case COMPLETE:
                webSocketResponse.put("type", "complete");
                webSocketResponse.put("message", "处理完成");
                break;
        }

        return webSocketResponse;
    }

}
