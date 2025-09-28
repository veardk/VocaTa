package com.vocata.ai.stt.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.ai.stt.SttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 七牛云语音识别服务实现
 * 基于七牛云AI Token API - ASR语音识别
 * 文档: https://developer.qiniu.com/aitokenapi/12981/asr-tts-ocr-api
 */
@Service
public class QiniuSttClient implements SttClient {

    private static final Logger logger = LoggerFactory.getLogger(QiniuSttClient.class);

    @Value("${qiniu.ai.api-key:}")
    private String apiKey;

    @Value("${qiniu.access-key:}")
    private String accessKey;

    @Value("${qiniu.secret-key:}")
    private String secretKey;

    @Value("${qiniu.stt.endpoint:https://openai.qiniu.com/v1}")
    private String endpoint;

    @Value("${qiniu.stt.model:asr}")
    private String defaultModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // 支持的语音识别模型 (根据七牛云文档)
    private static final Map<String, String> SUPPORTED_MODELS = Map.of(
        "zh-CN", "asr",         // 中文识别
        "en-US", "asr",         // 英文识别 (七牛云统一使用asr模型)
        "zh_cn", "asr",
        "en_us", "asr"
    );

    // 支持的音频格式
    private static final String[] SUPPORTED_FORMATS = {
        "wav", "mp3", "aac", "flac", "m4a", "ogg", "webm"
    };

    public QiniuSttClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getProviderName() {
        return "七牛云STT";
    }

    @Override
    public boolean isAvailable() {
        // 优先使用AI API Key，如果没有则使用存储access key
        String tokenToUse = StringUtils.hasText(apiKey) && !apiKey.equals("your-qiniu-ai-api-key")
                          ? apiKey
                          : accessKey;

        boolean isConfigured = StringUtils.hasText(tokenToUse) &&
                              !tokenToUse.equals("your-qiniu-access-key");

        if (!isConfigured) {
            logger.warn("七牛云STT配置不完整 - 需要配置qiniu.ai.api-key或qiniu.access-key");
        }

        return isConfigured;
    }

    @Override
    public Flux<SttClient.SttResult> streamRecognize(Flux<byte[]> audioStream, SttClient.SttConfig config) {
        if (!isAvailable()) {
            return Flux.error(new RuntimeException("七牛云STT服务配置不完整：需要access-key和secret-key"));
        }

        logger.info("开始七牛云流式语音识别，语言: {}, 模型: {}", config.getLanguage(), getModelForLanguage(config.getLanguage()));

        // 七牛云ASR API目前主要支持批量识别，流式识别通过收集音频数据后批量处理实现
        return audioStream
                .collectList()
                .flatMapMany(audioChunks -> {
                    // 合并所有音频数据块
                    int totalLength = audioChunks.stream().mapToInt(chunk -> chunk.length).sum();
                    byte[] combinedAudio = new byte[totalLength];
                    int offset = 0;
                    for (byte[] chunk : audioChunks) {
                        System.arraycopy(chunk, 0, combinedAudio, offset, chunk.length);
                        offset += chunk.length;
                    }

                    // 调用批量识别并转换为流式结果
                    return recognize(combinedAudio, config)
                            .map(result -> {
                                // 为流式识别创建中间结果
                                SttClient.SttResult streamResult = new SttClient.SttResult();
                                streamResult.setText(result.getText());
                                streamResult.setConfidence(result.getConfidence());
                                streamResult.setFinal(result.isFinal());
                                streamResult.setMetadata(result.getMetadata());
                                return streamResult;
                            })
                            .flux();
                })
                .onErrorResume(error -> {
                    logger.error("七牛云STT流式识别失败", error);
                    SttClient.SttResult errorResult = new SttClient.SttResult();
                    errorResult.setText("语音识别服务暂时不可用，请稍后再试");
                    errorResult.setConfidence(0.0);
                    errorResult.setFinal(true);
                    return Flux.just(errorResult);
                });
    }

