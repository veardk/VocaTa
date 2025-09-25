package com.vocata.ai.tts.impl;

import com.vocata.ai.tts.TtsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 模拟TTS服务实现（用于测试）
 * 在实际项目中应该替换为真实的TTS服务提供商
 */
@Service
public class MockTtsClient implements TtsClient {

    private static final Logger logger = LoggerFactory.getLogger(MockTtsClient.class);

    private static final String[] SUPPORTED_VOICES = {
        "xiaoxiao", "xiaoyi", "xiaoyun", "xiaomo"
    };

    private final Random random = new Random();

    @Override
    public String getProviderName() {
        return "MockTTS";
    }

    @Override
    public boolean isAvailable() {
        return true; // 模拟服务总是可用
    }

    @Override
    public Flux<byte[]> streamSynthesize(Flux<String> textStream, TtsConfig config) {
        logger.info("开始流式语音合成，语音ID: {}", config.getVoiceId());

        return textStream
                .filter(text -> text != null && !text.trim().isEmpty())
                .doOnNext(text -> logger.debug("合成文本块: {}", text))
                .map(text -> {
                    // 为每个文本块生成模拟音频数据
                    int audioSize = text.length() * 1000; // 假设每个字符1KB音频
                    byte[] mockAudioData = generateMockAudioData(audioSize);

                    logger.debug("生成音频数据: {} bytes", mockAudioData.length);

                    return mockAudioData;
                })
                .delayElements(Duration.ofMillis(200)); // 模拟合成延迟
    }

    @Override
    public Mono<TtsResult> synthesize(String text, TtsConfig config) {
        logger.info("开始批量语音合成，文本: {}, 语音ID: {}",
                   text.substring(0, Math.min(text.length(), 20)) + "...",
                   config.getVoiceId());

        return Mono.delay(Duration.ofMillis(500)) // 模拟处理时间
                .map(tick -> {
                    // 生成模拟音频数据
                    int audioSize = text.length() * 1000; // 假设每个字符1KB音频
                    byte[] mockAudioData = generateMockAudioData(audioSize);

                    // 创建TTS结果
                    TtsResult result = new TtsResult();
                    result.setAudioData(mockAudioData);
                    result.setAudioFormat(config.getAudioFormat());
                    result.setSampleRate(config.getSampleRate());
                    result.setVoiceId(config.getVoiceId());

                    // 估算音频时长（假设每个字符0.5秒）
                    double durationSeconds = text.length() * 0.5;
                    result.setDurationSeconds(durationSeconds);

                    // 添加元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("provider", "MockTTS");
                    metadata.put("voice_id", config.getVoiceId());
                    metadata.put("language", config.getLanguage());
                    metadata.put("text_length", text.length());
                    metadata.put("audio_size_bytes", audioSize);
                    metadata.put("processing_time_ms", 500);
                    result.setMetadata(metadata);

                    logger.info("TTS合成完成，音频时长: {}秒, 数据大小: {} bytes",
                               durationSeconds, mockAudioData.length);

                    return result;
                });
    }

    @Override
    public String[] getSupportedVoices() {
        return SUPPORTED_VOICES.clone();
    }

    @Override
    public double estimateAudioDuration(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        // 简单估算：假设每个字符0.5秒
        return text.length() * 0.5;
    }

    /**
     * 生成模拟音频数据
     */
    private byte[] generateMockAudioData(int size) {
        byte[] audioData = new byte[size];

        // 生成简单的音频波形模拟数据
        for (int i = 0; i < size; i++) {
            // 生成正弦波数据（模拟音频）
            double frequency = 440.0; // A音符频率
            double sample = Math.sin(2.0 * Math.PI * frequency * i / 44100.0);
            audioData[i] = (byte) (sample * 127);
        }

        return audioData;
    }
}