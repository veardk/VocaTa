package com.vocata.ai.stt.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocata.ai.stt.SttClient;
import com.vocata.file.service.FileService;
import com.vocata.file.dto.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


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

    @Autowired
    private FileService fileService;

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
     * 调用七牛云ASR API - 修正版
     */
    private Mono<Map<String, Object>> callQiniuAsrApi(byte[] audioData, SttClient.SttConfig config) {
        return uploadAudioToQiniu(audioData, config)
            .flatMap(context -> invokeVoiceAsrApi(context, config)
                .onErrorResume(throwable -> handleAsrFallback("/voice/asr", throwable, context, config))
            )
            .onErrorResume(throwable -> {
                if (throwable instanceof WebClientResponseException responseException) {
                    logger.error("七牛云ASR接口调用失败，状态码: {}, 响应体: {}",
                            responseException.getStatusCode(), responseException.getResponseBodyAsString());
                } else {
                    logger.error("七牛云ASR接口调用失败: {}", throwable.getMessage(), throwable);
                }
                return Mono.error(throwable);
            });
    }

    /**
     * 构建请求体 - 修正版：先上传音频到七牛云存储，再调用ASR API
     */
    private Mono<UploadedAudioContext> uploadAudioToQiniu(byte[] audioData, SttClient.SttConfig config) {
        try {
            // 音频格式
            String format = mapAudioFormat(config.getAudioFormat());

            // 生成临时文件名
            String fileName = "stt_" + System.currentTimeMillis() + "." + format;

            // 创建MultipartFile（保留以兼容其他调用场景）
            MultipartFile audioFile = new InMemoryMultipartFile(
                "audio",
                fileName,
                "audio/" + format,
                audioData
            );

            logger.info("开始上传音频文件到七牛云存储: {} bytes, 格式: {}", audioData.length, format);

            // 上传音频文件到七牛云存储
            return Mono.fromCallable(() -> {
                FileUploadResponse uploadResponse = fileService.uploadAudioFile(
                    audioData,
                    audioFile.getOriginalFilename(),
                    "audio",
                    audioFile.getContentType()
                );
                logger.info("音频文件上传成功，URL: {}", uploadResponse.getFileUrl());
                return new UploadedAudioContext(format, uploadResponse.getFileUrl());
            })
            .onErrorMap(e -> {
                logger.error("上传音频文件失败", e);
                return new RuntimeException("上传音频文件到七牛云存储失败: " + e.getMessage(), e);
            });

        } catch (Exception e) {
            logger.error("构建请求体失败", e);
            return Mono.error(new RuntimeException("构建七牛云ASR请求失败", e));
        }
    }

    private Mono<Map<String, Object>> invokeVoiceAsrApi(UploadedAudioContext context, SttClient.SttConfig config) {
        try {
            Map<String, Object> request = new HashMap<>();
            String model = getModelForLanguage(config.getLanguage());
            request.put("model", model);

            if (StringUtils.hasText(config.getLanguage())) {
                request.put("language", config.getLanguage());
            }

            Map<String, Object> audio = new HashMap<>();
            audio.put("format", context.format());
            audio.put("url", context.audioUrl());
            request.put("audio", audio);

            String requestBodyJson = objectMapper.writeValueAsString(request);
            String path = "/voice/asr";
            Map<String, String> headers = buildAuthHeaders("POST", path, requestBodyJson);

            String url = endpoint + path;

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .doOnNext(response -> logger.info("七牛云ASR(voice/asr)响应: {}", response));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建七牛云voice/asr请求失败", e));
        }
    }

    private Mono<Map<String, Object>> invokeOpenAiStyleApi(UploadedAudioContext context, SttClient.SttConfig config) {
        try {
            Map<String, Object> request = new HashMap<>();
            String model = getModelForLanguage(config.getLanguage());
            request.put("model", model);

            if (StringUtils.hasText(config.getLanguage())) {
                request.put("language", config.getLanguage());
            }

            Map<String, Object> inputAudioWrapper = new HashMap<>();
            inputAudioWrapper.put("type", "input_audio");

            Map<String, Object> inputAudio = new HashMap<>();
            inputAudio.put("url", context.audioUrl());
            inputAudio.put("format", context.format());
            inputAudioWrapper.put("input_audio", inputAudio);

            request.put("input_audio", List.of(inputAudioWrapper));
            request.put("response_format", "verbose_json");

            String requestBodyJson = objectMapper.writeValueAsString(request);
            String path = "/audio/transcriptions";
            Map<String, String> headers = buildAuthHeaders("POST", path, requestBodyJson);

            String url = endpoint + path;

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .doOnNext(response -> logger.info("七牛云ASR(OpenAI兼容)响应: {}", response));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建七牛云OpenAI兼容ASR请求失败", e));
        }
    }

    private Mono<Map<String, Object>> invokeLegacyAsrApi(UploadedAudioContext context, SttClient.SttConfig config) {
        try {
            Map<String, Object> request = new HashMap<>();
            String model = getModelForLanguage(config.getLanguage());
            request.put("model", model);

            if (StringUtils.hasText(config.getLanguage())) {
                request.put("language", config.getLanguage());
            }

            Map<String, Object> audio = new HashMap<>();
            audio.put("format", context.format());
            audio.put("url", context.audioUrl());
            request.put("audio", audio);

            String requestBodyJson = objectMapper.writeValueAsString(request);
            String path = "/asr";
            Map<String, String> headers = buildAuthHeaders("POST", path, requestBodyJson);
            String url = endpoint + path;

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .doOnNext(response -> logger.info("七牛云ASR(传统接口)响应: {}", response));
        } catch (Exception e) {
            return Mono.error(new RuntimeException("构建七牛云传统ASR请求失败", e));
        }
    }

    private Mono<Map<String, Object>> handleAsrFallback(String failedPath,
                                                        Throwable throwable,
                                                        UploadedAudioContext context,
                                                        SttClient.SttConfig config) {
        if (throwable instanceof WebClientResponseException responseException) {
            logger.warn("调用七牛云ASR接口{}失败，状态码: {}, 响应体: {}",
                    failedPath, responseException.getStatusCode(), responseException.getResponseBodyAsString());

            HttpStatusCode statusCode = responseException.getStatusCode();
            if (statusCode.isSameCodeAs(HttpStatus.NOT_FOUND) || statusCode.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
                if ("/voice/asr".equals(failedPath)) {
                    logger.info("尝试回退到七牛云传统ASR接口 /asr");
                    return invokeLegacyAsrApi(context, config)
                            .onErrorResume(inner -> handleAsrFallback("/asr", inner, context, config));
                }

                if ("/asr".equals(failedPath)) {
                    logger.info("尝试回退到七牛云OpenAI兼容ASR接口 /audio/transcriptions");
                    return invokeOpenAiStyleApi(context, config);
                }
            }
        }
        return Mono.error(throwable);
    }

    private record UploadedAudioContext(String format, String audioUrl) {}

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
     * 解析ASR响应 - 根据七牛云官方文档修正
     */
    private SttClient.SttResult parseAsrResponse(Map<String, Object> response, SttClient.SttConfig config) {
        SttClient.SttResult result = new SttClient.SttResult();

        try {
            logger.info("解析七牛云ASR响应: {}", response);

            // 检查是否有错误
            if (response.containsKey("error")) {
                String errorMessage = (String) response.get("error");
                result.setText("识别失败: " + errorMessage);
                result.setConfidence(0.0);
                logger.error("七牛云ASR API错误: {}", errorMessage);
            } else if (response.containsKey("data")) {
                // 成功响应 - 根据官方文档格式
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");

                if (data != null && data.containsKey("result")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultData = (Map<String, Object>) data.get("result");

                    if (resultData != null && resultData.containsKey("text")) {
                        String text = (String) resultData.get("text");
                        result.setText(StringUtils.hasText(text) ? text : "");
                        result.setConfidence(0.95); // 七牛云默认高置信度
                        logger.info("七牛云ASR识别成功: '{}'", text);
                    } else {
                        result.setText("");
                        result.setConfidence(0.0);
                        logger.warn("七牛云ASR响应中没有找到text字段");
                    }
                } else {
                    result.setText("");
                    result.setConfidence(0.0);
                    logger.warn("七牛云ASR响应中没有找到result字段");
                }
            } else {
                // 处理其他可能的响应格式
                result.setText("无法解析识别结果");
                result.setConfidence(0.0);
                logger.warn("七牛云ASR响应格式不匹配: {}", response);
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
        metadata.put("endpoint", endpoint);
        result.setMetadata(metadata);

        logger.info("七牛云ASR识别完成: 文本长度={}, 置信度={}",
                   result.getText().length(), result.getConfidence());

        return result;
    }
}