    @Override
    public Mono<SttClient.SttResult> recognize(byte[] audioData, SttClient.SttConfig config) {
        if (!isAvailable()) {
            return Mono.error(new RuntimeException("七牛云STT服务配置不完整：需要access-key和secret-key"));
        }

        if (audioData == null || audioData.length == 0) {
            return Mono.error(new RuntimeException("音频数据不能为空"));
        }

        logger.info("开始七牛云批量语音识别，数据大小: {} bytes, 语言: {}", audioData.length, config.getLanguage());

        try {
            return callQiniuAsrApi(audioData, config)
                    .map(response -> parseAsrResponse(response, config))
                    .onErrorResume(error -> {
                        logger.error("七牛云STT批量识别失败", error);
                        SttClient.SttResult errorResult = new SttClient.SttResult();
                        errorResult.setText("语音识别服务暂时不可用，请稍后再试");
                        errorResult.setConfidence(0.0);
                        errorResult.setFinal(true);

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("provider", "QiniuSTT");
                        metadata.put("error", error.getMessage());
                        errorResult.setMetadata(metadata);

                        return Mono.just(errorResult);
                    });

        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建七牛云ASR请求失败", e));
        }
    }

    /**
     * 调用七牛云ASR API
     */
    private Mono<Map<String, Object>> callQiniuAsrApi(byte[] audioData, SttClient.SttConfig config) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(audioData, config);
            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            // 构建认证头
            String path = "/voice/asr";
            Map<String, String> headers = buildAuthHeaders("POST", path, requestBodyJson);

