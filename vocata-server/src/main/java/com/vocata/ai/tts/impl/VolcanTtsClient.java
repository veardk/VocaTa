package com.vocata.ai.tts.impl;

import com.vocata.ai.tts.TtsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 火山引擎语音合成服务实现
 * 基于火山引擎TTS API
 * 文档: https://www.volcengine.com/docs/6561/1000027
 */
@Service
public class VolcanTtsClient implements TtsClient {

    private static final Logger logger = LoggerFactory.getLogger(VolcanTtsClient.class);

    @Value("${volcan.tts.access-key:}")
    private String accessKey;

    @Value("${volcan.tts.secret-key:}")
    private String secretKey;

    @Value("${volcan.tts.app-id:}")
    private String appId;

    @Value("${volcan.tts.host:openspeech.bytedance.com}")
    private String host;

    @Value("${volcan.tts.region:ap-beijing-1}")
    private String region;

    @Value("${volcan.tts.service:tts}")
    private String service;

    private final WebClient webClient;

    // 支持的语音列表
    private static final String[] SUPPORTED_VOICES = {
        "zh_female_tianmeixiaotian_moon_bigtts",  // 天美小甜
        "zh_female_huanhuan_moon_bigtts",         // 欢欢
        "zh_male_wennuan_moon_bigtts",            // 温暖
        "zh_female_yangqi_moon_bigtts",           // 阳气
        "zh_female_shuangkuai_moon_bigtts",       // 爽快
        "en_female_bella_moon_bigtts",            // 英文女声Bella
        "en_male_adam_moon_bigtts"                // 英文男声Adam
    };

    public VolcanTtsClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String getProviderName() {
        return "火山引擎TTS";
    }

    @Override
    public boolean isAvailable() {
        return accessKey != null && !accessKey.isEmpty() &&
               secretKey != null && !secretKey.isEmpty();
    }

    @Override
    public String[] getSupportedVoices() {
        return SUPPORTED_VOICES.clone();
    }

    @Override
    public double estimateAudioDuration(String text) {
        // 根据中文字符数和语速估算时长
        // 假设平均语速为每秒3个汉字
        int chineseChars = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();
        int otherChars = text.length() - chineseChars;
        return (chineseChars / 3.0) + (otherChars / 10.0);
    }

    @Override
    public Flux<byte[]> streamSynthesize(Flux<String> textStream, TtsConfig config) {
        if (!isAvailable()) {
            return Flux.error(new RuntimeException("火山引擎TTS服务配置不完整"));
        }

        logger.info("开始火山引擎流式语音合成，语音: {}", config.getVoiceId());

        return textStream
                .buffer(100) // 将文本流缓存为批次
                .concatMap(textList -> {
                    String combinedText = String.join("", textList);
                    if (combinedText.trim().isEmpty()) {
                        return Flux.empty();
                    }
                    return synthesize(combinedText, config)
                            .map(TtsResult::getAudioData)
                            .flux();
                })
                .onErrorResume(error -> {
                    logger.error("火山引擎TTS流式合成失败", error);
                    return Flux.empty();
                });
    }

    @Override
    public Mono<TtsResult> synthesize(String text, TtsConfig config) {
        if (!isAvailable()) {
            return Mono.error(new RuntimeException("火山引擎TTS服务配置不完整"));
        }

        if (text == null || text.trim().isEmpty()) {
            return Mono.error(new RuntimeException("合成文本不能为空"));
        }

        logger.info("开始火山引擎语音合成，文本长度: {}, 语音: {}", text.length(), config.getVoiceId());

        try {
            return callVolcanTtsApi(text, config)
                    .map(response -> parseResponse(response, config))
                    .onErrorResume(error -> {
                        logger.error("火山引擎TTS合成失败", error);
                        TtsResult errorResult = new TtsResult();
                        errorResult.setAudioData(new byte[0]);
                        errorResult.setAudioFormat("error");
                        errorResult.setDurationSeconds(0);

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("provider", "VolcanTTS");
                        metadata.put("error", error.getMessage());
                        errorResult.setMetadata(metadata);

                        return Mono.just(errorResult);
                    });

        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建火山引擎TTS请求失败", e));
        }
    }

