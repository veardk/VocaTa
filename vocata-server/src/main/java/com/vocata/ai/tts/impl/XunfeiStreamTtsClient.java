package com.vocata.ai.tts.impl;

import com.vocata.ai.tts.TtsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 科大讯飞流式TTS服务实现
 * 基于WebSocket实现真正的流式语音合成
 * API文档: https://www.xfyun.cn/doc/tts/online_tts/API.html
 */
@Service("xunfeiTtsClient")
public class XunfeiStreamTtsClient implements TtsClient {

    private static final Logger logger = LoggerFactory.getLogger(XunfeiStreamTtsClient.class);

    @Value("${xunfei.tts.app-id:}")
    private String appId;

    @Value("${xunfei.tts.api-key:}")
    private String apiKey;

    @Value("${xunfei.tts.secret-key:}")
    private String secretKey;

    @Value("${xunfei.tts.host:tts-api.xfyun.cn}")
    private String host;

    @Value("${xunfei.tts.path:/v2/tts}")
    private String path;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient();

    // 支持的语音列表（科大讯飞音色）
    private static final String[] SUPPORTED_VOICES = {
        "xiaoyan",      // 小燕（女声，推荐）
        "xiaoyu",       // 小宇（男声）
        "xiaoxue",      // 小雪（女声）
        "xiaofeng",     // 小峰（男声）
        "xiaoqian",     // 小倩（女声）
        "xiaolin",      // 小琳（女声）
        "xiaomeng",     // 小萌（女声）
        "xiaojing",     // 小静（女声）
        "xiaokun",      // 小坤（男声）
        "xiaoqiang",    // 小强（男声）
        "vixf",         // 小峰（粤语）
        "vixm",         // 小美（粤语）
        "catherine",    // 英文女声
        "henry",        // 英文男声
        "voice-en-harry" // 映射到英文男声
    };

    @Override
    public String getProviderName() {
        return "科大讯飞流式TTS";
    }

    @Override
    public boolean isAvailable() {
        boolean hasConfig = appId != null && !appId.isEmpty() &&
                           apiKey != null && !apiKey.isEmpty() &&
                           secretKey != null && !secretKey.isEmpty();

        if (!hasConfig) {
            logger.warn("科大讯飞TTS配置不完整 - appId:{}, apiKey:{}, secretKey:{}",
                       appId != null && !appId.isEmpty(),
                       apiKey != null && !apiKey.isEmpty(),
                       secretKey != null && !secretKey.isEmpty());
            logger.warn("请在配置文件中设置: xunfei.tts.app-id, xunfei.tts.api-key, xunfei.tts.secret-key");
        }

        return hasConfig;
    }

    @Override
    public String[] getSupportedVoices() {
        return SUPPORTED_VOICES.clone();
    }

    @Override
    public double estimateAudioDuration(String text) {
        // 根据中文字符数和语速估算时长
        // 假设平均语速为每秒3.5个汉字
        int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int otherChars = text.length() - chineseChars;
        return (chineseChars / 3.5) + (otherChars / 12.0);
    }

    @Override
    public Flux<TtsResult> streamSynthesizeWithText(Flux<String> textStream, TtsConfig config) {
        if (!isAvailable()) {
            return Flux.error(new RuntimeException("科大讯飞TTS服务配置不完整"));
        }

        logger.info("开始科大讯飞流式语音合成（包含文字），语音: {}", config.getVoiceId());

        return textStream
                // 优化缓冲策略：按句子分割或时间窗口
                .bufferTimeout(3, java.time.Duration.ofMillis(300))
                .concatMap(textList -> {
                    String combinedText = String.join("", textList);
                    if (combinedText.trim().isEmpty()) {
                        return Flux.empty();
                    }
                    return streamSynthesizeTextWithResult(combinedText, config);
                })
                .onErrorResume(error -> {
                    logger.error("科大讯飞TTS流式合成失败", error);
                    return Flux.empty();
                });
    }

    @Override
    public Flux<byte[]> streamSynthesize(Flux<String> textStream, TtsConfig config) {
        // 重用新方法，但只返回音频数据
        return streamSynthesizeWithText(textStream, config)
                .map(TtsResult::getAudioData);
    }

