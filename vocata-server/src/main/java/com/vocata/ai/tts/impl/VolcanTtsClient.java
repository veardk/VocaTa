package com.vocata.ai.tts.impl;

import com.vocata.ai.tts.TtsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 */
@Service
public class VolcanTtsClient implements TtsClient {

    private static final Logger logger = LoggerFactory.getLogger(VolcanTtsClient.class);

    @Value("${volcan.tts.access-key:}")
    private String accessKey;

    @Value("${volcan.tts.secret-key:}")
    private String secretKey;

    @Value("${volcan.tts.access-token:}")
    private String accessToken;

    @Value("${volcan.tts.app-id:}")
    private String appId;

    @Value("${volcan.tts.cluster:volcano_tts}")
    private String cluster;

    @Value("${volcan.tts.user-id:default_user}")
    private String userId;

    @Value("${volcan.tts.host:openspeech.bytedance.com}")
    private String host;

    @Value("${volcan.tts.region:ap-beijing-1}")
    private String region;

    @Value("${volcan.tts.service:tts}")
    private String service;

    private final WebClient webClient;


    private final ObjectMapper objectMapper = new ObjectMapper();
    // 支持的语音列表（基于2024年火山引擎最新音色）
    private static final String[] SUPPORTED_VOICES = {
        // 通用场景音色
        "BV001_streaming",              // 通用女声
        "BV002_streaming",              // 通用男声
        "BV034_streaming",              // 清甜女声
        "BV033_streaming",              // 温暖男声

        // 多情感音色
        "BV700_streaming",              // 小萝莉
        "BV701_streaming",              // 温柔女声
        "BV702_streaming",              // 清脆男声

        // 角色扮演音色（2024年新增）
        "BV158_streaming",              // 奶气萌娃
        "BV159_streaming",              // 病弱少女
        "BV160_streaming",              // 傲娇霸总
        "BV161_streaming",              // 温柔学姐

        // 趣味口音
        "BV119_streaming",              // 东北话
        "BV120_streaming",              // 四川话
        "BV121_streaming",              // 粤语

        // 英文音色
        "en_female_bella_moon_bigtts",  // 英文女声Bella
        "en_male_adam_moon_bigtts",     // 英文男声Adam

        // 经典音色（兼容）
        "zh_female_tianmeixiaotian_moon_bigtts",  // 天美小甜
        "zh_female_huanhuan_moon_bigtts",         // 欢欢
        "zh_male_wennuan_moon_bigtts",            // 温暖
        "zh_female_yangqi_moon_bigtts",           // 阳气
        "zh_female_shuangkuai_moon_bigtts"        // 爽快
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
        // 检查必需配置参数
        boolean hasBasicConfig = appId != null && !appId.isEmpty();

        // 火山引擎TTS使用Bearer Token认证
        boolean hasTokenAuth = accessToken != null && !accessToken.isEmpty();

        boolean isConfigured = hasBasicConfig && hasTokenAuth;

        if (!isConfigured) {
            logger.warn("火山引擎TTS配置不完整 - appId:{}, hasTokenAuth:{}",
                       appId != null && !appId.isEmpty(), hasTokenAuth);
            logger.warn("请在配置文件中设置: volcan.tts.app-id 和 volcan.tts.access-token");
        }

        return isConfigured;
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
            return Flux.error(new RuntimeException("火山引擎TTS服务配置不完整：需要app-id和access-token"));
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
            return Mono.error(new RuntimeException("火山引擎TTS服务配置不完整：需要app-id和access-token"));
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
            logger.debug("火山引擎TTS请求参数: {}", objectToJson(requestBody));

            // 构建签名和请求头
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            Map<String, String> headers = buildHeaders(requestBody, timestamp);
            logger.debug("火山引擎TTS请求头: {}", headers);

            String url = String.format("https://%s/api/v1/tts", host);

            return webClient.post()
                    .uri(url)
                    .headers(httpHeaders -> {
                        headers.forEach(httpHeaders::add);
                        // 不设置Host头，让WebClient自动处理
                    })
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        return response.bodyToMono(String.class)
                                .map(errorBody -> {
                                    logger.error("火山引擎TTS API错误响应: Status={}, Body={}", response.statusCode(), errorBody);
                                    return new RuntimeException("火山引擎TTS API错误: " + response.statusCode() + ", " + errorBody);
                                });
                    })
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .doOnNext(response -> logger.debug("火山引擎TTS API响应: {}", response))
                    .doOnError(error -> logger.error("火山引擎TTS API调用失败: {}", error.getMessage()));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建火山引擎TTS API请求失败", e));
        }
    }

    /**
     * 构建请求体 - 基于2024年火山引擎TTS API格式
     */
    private Map<String, Object> buildRequestBody(String text, TtsConfig config) {
        Map<String, Object> request = new HashMap<>();

        // 应用信息
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("appid", appId);
        appInfo.put("cluster", cluster != null ? cluster : "volcano_tts");
        request.put("app", appInfo);

        // 用户信息
        Map<String, Object> user = new HashMap<>();
        user.put("uid", userId != null ? userId : "default_user");
        request.put("user", user);

        // 音频配置
        Map<String, Object> audio = new HashMap<>();
        String mappedVoiceId = getVolcanVoiceId(config.getVoiceId());
        audio.put("voice_type", mappedVoiceId);
        audio.put("encoding", mapAudioFormat(config.getAudioFormat()));
        audio.put("sample_rate", config.getSampleRate());
        audio.put("speed_ratio", config.getSpeed());
        audio.put("volume_ratio", config.getVolume());
        audio.put("pitch_ratio", config.getPitch());

        logger.info("使用音色: {} -> {}", config.getVoiceId(), mappedVoiceId);
        request.put("audio", audio);

        // 请求内容
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("text", text);
        reqData.put("text_type", "plain");
        reqData.put("operation", "query");  // 修改为query
        // 添加必需的reqid字段
        reqData.put("reqid", java.util.UUID.randomUUID().toString());
        request.put("request", reqData);

        return request;
    }

    /**
     * 构建请求头（支持Bearer Token认证）
     */
    private Map<String, String> buildHeaders(Map<String, Object> body, String timestamp) throws Exception {
        Map<String, String> headers = new HashMap<>();

        // 基本头信息
        headers.put("Content-Type", "application/json");
        // 移除固定的Resource-Id，让服务端自动识别

        // 优先使用Access Token认证（推荐方式）
        if (accessToken != null && !accessToken.isEmpty()) {
            headers.put("Authorization", "Bearer; " + accessToken);
            logger.debug("使用Bearer Token认证方式");
        } else {
            throw new RuntimeException("缺少有效的认证信息：需要access-token");
        }

        return headers;
    }

    /**
     * 计算火山引擎签名 - 移除Host头避免WebClient限制
     */
    private String calculateSignature(Map<String, Object> body, String timestamp) throws Exception {
        // 构建规范化请求
        String method = "POST";
        String uri = "/api/v1/tts";
        String query = "";

        // 规范化头部（移除Host头，避免WebClient限制）
        String canonicalHeaders = String.format("content-type:application/json\nx-date:%s\n", timestamp);
        String signedHeaders = "content-type;x-date";

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
     * 映射语音ID - 支持最新的火山引擎音色
     */
    private String getVolcanVoiceId(String voiceId) {
        if (voiceId == null || voiceId.isEmpty()) {
            return "BV001_streaming"; // 默认通用女声
        }

        // 检查是否是支持的语音
        for (String supportedVoice : SUPPORTED_VOICES) {
            if (supportedVoice.equals(voiceId)) {
                return voiceId;
            }
        }

        // 如果输入的是老版本音色ID，映射到新版本
        switch (voiceId) {
            case "tianmeixiaotian":
                return "zh_female_tianmeixiaotian_moon_bigtts";
            case "huanhuan":
                return "zh_female_huanhuan_moon_bigtts";
            case "wennuan":
                return "zh_male_wennuan_moon_bigtts";
            case "yangqi":
                return "zh_female_yangqi_moon_bigtts";
            case "shuangkuai":
                return "zh_female_shuangkuai_moon_bigtts";
            case "voice-en-harry":
                return "en_male_adam_moon_bigtts";  // 映射到英文男声
            default:
                logger.warn("未识别的音色ID: {}, 使用默认音色", voiceId);
                return "BV001_streaming"; // 默认通用女声
        }
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
            // 使用Jackson进行正确的JSON序列化
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("JSON序列化失败", e);
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