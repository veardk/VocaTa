package com.vocata.ai.service;

import com.vocata.ai.dto.UnifiedAiRequest;
import com.vocata.ai.dto.UnifiedAiStreamChunk;
import com.vocata.ai.llm.LlmProvider;
import com.vocata.ai.stt.SttClient;
import com.vocata.ai.tts.TtsClient;
import com.vocata.character.entity.Character;
import com.vocata.character.mapper.CharacterMapper;
import com.vocata.conversation.constants.ContentType;
import com.vocata.conversation.constants.SenderType;
import com.vocata.conversation.entity.Conversation;
import com.vocata.conversation.entity.Message;
import com.vocata.conversation.mapper.ConversationMapper;
import com.vocata.conversation.mapper.MessageMapper;
import com.vocata.conversation.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

        return sttClient.streamRecognize(audioStream, sttConfig)
                .filter(sttResult -> sttResult.getText() != null && !sttResult.getText().trim().isEmpty())
                .doOnNext(sttResult -> logger.debug("STT识别: {}", sttResult.getText()))
                .map(sttResult -> {
                    // 发送STT结果
                    AiStreamingResponse response = new AiStreamingResponse();
                    response.setType(AiStreamingResponse.ResponseType.STT_RESULT);
                    response.setSttResult(sttResult);
                    return response;
                })
                .concatWith(
                    // 第二步：收集完整的STT结果并调用LLM
                    sttClient.streamRecognize(audioStream, sttConfig)
                            .filter(SttClient.SttResult::isFinal)
                            .take(1)
                            .flatMap(finalSttResult -> processLlmWithTts(conversation, character,
                                                                       finalSttResult.getText(), userId))
                );
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
                    .doOnNext(chunk -> logger.debug("LLM响应块: {}", chunk.getContent()))
                    .map(chunk -> {
                        // 转换LLM响应为流式响应
                        AiStreamingResponse response = new AiStreamingResponse();
                        response.setType(AiStreamingResponse.ResponseType.LLM_CHUNK);
                        response.setLlmChunk(chunk);
                        return response;
                    })
                    .concatWith(
                        // 第三步：收集完整的LLM响应并调用TTS
                        llmProvider.streamChat(llmRequest)
                                .filter(chunk -> chunk.getIsFinal() != null && chunk.getIsFinal())
                                .take(1)
                                .flatMap(finalChunk -> processTtsResponse(conversation.getId(),
                                                                        character,
                                                                        finalChunk.getAccumulatedContent(),
                                                                        userId))
                    )
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
            ttsClient.streamSynthesize(Flux.just(aiText), ttsConfig)
                    .doOnNext(audioData -> logger.debug("生成音频数据: {} bytes", audioData.length))
                    .map(audioData -> {
                        // 返回音频流
                        AiStreamingResponse response = new AiStreamingResponse();
                        response.setType(AiStreamingResponse.ResponseType.AUDIO_CHUNK);
                        response.setAudioData(audioData);
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

        // 设置系统提示词（角色人设）
        request.setSystemPrompt(character.getPersona());

        // 设置用户消息
        request.setUserMessage(userText);

        // 获取历史对话上下文
        List<Message> recentMessages = messageMapper.findByConversationIdOrderByCreateDateAsc(conversation.getId());
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
        modelConfig.setModelName("gpt-3.5-turbo"); // 默认模型
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

            // 添加处理元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("processing_timestamp", LocalDateTime.now().toString());
            metadata.put("ai_provider", llmProvider.getProviderName());
            metadata.put("stt_provider", sttClient.getProviderName());
            metadata.put("tts_provider", ttsClient.getProviderName());
            message.setMetadata(metadata);

            messageMapper.insert(message);
            return message;
        });
    }

    /**
     * AI流式响应封装类
     */
    public static class AiStreamingResponse {
        private ResponseType type;
        private SttClient.SttResult sttResult;
        private UnifiedAiStreamChunk llmChunk;
        private byte[] audioData;
        private String error;
        private Map<String, Object> metadata;

        public enum ResponseType {
            STT_RESULT,    // STT识别结果
            LLM_CHUNK,     // LLM文本流块
            AUDIO_CHUNK,   // TTS音频流块
            ERROR,         // 错误信息
            COMPLETE       // 完成信号
        }

        // Getters and Setters
        public ResponseType getType() {
            return type;
        }

        public void setType(ResponseType type) {
            this.type = type;
        }

        public SttClient.SttResult getSttResult() {
            return sttResult;
        }

        public void setSttResult(SttClient.SttResult sttResult) {
            this.sttResult = sttResult;
        }

        public UnifiedAiStreamChunk getLlmChunk() {
            return llmChunk;
        }

        public void setLlmChunk(UnifiedAiStreamChunk llmChunk) {
            this.llmChunk = llmChunk;
        }

        public byte[] getAudioData() {
            return audioData;
        }

        public void setAudioData(byte[] audioData) {
            this.audioData = audioData;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}