    @Override
    public Mono<TtsResult> synthesize(String text, TtsConfig config) {
        if (!isAvailable()) {
            return Mono.error(new RuntimeException("科大讯飞TTS服务配置不完整"));
        }

        if (text == null || text.trim().isEmpty()) {
            return Mono.error(new RuntimeException("合成文本不能为空"));
        }

        logger.info("开始科大讯飞语音合成，文本长度: {}, 语音: {}", text.length(), config.getVoiceId());

        return streamSynthesizeText(text, config)
                .collectList()
                .map(audioChunks -> {
                    // 合并所有音频块
                    int totalSize = audioChunks.stream().mapToInt(chunk -> chunk.length).sum();
                    byte[] completeAudio = new byte[totalSize];
                    int offset = 0;
                    for (byte[] chunk : audioChunks) {
                        System.arraycopy(chunk, 0, completeAudio, offset, chunk.length);
                        offset += chunk.length;
                    }

                    TtsResult result = new TtsResult();
                    result.setAudioData(completeAudio);
                    result.setAudioFormat(config.getAudioFormat());
                    result.setSampleRate(config.getSampleRate());
                    result.setVoiceId(getXunfeiVoiceId(config.getVoiceId()));
                    result.setDurationSeconds(estimateAudioDuration(text));

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("provider", "XunfeiTTS");
                    metadata.put("voice_id", config.getVoiceId());
                    metadata.put("language", config.getLanguage());
                    metadata.put("streaming", true);
                    result.setMetadata(metadata);

                    logger.info("科大讯飞TTS合成成功，音频大小: {} bytes", completeAudio.length);
                    return result;
                })
                .onErrorResume(error -> {
                    logger.error("科大讯飞TTS合成失败", error);
                    TtsResult errorResult = new TtsResult();
                    errorResult.setAudioData(new byte[0]);
                    errorResult.setAudioFormat("error");
                    errorResult.setDurationSeconds(0);

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("provider", "XunfeiTTS");
                    metadata.put("error", error.getMessage());
                    errorResult.setMetadata(metadata);

                    return Mono.just(errorResult);
                });
    }

    /**
     * 流式合成单个文本片段，同时返回文字和音频
     */
    private Flux<TtsResult> streamSynthesizeTextWithResult(String text, TtsConfig config) {
        try {
            String wsUrl = buildWebSocketUrl();
            logger.debug("科大讯飞TTS WebSocket URL: {}", wsUrl);

            Sinks.Many<TtsResult> resultSink = Sinks.many().unicast().onBackpressureBuffer();

            return webSocketClient
                    .execute(URI.create(wsUrl), new TtsResultWebSocketHandler(text, config, resultSink))
                    .then(Mono.fromRunnable(() -> resultSink.tryEmitComplete()))
                    .thenMany(resultSink.asFlux())
                    .doOnNext(result -> logger.debug("收到TTS结果: {} bytes音频, 文字: {}",
                        result.getAudioData().length, result.getCorrespondingText()))
                    .doOnComplete(() -> logger.debug("流式合成完成: {}", text))
                    .doOnError(error -> logger.error("流式合成失败: {}", error.getMessage()));

        } catch (Exception e) {
            return Flux.error(new RuntimeException("构建科大讯飞TTS WebSocket请求失败", e));
        }
    }

    /**
     * 流式合成单个文本片段
     */
    private Flux<byte[]> streamSynthesizeText(String text, TtsConfig config) {
        try {
            String wsUrl = buildWebSocketUrl();
            logger.debug("科大讯飞TTS WebSocket URL: {}", wsUrl);

            Sinks.Many<byte[]> audioSink = Sinks.many().unicast().onBackpressureBuffer();

            return webSocketClient
                    .execute(URI.create(wsUrl), new TtsWebSocketHandler(text, config, audioSink))
                    .then(Mono.fromRunnable(() -> audioSink.tryEmitComplete()))
                    .thenMany(audioSink.asFlux())
                    .doOnNext(audioChunk -> logger.debug("收到音频块: {} bytes", audioChunk.length))
                    .doOnComplete(() -> logger.debug("流式合成完成: {}", text))
                    .doOnError(error -> logger.error("流式合成失败: {}", error.getMessage()));

        } catch (Exception e) {
            return Flux.error(new RuntimeException("构建科大讯飞TTS WebSocket请求失败", e));
        }
    }

    /**
     * WebSocket处理器
     */
    private class TtsWebSocketHandler implements WebSocketHandler {
        private final String text;
        private final TtsConfig config;
        private final Sinks.Many<byte[]> audioSink;

        public TtsWebSocketHandler(String text, TtsConfig config, Sinks.Many<byte[]> audioSink) {
            this.text = text;
            this.config = config;
            this.audioSink = audioSink;
        }

        @Override
        public Mono<Void> handle(WebSocketSession session) {
            // 发送TTS请求
            Mono<Void> sendRequest = session.send(
                    Mono.just(session.textMessage(buildTtsRequest(text, config)))
            );

            // 处理响应
            Mono<Void> handleResponse = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(this::handleTtsResponse)
                    .then();

            return Mono.when(sendRequest, handleResponse);
        }