            String url = endpoint + path;

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .doOnNext(response -> logger.debug("七牛云ASR API响应: {}", response))
                    .doOnError(error -> logger.error("七牛云ASR API调用失败: {}", error.getMessage()));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建七牛云ASR API请求失败", e));
        }
    }

    /**
     * 构建请求体 (根据七牛云语音识别API文档)
     * 七牛云ASR API需要音频文件的URL，不是直接的base64数据
     */
    private Map<String, Object> buildRequestBody(byte[] audioData, SttClient.SttConfig config) {
        Map<String, Object> request = new HashMap<>();

        // 模型
        String model = getModelForLanguage(config.getLanguage());
        request.put("model", model);

        // 音频数据结构
        Map<String, Object> audio = new HashMap<>();

        // 音频格式
        String format = mapAudioFormat(config.getAudioFormat());
        audio.put("format", format);

        // 七牛云ASR API要求提供音频文件的URL
        // 由于我们有音频数据但没有URL，我们需要：
        // 1. 将音频数据上传到七牛云存储
        // 2. 获取可访问的URL
        // 3. 使用该URL调用ASR API

        // 暂时使用base64数据URL作为兼容方案
        String base64Audio = Base64.getEncoder().encodeToString(audioData);
        String dataUrl = "data:audio/" + format + ";base64," + base64Audio;
        audio.put("url", dataUrl);

        request.put("audio", audio);

        logger.debug("七牛云ASR请求体: model={}, format={}, 数据大小: {} bytes", model, format, audioData.length);

        return request;
    }

    /**
     * 根据语言获取模型
     */
    private String getModelForLanguage(String language) {
        if (language == null) {
            return defaultModel;
        }

        String model = SUPPORTED_MODELS.get(language.toLowerCase());
        if (model != null) {
            return model;
        }

        // 简单的语言映射 - 七牛云统一使用asr模型
        if (language.toLowerCase().startsWith("zh")) {
            return "asr";
        } else if (language.toLowerCase().startsWith("en")) {
            return "asr";
        }

        return defaultModel;
    }

    /**
     * 映射音频格式
     */
    private String mapAudioFormat(String format) {
        if (format == null || format.isEmpty()) {
            return "wav"; // 默认格式
        }

        String lowerFormat = format.toLowerCase();

        // 支持的格式直接返回
        for (String supportedFormat : SUPPORTED_FORMATS) {
            if (lowerFormat.equals(supportedFormat) || lowerFormat.endsWith(supportedFormat)) {
                return supportedFormat;
            }
        }

        // 格式映射
        if (lowerFormat.contains("webm")) return "webm";
        if (lowerFormat.contains("wav")) return "wav";
        if (lowerFormat.contains("mp3")) return "mp3";
        if (lowerFormat.contains("m4a")) return "m4a";
        if (lowerFormat.contains("aac")) return "aac";
        if (lowerFormat.contains("ogg")) return "ogg";
        if (lowerFormat.contains("flac")) return "flac";

        return "wav"; // 默认格式
    }

    /**
     * 构建认证头 (使用Bearer Token方式)
     */
    private Map<String, String> buildAuthHeaders(String method, String path, String body) throws Exception {
        Map<String, String> headers = new HashMap<>();

        // 优先使用AI API Key，如果没有则使用存储access key
        String tokenToUse = StringUtils.hasText(apiKey) && !apiKey.equals("your-qiniu-ai-api-key")
                          ? apiKey
                          : accessKey;

        headers.put("Authorization", "Bearer " + tokenToUse);
        headers.put("Content-Type", "application/json");

        return headers;
    }

    /**
     * 解析ASR响应
     */
    private SttClient.SttResult parseAsrResponse(Map<String, Object> response, SttClient.SttConfig config) {
        SttClient.SttResult result = new SttClient.SttResult();

        try {
            // 检查响应状态
            Object codeObj = response.get("code");
            Integer code = null;
            if (codeObj instanceof Integer) {
                code = (Integer) codeObj;
            } else if (codeObj instanceof String) {
                try {
                    code = Integer.parseInt((String) codeObj);
                } catch (NumberFormatException e) {
                    logger.warn("无法解析响应code: {}", codeObj);
                }
            }

            if (code != null && code == 200) {
                // 成功响应
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    // 提取识别结果
                    Object textObj = data.get("result");
                    if (textObj instanceof String) {
                        String text = (String) textObj;
                        result.setText(StringUtils.hasText(text) ? text : "");
                        result.setConfidence(0.95); // 七牛云默认高置信度
                    } else if (textObj instanceof List) {
                        // 如果结果是数组格式
                        @SuppressWarnings("unchecked")
                        List<Object> resultList = (List<Object>) textObj;
                        if (!resultList.isEmpty()) {
                            StringBuilder textBuilder = new StringBuilder();
                            for (Object item : resultList) {
                                if (item instanceof String) {
                                    textBuilder.append(item).append(" ");
                                } else if (item instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> itemMap = (Map<String, Object>) item;
                                    Object itemText = itemMap.get("text");
                                    if (itemText instanceof String) {
                                        textBuilder.append(itemText).append(" ");
                                    }
                                }
                            }
                            result.setText(textBuilder.toString().trim());
                            result.setConfidence(0.95);
                        } else {
                            result.setText("");
                            result.setConfidence(0.0);
                        }
                    } else {
                        result.setText("");
                        result.setConfidence(0.0);
                    }

                    // 提取置信度（如果API返回）
                    Object confidenceObj = data.get("confidence");
                    if (confidenceObj instanceof Number) {
                        result.setConfidence(((Number) confidenceObj).doubleValue());
                    }
                } else {
                    result.setText("识别结果为空");
                    result.setConfidence(0.0);
                }
            } else {
                // 错误响应
                String message = (String) response.get("message");
                result.setText("识别失败: " + (message != null ? message : "未知错误"));
                result.setConfidence(0.0);
                logger.error("七牛云ASR API错误: code={}, message={}", code, message);
            }
        } catch (Exception e) {
            logger.error("解析七牛云ASR响应失败", e);
            result.setText("解析识别结果失败");
            result.setConfidence(0.0);
        }

        result.setFinal(true);

        // 设置元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "QiniuSTT");
        metadata.put("model", getModelForLanguage(config.getLanguage()));
        metadata.put("language", config.getLanguage());
        result.setMetadata(metadata);

        logger.info("七牛云ASR识别完成: 文本长度={}, 置信度={}",
                   result.getText().length(), result.getConfidence());

        return result;
    }
}