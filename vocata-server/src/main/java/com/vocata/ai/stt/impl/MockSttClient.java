package com.vocata.ai.stt.impl;

import com.vocata.ai.stt.SttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 模拟STT服务实现（用于测试）
 * 在实际项目中应该替换为真实的STT服务提供商
 */
@Service
public class MockSttClient implements SttClient {

    private static final Logger logger = LoggerFactory.getLogger(MockSttClient.class);

    // 模拟识别结果
    private static final String[] MOCK_TEXTS = {
        "你好", "我想", "和你", "聊天", "关于", "人工智能", "的话题"
    };

    @Override
    public String getProviderName() {
        return "MockSTT";
    }

    @Override
    public boolean isAvailable() {
        return true; // 模拟服务总是可用
    }

    @Override
    public Flux<SttResult> streamRecognize(Flux<byte[]> audioStream, SttConfig config) {
        logger.info("开始流式语音识别，语言: {}", config.getLanguage());

        return audioStream
                .buffer(Duration.ofMillis(500)) // 每500ms处理一批音频数据
                .take(MOCK_TEXTS.length) // 限制结果数量
                .index() // 添加索引
                .map(tuple -> {
                    Long index = tuple.getT1();

                    // 创建模拟识别结果
                    SttResult result = new SttResult();
                    result.setText(MOCK_TEXTS[index.intValue() % MOCK_TEXTS.length]);
                    result.setConfidence(0.95 - (index * 0.01)); // 模拟置信度递减
                    result.setFinal(index == MOCK_TEXTS.length - 1); // 最后一个为最终结果
                    result.setStartTimeMs(index * 500);
                    result.setEndTimeMs((index + 1) * 500);

                    // 添加元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("provider", "MockSTT");
                    metadata.put("language", config.getLanguage());
                    metadata.put("chunk_index", index);
                    result.setMetadata(metadata);

                    logger.debug("STT识别结果: {} (置信度: {})", result.getText(), result.getConfidence());

                    return result;
                })
                .delayElements(Duration.ofMillis(100)); // 模拟处理延迟
    }

    @Override
    public Mono<SttResult> recognize(byte[] audioData, SttConfig config) {
        logger.info("开始批量语音识别，数据大小: {} bytes", audioData.length);

        return Mono.delay(Duration.ofMillis(1000)) // 模拟处理时间
                .map(tick -> {
                    // 创建完整识别结果
                    SttResult result = new SttResult();
                    result.setText(String.join("", MOCK_TEXTS)); // 拼接所有模拟文本
                    result.setConfidence(0.92);
                    result.setFinal(true);
                    result.setStartTimeMs(0);
                    result.setEndTimeMs(audioData.length / 32); // 模拟音频时长

                    // 添加元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("provider", "MockSTT");
                    metadata.put("language", config.getLanguage());
                    metadata.put("audio_size_bytes", audioData.length);
                    metadata.put("processing_time_ms", 1000);
                    result.setMetadata(metadata);

                    logger.info("批量STT识别完成: {}", result.getText());

                    return result;
                });
    }
}