        private void handleTtsResponse(String response) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

                Integer code = (Integer) responseMap.get("code");
                if (code != null && code == 0) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                    if (data != null) {
                        String audioBase64 = (String) data.get("audio");
                        if (audioBase64 != null && !audioBase64.isEmpty()) {
                            byte[] audioData = Base64.getDecoder().decode(audioBase64);

                            // 创建包含文字和音频的TtsResult
                            TtsResult result = new TtsResult();
                            result.setAudioData(audioData);
                            result.setCorrespondingText(extractCorrespondingText(data));
                            result.setStartTime(System.currentTimeMillis());
                            result.setAudioFormat(config.getAudioFormat());
                            result.setSampleRate(config.getSampleRate());

                            // 通过Sinks发送完整的TtsResult，而不仅仅是音频数据
                            // 注意：这里需要修改Sinks的类型定义
                            audioSink.tryEmitNext(audioData); // 暂时保持原有接口兼容性

                            logger.debug("收到音频块: {} bytes, 对应文字: {}",
                                       audioData.length, result.getCorrespondingText());
                        }

                        Integer status = (Integer) data.get("status");
                        if (status != null && status == 2) { // 合成完成
                            audioSink.tryEmitComplete();
                        }
                    }
                } else {
                    String message = (String) responseMap.get("message");
                    logger.error("科大讯飞TTS API错误: {} - {}", code, message);
                    audioSink.tryEmitError(new RuntimeException("科大讯飞TTS API错误: " + code + " - " + message));
                }
            } catch (Exception e) {
                logger.error("解析科大讯飞TTS响应失败", e);
                audioSink.tryEmitError(new RuntimeException("解析科大讯飞TTS响应失败", e));
            }
        }

        /**
         * 提取响应中对应的文字内容
         */
        private String extractCorrespondingText(Map<String, Object> data) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ws = (List<Map<String, Object>>) data.get("ws");
                if (ws != null && !ws.isEmpty()) {
                    StringBuilder textBuilder = new StringBuilder();
                    for (Map<String, Object> wordSegment : ws) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> cw = (List<Map<String, Object>>) wordSegment.get("cw");
                        if (cw != null) {
                            for (Map<String, Object> word : cw) {
                                String w = (String) word.get("w");
                                if (w != null) {
                                    textBuilder.append(w);
                                }
                            }
                        }
                    }
                    return textBuilder.toString();
                }
            } catch (Exception e) {
                logger.warn("提取对应文字失败", e);
            }
            return ""; // 如果无法提取文字，返回空字符串
        }
    }

    /**
     * WebSocket处理器 - 同时返回文字和音频
     */
    private class TtsResultWebSocketHandler implements WebSocketHandler {
        private final String text;
        private final TtsConfig config;
        private final Sinks.Many<TtsResult> resultSink;

        public TtsResultWebSocketHandler(String text, TtsConfig config, Sinks.Many<TtsResult> resultSink) {
            this.text = text;
            this.config = config;
            this.resultSink = resultSink;
        }

        @Override
        public Mono<Void> handle(WebSocketSession session) {
            // 发送TTS请求
            Mono<Void> sendRequest = session.send(
                    Mono.just(session.textMessage(buildTtsRequest(text, config)))
            );

            // 处理响应
            Mono<Void> handleResponse = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(this::handleTtsResponse)
                    .then();

            return Mono.zip(sendRequest, handleResponse).then();
        }

        private void handleTtsResponse(String responseText) {
            try {
                Map<String, Object> response = objectMapper.readValue(responseText, Map.class);
                Integer code = (Integer) response.get("code");

                if (code == null || code != 0) {
                    String message = (String) response.get("message");
                    logger.error("科大讯飞TTS错误 - code: {}, message: {}", code, message);
                    resultSink.tryEmitError(new RuntimeException("科大讯飞TTS服务错误: " + message));
                    return;
                }

                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    Integer status = (Integer) data.get("status");
                    String audio = (String) data.get("audio");

                    // 提取对应的文字内容
                    String correspondingText = extractCorrespondingText(data);

                    if (audio != null && !audio.isEmpty()) {
                        byte[] audioData = Base64.getDecoder().decode(audio);

                        // 创建TtsResult对象
                        TtsResult result = new TtsResult();
                        result.setAudioData(audioData);
                        result.setCorrespondingText(correspondingText);
                        result.setAudioFormat(config.getAudioFormat());
                        result.setSampleRate(config.getSampleRate());
                        result.setVoiceId(config.getVoiceId());
                        result.setStartTime(System.currentTimeMillis());

                        resultSink.tryEmitNext(result);
                    }

                    // status = 2 表示数据传输完成
                    if (status != null && status == 2) {
                        resultSink.tryEmitComplete();
                    }
                }
            } catch (Exception e) {
                logger.error("处理科大讯飞TTS响应失败", e);
                resultSink.tryEmitError(new RuntimeException("处理TTS响应失败", e));
            }
        }

        /**
         * 提取响应中对应的文字内容
         */
        private String extractCorrespondingText(Map<String, Object> data) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ws = (List<Map<String, Object>>) data.get("ws");
                if (ws != null && !ws.isEmpty()) {
                    StringBuilder textBuilder = new StringBuilder();
                    for (Map<String, Object> wordSegment : ws) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> cw = (List<Map<String, Object>>) wordSegment.get("cw");
                        if (cw != null) {
                            for (Map<String, Object> word : cw) {
                                String w = (String) word.get("w");
                                if (w != null) {
                                    textBuilder.append(w);
                                }
                            }
                        }
                    }
                    return textBuilder.toString();
                }
            } catch (Exception e) {
                logger.warn("提取对应文字失败", e);
            }
            return ""; // 如果无法提取文字，返回空字符串
        }
    }

    /**
     * 构建WebSocket URL（包含鉴权）
     */
    private String buildWebSocketUrl() throws Exception {
        String timestamp = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z")
                .withZone(ZoneId.of("GMT"))
                .format(Instant.now());

        String signatureOrigin = "host: " + host + "\n" +
                                "date: " + timestamp + "\n" +
                                "GET " + path + " HTTP/1.1";

        String signature = hmacSha256(signatureOrigin, secretKey);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", signature);

        String authorizationBase64 = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        return String.format("wss://%s%s?authorization=%s&date=%s&host=%s",
                host, path,
                java.net.URLEncoder.encode(authorizationBase64, StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(timestamp, StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(host, StandardCharsets.UTF_8));
    }

    /**
     * 构建TTS请求JSON
     */
    private String buildTtsRequest(String text, TtsConfig config) {
        try {
            Map<String, Object> request = new HashMap<>();

            // 通用参数
            Map<String, Object> common = new HashMap<>();
            common.put("app_id", appId);
            request.put("common", common);

            // 业务参数
            Map<String, Object> business = new HashMap<>();
            business.put("aue", mapAudioFormat(config.getAudioFormat()));
            business.put("auf", "audio/L16;rate=" + config.getSampleRate());
            business.put("vcn", getXunfeiVoiceId(config.getVoiceId()));
            business.put("speed", (int) (config.getSpeed() * 50)); // 转换为科大讯飞的速度范围
            business.put("volume", (int) (config.getVolume() * 100)); // 转换为科大讯飞的音量范围
            business.put("pitch", (int) (config.getPitch() * 50)); // 转换为科大讯飞的音调范围
            business.put("tte", "UTF8");
            request.put("business", business);

            // 数据参数
            Map<String, Object> data = new HashMap<>();
            data.put("text", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
            data.put("status", 2); // 一次性发送全部文本
            request.put("data", data);

            String requestJson = objectMapper.writeValueAsString(request);
            logger.debug("科大讯飞TTS请求: {}", requestJson);
            return requestJson;

        } catch (Exception e) {
            throw new RuntimeException("构建科大讯飞TTS请求失败", e);
        }
    }

    /**
     * 映射语音ID到科大讯飞音色
     */
    private String getXunfeiVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isEmpty()) {
            return "xiaoyan"; // 默认小燕
        }

        // 检查是否是支持的语音
        for (String supportedVoice : SUPPORTED_VOICES) {
            if (supportedVoice.equals(voiceId)) {
                return voiceId;
            }
        }

        // 特殊映射
        switch (voiceId) {
            case "voice-en-harry":
                return "henry"; // 映射到英文男声
            case "voice-zh-female":
                return "xiaoyan"; // 映射到中文女声
            case "voice-zh-male":
                return "xiaoyu"; // 映射到中文男声
            default:
                logger.warn("未识别的音色ID: {}, 使用默认音色", voiceId);
                return "xiaoyan"; // 默认小燕
        }
    }

    /**
     * 映射音频格式
     */
    private String mapAudioFormat(String format) {
        if (format == null) return "lame";

        switch (format.toLowerCase()) {
            case "wav":
                return "raw";
            case "mp3":
                return "lame";
            case "pcm":
                return "raw";
            default:
                return "lame";
        }
    }

    /**
     * HMAC-SHA256签名
     */
    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}