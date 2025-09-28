package com.vocata.ai.tts.impl;

import com.vocata.ai.tts.TtsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.websocket.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 科大讯飞TTS客户端
 *
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

    // 支持的语音列表
    private static final String[] SUPPORTED_VOICES = {
        "xiaoyan",              // 小燕（女声，推荐）
        "xiaoyu",               // 小宇（男声）
        "xiaoxue",              // 小雪（女声）
        "xiaofeng",             // 小峰（男声）
        "xiaoqian",             // 小倩（女声）
        "xiaolin",              // 小琳（女声）
        "xiaomeng",             // 小萌（女声）
        "xiaojing",             // 小静（女声）
        "xiaokun",              // 小坤（男声）
        "xiaoqiang",            // 小强（男声）
        "vixf",                 // 小峰（粤语）
        "vixm",                 // 小美（粤语）
        "catherine",            // 英文女声
        "henry",                // 英文男声
        "x4_xiaoyan",           // 讯飞小燕（普通话）
        "x4_yezi",              // 讯飞叶子（普通话）
        "aisjiuxu",             // 讯飞许久（普通话）
        "aisjinger",            // 讯飞小静（普通话）
        "aisbabyxu"             // 讯飞许小宝（普通话）
    };

    @Override
    public String getProviderName() {
        return "科大讯飞TTS WebSocket API";
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

        String actualVoiceId = getXunfeiVoiceId(config.getVoiceId());
        logger.info("开始科大讯飞流式语音合成（包含文字），输入音色: {} -> 实际使用: {}",
                   config.getVoiceId(), actualVoiceId);

        return textStream
                .bufferTimeout(3, java.time.Duration.ofMillis(300))
                .concatMap(textList -> {
                    String combinedText = String.join("", textList);
                    if (combinedText.trim().isEmpty()) {
                        return Flux.empty();
                    }
                    return synthesizeSingleTextWithResult(combinedText, config);
                })
                .onErrorResume(error -> {
                    logger.error("科大讯飞TTS流式合成失败", error);
                    return Flux.empty();
                });
    }

    @Override
    public Flux<byte[]> streamSynthesize(Flux<String> textStream, TtsConfig config) {
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

        String actualVoiceId = getXunfeiVoiceId(config.getVoiceId());
        logger.info("开始科大讯飞WebSocket TTS合成，文本长度: {}, 输入音色: {} -> 实际使用: {}",
                   text.length(), config.getVoiceId(), actualVoiceId);

        return Mono.fromCallable(() -> {
            try {
                return callXunfeiTtsApi(text, actualVoiceId, config);
            } catch (Exception e) {
                logger.error("科大讯飞WebSocket TTS合成失败", e);
                throw new RuntimeException("TTS合成失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 流式合成单个文本片段，同时返回文字和音频
     */
    private Flux<TtsResult> synthesizeSingleTextWithResult(String text, TtsConfig config) {
        logger.info("开始科大讯飞WebSocket流式TTS合成 - 文本: '{}', 长度: {} 字符",
                   text.length() > 30 ? text.substring(0, 30) + "..." : text,
                   text.length());

        return Mono.fromCallable(() -> {
            try {
                String actualVoiceId = getXunfeiVoiceId(config.getVoiceId());
                TtsResult result = callXunfeiTtsApi(text, actualVoiceId, config);
                result.setCorrespondingText(text);
                result.setStartTime(System.currentTimeMillis());

                logger.info("科大讯飞WebSocket流式TTS合成完成 - 文本: '{}', 音频大小: {} bytes",
                           text.length() > 30 ? text.substring(0, 30) + "..." : text,
                           result.getAudioData().length);

                return result;
            } catch (Exception e) {
                logger.error("科大讯飞WebSocket流式TTS合成失败: {}", e.getMessage(), e);
                throw new RuntimeException("TTS合成失败: " + e.getMessage(), e);
            }
        }).flux();
    }

    /**
     * 获取科大讯飞音色ID
     */
    private String getXunfeiVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isEmpty()) {
            return "x4_lingxiaoyu_emo"; // 默认小燕
        }

        // 检查是否是科大讯飞支持的音色
        for (String supportedVoice : SUPPORTED_VOICES) {
            if (supportedVoice.equals(voiceId)) {
                return voiceId;
            }
        }

        // 如果传入的音色ID不被支持，使用默认音色
        logger.warn("不支持的音色ID: {}，使用默认音色: xiaoyan", voiceId);
        return "x4_lingxiaoyu_emo";
    }

    /**
     * 映射音频格式为科大讯飞API支持的格式
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
     * 调用科大讯飞WebSocket TTS API
     */
    private TtsResult callXunfeiTtsApi(String text, String voiceId, TtsConfig config) throws Exception {
        String wsUrl = getWebSocketAuthUrl();
        logger.info("连接科大讯飞WebSocket TTS API: {}", wsUrl);

        ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean isComplete = new AtomicBoolean(false);

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        Session wsSession = container.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                logger.info("WebSocket连接已建立");
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        try {
                            handleTtsResponse(message, audioStream, isComplete, latch);
                        } catch (Exception e) {
                            logger.error("处理TTS响应失败", e);
                            errorRef.set(e);
                            latch.countDown();
                        }
                    }
                });

                try {
                    sendTtsRequest(session, text, voiceId, config);
                } catch (Exception e) {
                    logger.error("发送TTS请求失败", e);
                    errorRef.set(e);
                    latch.countDown();
                }
            }

            @Override
            public void onError(Session session, Throwable throwable) {
                logger.error("WebSocket连接错误", throwable);
                errorRef.set(new Exception(throwable));
                latch.countDown();
            }
        }, ClientEndpointConfig.Builder.create().build(), new URI(wsUrl));

        boolean finished = latch.await(30, TimeUnit.SECONDS);

        if (wsSession.isOpen()) {
            wsSession.close();
        }

        if (!finished) {
            throw new RuntimeException("TTS合成超时");
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }

        byte[] audioData = audioStream.toByteArray();

        if (audioData.length == 0) {
            throw new RuntimeException("未收到音频数据");
        }

        TtsResult result = new TtsResult();
        result.setAudioData(audioData);
        result.setAudioFormat(config.getAudioFormat());
        result.setSampleRate(config.getSampleRate());
        result.setVoiceId(voiceId);
        result.setDurationSeconds(estimateAudioDuration(text));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "XunfeiTTS-WebSocket-API");
        metadata.put("voice_id", voiceId);
        metadata.put("language", config.getLanguage());
        metadata.put("audioSize", audioData.length);
        result.setMetadata(metadata);

        logger.info("科大讯飞WebSocket TTS合成完成，音频大小: {} bytes", audioData.length);
        return result;
    }

    /**
     * 处理TTS响应消息
     */
    private void handleTtsResponse(String message, ByteArrayOutputStream audioStream,
                                   AtomicBoolean isComplete, CountDownLatch latch) throws Exception {
        Map<String, Object> response = objectMapper.readValue(message, Map.class);

        Integer code = (Integer) response.get("code");
        if (code != null && code != 0) {
            String errorMsg = (String) response.get("message");
            throw new RuntimeException("TTS API错误: " + code + " - " + errorMsg);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data != null) {
            String audioBase64 = (String) data.get("audio");
            if (audioBase64 != null && !audioBase64.isEmpty()) {
                byte[] audioChunk = Base64.getDecoder().decode(audioBase64);
                audioStream.write(audioChunk);
                logger.debug("收到音频数据块: {} bytes", audioChunk.length);
            }

            Integer status = (Integer) data.get("status");
            if (status != null && status == 2) {
                logger.info("音频数据接收完成");
                isComplete.set(true);
                latch.countDown();
            }
        }
    }

    /**
     * 发送TTS请求
     */
    private void sendTtsRequest(Session session, String text, String voiceId, TtsConfig config) throws Exception {
        Map<String, Object> request = new HashMap<>();

        Map<String, Object> common = new HashMap<>();
        common.put("app_id", appId);
        request.put("common", common);

        Map<String, Object> business = new HashMap<>();
        business.put("aue", mapAudioFormat(config.getAudioFormat()));
        business.put("vcn", voiceId);
        business.put("speed", (int)(config.getSpeed() * 50));
        business.put("volume", (int)(config.getVolume() * 100));
        business.put("pitch", (int)(config.getPitch() * 50));
        business.put("tte", "UTF8");
        request.put("business", business);

        Map<String, Object> data = new HashMap<>();
        data.put("status", 2);
        data.put("text", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        request.put("data", data);

        String requestJson = objectMapper.writeValueAsString(request);
        session.getBasicRemote().sendText(requestJson);
        logger.info("已发送TTS请求，文本长度: {} 字符", text.length());
    }

    /**
     * 生成WebSocket认证URL
     */
    private String getWebSocketAuthUrl() throws Exception {
        URL url = new URL("https://" + host + path);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        String signatureOrigin = "host: " + host + "\n" +
                                "date: " + date + "\n" +
                                "GET " + path + " HTTP/1.1";

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                                            apiKey, "hmac-sha256", "host date request-line", signature);

        String authBase64 = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

        return String.format("wss://%s%s?authorization=%s&date=%s&host=%s",
                           host, path,
                           java.net.URLEncoder.encode(authBase64, "UTF-8"),
                           java.net.URLEncoder.encode(date, "UTF-8"),
                           host);
    }
}