    /**
     * 调用火山引擎TTS API
     */
    private Mono<Map<String, Object>> callVolcanTtsApi(String text, TtsConfig config) {
        try {
            // 构建请求参数
            Map<String, Object> requestBody = buildRequestBody(text, config);

            // 构建签名和请求头
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            Map<String, String> headers = buildHeaders(requestBody, timestamp);

            String url = String.format("https://%s/api/v1/tts", host);

            return webClient.post()
                    .uri(url)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::add))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .doOnNext(response -> logger.debug("火山引擎TTS API响应: {}", response));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建火山引擎TTS API请求失败", e));
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String text, TtsConfig config) {
        Map<String, Object> request = new HashMap<>();

        // 应用信息
        Map<String, Object> appInfo = new HashMap<>();
        if (appId != null && !appId.isEmpty()) {
            appInfo.put("appid", appId);
        }
        appInfo.put("cluster", "volcano_tts");
        request.put("app", appInfo);

        // 用户信息
        request.put("user", Map.of("uid", "default_user"));

        // 音频配置
        Map<String, Object> audio = new HashMap<>();
        audio.put("voice_type", getVolcanVoiceId(config.getVoiceId()));
        audio.put("encoding", mapAudioFormat(config.getAudioFormat()));
        audio.put("sample_rate", config.getSampleRate());
        audio.put("speed_ratio", config.getSpeed());
        audio.put("volume_ratio", config.getVolume());
        audio.put("pitch_ratio", config.getPitch());
        request.put("audio", audio);

        // 请求内容
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("text", text);
        reqData.put("text_type", "plain");
        reqData.put("operation", "submit");
        request.put("request", reqData);

        return request;
    }

    /**
     * 构建请求头（包含签名）
     */
    private Map<String, String> buildHeaders(Map<String, Object> body, String timestamp) throws Exception {
        Map<String, String> headers = new HashMap<>();

        // 基本头信息
        headers.put("Content-Type", "application/json");
        headers.put("Host", host);
        headers.put("X-Date", timestamp);

        // 计算签名
        String authorization = calculateSignature(body, timestamp);
        headers.put("Authorization", authorization);

        return headers;
    }

    /**
     * 计算火山引擎签名
     */
    private String calculateSignature(Map<String, Object> body, String timestamp) throws Exception {
        // 构建规范化请求
        String method = "POST";
        String uri = "/api/v1/tts";
        String query = "";

        // 规范化头部
        String canonicalHeaders = String.format("content-type:application/json\nhost:%s\nx-date:%s\n", host, timestamp);
        String signedHeaders = "content-type;host;x-date";

        // 请求体哈希
        String bodyJson = objectToJson(body);
        String hashedPayload = sha256Hex(bodyJson);

        // 构建规范化请求字符串
        String canonicalRequest = String.join("\n", method, uri, query, canonicalHeaders, signedHeaders, hashedPayload);

        // 构建签名字符串
        String credentialScope = String.format("%s/%s/%s/request",
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC")).format(Instant.ofEpochSecond(Long.parseLong(timestamp))),
            region, service);
        String stringToSign = String.join("\n", "AWS4-HMAC-SHA256", timestamp, credentialScope, sha256Hex(canonicalRequest));

        // 计算签名
        byte[] signingKey = getSignatureKey(secretKey,
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC")).format(Instant.ofEpochSecond(Long.parseLong(timestamp))),
            region, service);
        String signature = hmacSha256Hex(stringToSign, signingKey);

        return String.format("AWS4-HMAC-SHA256 Credential=%s/%s, SignedHeaders=%s, Signature=%s",
            accessKey, credentialScope, signedHeaders, signature);
    }

    /**
     * 映射语音ID
     */
    private String getVolcanVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isEmpty()) {
            return "zh_female_tianmeixiaotian_moon_bigtts"; // 默认语音
        }

        // 检查是否是支持的语音
        for (String supportedVoice : SUPPORTED_VOICES) {
            if (supportedVoice.equals(voiceId)) {
                return voiceId;
            }
        }

        return "zh_female_tianmeixiaotian_moon_bigtts"; // 默认语音
    }

    /**
     * 映射音频格式
     */
    private String mapAudioFormat(String format) {
        if (format == null) return "mp3";

        switch (format.toLowerCase()) {
            case "wav":
                return "wav";
            case "mp3":
                return "mp3";
            case "pcm":
                return "pcm";
            default:
                return "mp3";
        }
    }

    /**
     * 解析API响应
     */
    private TtsResult parseResponse(Map<String, Object> response, TtsConfig config) {
        TtsResult result = new TtsResult();

        try {
            Integer code = (Integer) response.get("code");
            if (code != null && code == 0) {
                // 成功响应
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    String audioBase64 = (String) data.get("data");
                    if (audioBase64 != null) {
                        byte[] audioData = java.util.Base64.getDecoder().decode(audioBase64);
                        result.setAudioData(audioData);
                        result.setAudioFormat(config.getAudioFormat());
                        result.setSampleRate(config.getSampleRate());
                        result.setVoiceId(config.getVoiceId());
                        result.setDurationSeconds(estimateAudioDuration(audioData, config.getSampleRate()));

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("provider", "VolcanTTS");
                        metadata.put("voice_id", config.getVoiceId());
                        metadata.put("language", config.getLanguage());
                        result.setMetadata(metadata);

                        logger.info("火山引擎TTS合成成功，音频大小: {} bytes", audioData.length);
                    } else {
                        throw new RuntimeException("API响应中没有音频数据");
                    }
                } else {
                    throw new RuntimeException("API响应数据为空");
                }
            } else {
                String message = (String) response.get("message");
                throw new RuntimeException("API错误: " + code + " - " + message);
            }
        } catch (Exception e) {
            logger.error("解析火山引擎TTS响应失败", e);
            result.setAudioData(new byte[0]);
            result.setAudioFormat("error");
            result.setDurationSeconds(0);
        }

        return result;
    }

    /**
     * 估算音频时长（基于音频数据大小）
     */
    private double estimateAudioDuration(byte[] audioData, int sampleRate) {
        // 简单估算：假设16位单声道
        return (double) audioData.length / (sampleRate * 2);
    }

    // 工具方法
    private String objectToJson(Object obj) {
        try {
            // 简单的JSON序列化，生产环境应使用Jackson
            return obj.toString().replaceAll("=", ":").replaceAll(", ", ",");
        } catch (Exception e) {
            return "{}";
        }
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String hmacSha256Hex(String data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private byte[] hmacSha256(String data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kDate = hmacSha256(dateStamp, ("AWS4" + key).getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSha256(regionName, kDate);
        byte[] kService = hmacSha256(serviceName, kRegion);
        return hmacSha256("aws4_request", kService);
    }
}