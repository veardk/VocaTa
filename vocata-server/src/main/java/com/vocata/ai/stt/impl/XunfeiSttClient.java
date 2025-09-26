package com.vocata.ai.stt.impl;

import com.vocata.ai.stt.SttClient;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 科大讯飞语音识别服务实现
 * 基于科大讯飞实时语音转写API
 * 文档: https://www.xfyun.cn/doc/asr/voicedictation/API.html
 */
@Service
public class XunfeiSttClient implements SttClient {

    private static final Logger logger = LoggerFactory.getLogger(XunfeiSttClient.class);

    @Value("${xunfei.stt.app-id:}")
    private String appId;

    @Value("${xunfei.stt.api-key:}")
    private String apiKey;

    @Value("${xunfei.stt.secret-key:}")
    private String secretKey;

    @Value("${xunfei.stt.host:iat-api.xfyun.cn}")
    private String host;

    @Value("${xunfei.stt.path:/v2/iat}")
    private String path;

    private final WebClient webClient;

    public XunfeiSttClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String getProviderName() {
        return "科大讯飞STT";
    }

    @Override
    public boolean isAvailable() {
        // 检查必要的配置参数
        return appId != null && !appId.isEmpty() &&
               apiKey != null && !apiKey.isEmpty() &&
               secretKey != null && !secretKey.isEmpty();
    }

    @Override
    public Flux<SttResult> streamRecognize(Flux<byte[]> audioStream, SttConfig config) {
        if (!isAvailable()) {
            return Flux.error(new RuntimeException("科大讯飞STT服务配置不完整"));
        }

        logger.info("开始科大讯飞流式语音识别，语言: {}", config.getLanguage());

        // 对于流式识别，我们先收集音频数据然后批量处理
        return audioStream
                .reduce(new StringBuilder(), (acc, bytes) -> {
                    // 收集所有音频数据
                    acc.append(Base64.getEncoder().encodeToString(bytes));
                    return acc;
                })
                .flatMapMany(audioData -> {
                    // 将收集的音频数据发送到科大讯飞API
                    return callXunfeiSttApi(audioData.toString(), config)
                            .flatMapMany(response -> parseStreamResponse(response, config));
                })
                .onErrorResume(error -> {
                    logger.error("科大讯飞STT流式识别失败", error);
                    // 返回错误结果
                    SttResult errorResult = new SttResult();
                    errorResult.setText("[识别失败: " + error.getMessage() + "]");
                    errorResult.setConfidence(0.0);
                    errorResult.setFinal(true);
                    return Flux.just(errorResult);
                });
    }

    @Override
    public Mono<SttResult> recognize(byte[] audioData, SttConfig config) {
        if (!isAvailable()) {
            return Mono.error(new RuntimeException("科大讯飞STT服务配置不完整"));
        }

        logger.info("开始科大讯飞批量语音识别，数据大小: {} bytes", audioData.length);

        String base64Audio = Base64.getEncoder().encodeToString(audioData);

        return callXunfeiSttApi(base64Audio, config)
                .map(response -> parseBatchResponse(response, config))
                .onErrorResume(error -> {
                    logger.error("科大讯飞STT批量识别失败", error);
                    SttResult errorResult = new SttResult();
                    errorResult.setText("[识别失败: " + error.getMessage() + "]");
                    errorResult.setConfidence(0.0);
                    errorResult.setFinal(true);
                    return Mono.just(errorResult);
                });
    }

    /**
     * 调用科大讯飞STT API
     */
    private Mono<Map<String, Object>> callXunfeiSttApi(String base64Audio, SttConfig config) {
        try {
            // 构建请求URL和认证信息
            String url = buildAuthenticatedUrl();

            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(base64Audio, config);

            return webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .doOnNext(response -> logger.debug("科大讯飞API响应: {}", response));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建科大讯飞API请求失败", e));
        }
    }

