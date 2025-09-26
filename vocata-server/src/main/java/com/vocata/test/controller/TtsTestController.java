package com.vocata.test.controller;

import com.vocata.ai.tts.TtsClient;
import com.vocata.common.result.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TTS测试控制器
 * 用于测试科大讯飞TTS功能
 */
@RestController
@RequestMapping("/api/open/test/tts")
public class TtsTestController {

    private static final Logger logger = LoggerFactory.getLogger(TtsTestController.class);

    @Autowired
    @Qualifier("xunfeiTtsClient")
    private TtsClient ttsClient;

    /**
     * 测试TTS合成
     */
    @PostMapping("/synthesize")
    public ApiResponse<Map<String, Object>> testSynthesize(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            text = "你好，这是一个TTS测试";
        }

        logger.info("开始测试TTS合成 - 文本: '{}'", text);

        try {
            // 创建TTS配置
            TtsClient.TtsConfig config = new TtsClient.TtsConfig();
            config.setVoiceId("xiaoyan");
            config.setLanguage("zh-CN");
            config.setAudioFormat("mp3");
            config.setSampleRate(16000);
            config.setSpeed(1.0);
            config.setVolume(1.0);
            config.setPitch(1.0);

            // 调用TTS服务
            TtsClient.TtsResult result = ttsClient.synthesize(text, config).block();

            if (result != null && result.getAudioData() != null && result.getAudioData().length > 0) {
                logger.info("TTS合成成功 - 音频大小: {} bytes", result.getAudioData().length);

                return ApiResponse.success(Map.of(
                    "success", true,
                    "audioSize", result.getAudioData().length,
                    "format", result.getAudioFormat(),
                    "sampleRate", result.getSampleRate(),
                    "voiceId", result.getVoiceId(),
                    "text", text
                ));
            } else {
                logger.error("TTS合成失败 - 没有返回音频数据");
                return ApiResponse.error(500, "TTS合成失败 - 没有返回音频数据");
            }

        } catch (Exception e) {
            logger.error("TTS测试异常", e);
            return ApiResponse.error(500, "TTS测试失败: " + e.getMessage());
        }
    }

    /**
     * 检查TTS服务状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> checkStatus() {
        logger.info("检查TTS服务状态");

        try {
            boolean isAvailable = ttsClient.isAvailable();
            String[] supportedVoices = ttsClient.getSupportedVoices();

            return ApiResponse.success(Map.of(
                "providerName", ttsClient.getProviderName(),
                "isAvailable", isAvailable,
                "supportedVoices", supportedVoices
            ));

        } catch (Exception e) {
            logger.error("检查TTS状态异常", e);
            return ApiResponse.error(500, "检查TTS状态失败: " + e.getMessage());
        }
    }
}