    /**
     * 构建带认证信息的URL
     */
    private String buildAuthenticatedUrl() throws Exception {
        // 生成RFC1123格式的时间戳
        String date = ZonedDateTime.now(ZoneId.of("GMT")).format(DateTimeFormatter.RFC_1123_DATE_TIME);

        // 构建签名原文
        String signatureOrigin = "host: " + host + "\n";
        signatureOrigin += "date: " + date + "\n";
        signatureOrigin += "GET " + path + " HTTP/1.1";

        // 进行HMAC-SHA256加密
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(hexDigits);

        // 构建authorization
        String authorization = String.format("api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
                apiKey, signature);

        // URL编码
        String encodedAuthorization = URLEncoder.encode(authorization, "UTF-8");
        String encodedDate = URLEncoder.encode(date, "UTF-8");
        String encodedHost = URLEncoder.encode(host, "UTF-8");

        return String.format("wss://%s%s?authorization=%s&date=%s&host=%s",
                host, path, encodedAuthorization, encodedDate, encodedHost);
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String base64Audio, SttConfig config) {
        Map<String, Object> requestBody = new HashMap<>();

        // 通用参数
        Map<String, Object> common = new HashMap<>();
        common.put("app_id", appId);
        requestBody.put("common", common);

        // 业务参数
        Map<String, Object> business = new HashMap<>();
        business.put("language", mapLanguage(config.getLanguage()));
        business.put("domain", "iat"); // 通用识别
        business.put("accent", "mandarin"); // 普通话
        business.put("vad_eos", 10000); // 静音检测时长
        business.put("dwa", "wpgs"); // 动态修正
        requestBody.put("business", business);

        // 数据参数
        Map<String, Object> data = new HashMap<>();
        data.put("status", 2); // 数据状态，0:首个音频数据包，1:中间，2:最后一个
        data.put("format", "audio/L16;rate=16000"); // 音频格式
        data.put("encoding", "raw");
        data.put("audio", base64Audio);
        requestBody.put("data", data);

        return requestBody;
    }

    /**
     * 映射语言代码
     */
    private String mapLanguage(String language) {
        if (language == null) return "zh_cn";

        switch (language.toLowerCase()) {
            case "zh-cn":
            case "zh_cn":
            case "chinese":
                return "zh_cn";
            case "en-us":
            case "en_us":
            case "english":
                return "en_us";
            default:
                return "zh_cn";
        }
    }

    /**
     * 解析流式响应
     */
    private Flux<SttResult> parseStreamResponse(Map<String, Object> response, SttConfig config) {
        try {
            Integer code = (Integer) response.get("code");
            if (code != null && code != 0) {
                String message = (String) response.get("message");
                throw new RuntimeException("科大讯飞API错误: " + code + " - " + message);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) data.get("result");
                if (result != null) {
                    String text = (String) result.get("ws");
                    if (text != null && !text.isEmpty()) {
                        SttResult sttResult = new SttResult();
                        sttResult.setText(text);
                        sttResult.setConfidence(0.95); // 科大讯飞不直接提供置信度
                        sttResult.setFinal(true);

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("provider", "XunfeiSTT");
                        metadata.put("language", config.getLanguage());
                        sttResult.setMetadata(metadata);

                        return Flux.just(sttResult);
                    }
                }
            }

            // 如果没有有效数据，返回空结果
            return Flux.empty();

        } catch (Exception e) {
            logger.error("解析科大讯飞响应失败", e);
            SttResult errorResult = new SttResult();
            errorResult.setText("[解析失败]");
            errorResult.setConfidence(0.0);
            errorResult.setFinal(true);
            return Flux.just(errorResult);
        }
    }

    /**
     * 解析批量响应
     */
    private SttResult parseBatchResponse(Map<String, Object> response, SttConfig config) {
        SttResult result = new SttResult();

        try {
            Integer code = (Integer) response.get("code");
            if (code != null && code != 0) {
                String message = (String) response.get("message");
                result.setText("[API错误: " + code + " - " + message + "]");
                result.setConfidence(0.0);
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultData = (Map<String, Object>) data.get("result");
                    if (resultData != null) {
                        String text = (String) resultData.get("ws");
                        result.setText(text != null ? text : "[无识别结果]");
                        result.setConfidence(0.95);
                    } else {
                        result.setText("[无识别结果]");
                        result.setConfidence(0.0);
                    }
                } else {
                    result.setText("[响应数据为空]");
                    result.setConfidence(0.0);
                }
            }
        } catch (Exception e) {
            logger.error("解析科大讯飞批量响应失败", e);
            result.setText("[解析失败: " + e.getMessage() + "]");
            result.setConfidence(0.0);
        }

        result.setFinal(true);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "XunfeiSTT");
        metadata.put("language", config.getLanguage());
        result.setMetadata(metadata);

        return result;
    }
}