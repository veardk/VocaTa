package com.vocata.ai.config;

import com.vocata.ai.llm.LlmProvider;
import com.vocata.ai.stt.SttClient;
import com.vocata.ai.tts.TtsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

/**
 * AI服务配置
 * 根据配置选择合适的LLM、STT和TTS提供者
 */
@Configuration
public class AiServiceConfig {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceConfig.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${ai.llm.provider:gemini}")
    private String preferredProvider;

    @Value("${ai.stt.provider:xunfei}")
    private String preferredSttProvider;

    @Value("${ai.tts.provider:volcan}")
    private String preferredTtsProvider;

    /**
     * 选择并配置主要的LLM提供者
     */
    @Bean
    @Primary
    public LlmProvider primaryLlmProvider() {
        Map<String, LlmProvider> providers = applicationContext.getBeansOfType(LlmProvider.class);

        logger.info("检测到的LLM提供者: {}", providers.keySet());

        // 首先尝试使用配置的首选提供者
        LlmProvider preferredLlmProvider = findProviderByName(providers, preferredProvider);
        if (preferredLlmProvider != null && preferredLlmProvider.isAvailable()) {
            logger.info("使用首选LLM提供者: {}", preferredLlmProvider.getProviderName());
            return preferredLlmProvider;
        }

        // 如果首选提供者不可用，按优先级选择可用的提供者
        String[] providerPriority = {"gemini", "openai", "mock"};

        for (String providerName : providerPriority) {
            LlmProvider provider = findProviderByName(providers, providerName);
            if (provider != null && provider.isAvailable()) {
                logger.info("使用备用LLM提供者: {}", provider.getProviderName());
                return provider;
            }
        }

        // 如果没有找到可用的提供者，抛出异常
        throw new RuntimeException("未找到可用的LLM提供者。请检查API配置。");
    }

    /**
     * 选择并配置主要的STT提供者
     */
    @Bean
    @Primary
    public SttClient primarySttClient() {
        Map<String, SttClient> clients = applicationContext.getBeansOfType(SttClient.class);

        logger.info("检测到的STT提供者: {}", clients.keySet());

        // 首先尝试使用配置的首选提供者
        SttClient preferredSttClient = findSttClientByName(clients, preferredSttProvider);
        if (preferredSttClient != null && preferredSttClient.isAvailable()) {
            logger.info("使用首选STT提供者: {}", preferredSttClient.getProviderName());
            return preferredSttClient;
        }

        // 如果首选提供者不可用，按优先级选择可用的提供者
        String[] sttProviderPriority = {"xunfei", "mock"};

        for (String providerName : sttProviderPriority) {
            SttClient client = findSttClientByName(clients, providerName);
            if (client != null && client.isAvailable()) {
                logger.info("使用备用STT提供者: {}", client.getProviderName());
                return client;
            }
        }

        // 如果没有找到可用的提供者，抛出异常
        throw new RuntimeException("未找到可用的STT提供者。请检查API配置。");
    }

    /**
     * 选择并配置主要的TTS提供者
     */
    @Bean
    @Primary
    public TtsClient primaryTtsClient() {
        Map<String, TtsClient> clients = applicationContext.getBeansOfType(TtsClient.class);

        logger.info("检测到的TTS提供者: {}", clients.keySet());

        // 首先尝试使用配置的首选提供者
        TtsClient preferredTtsClient = findTtsClientByName(clients, preferredTtsProvider);
        if (preferredTtsClient != null && preferredTtsClient.isAvailable()) {
            logger.info("使用首选TTS提供者: {}", preferredTtsClient.getProviderName());
            return preferredTtsClient;
        }

        // 如果首选提供者不可用，按优先级选择可用的提供者
        String[] ttsProviderPriority = {"volcan", "mock"};

        for (String providerName : ttsProviderPriority) {
            TtsClient client = findTtsClientByName(clients, providerName);
            if (client != null && client.isAvailable()) {
                logger.info("使用备用TTS提供者: {}", client.getProviderName());
                return client;
            }
        }

        // 如果没有找到可用的提供者，抛出异常
        throw new RuntimeException("未找到可用的TTS提供者。请检查API配置。");
    }

    private LlmProvider findProviderByName(Map<String, LlmProvider> providers, String name) {
        // 尝试通过bean名称查找
        String beanName = name.toLowerCase() + "LlmProvider";
        LlmProvider provider = providers.get(beanName);

        if (provider != null) {
            return provider;
        }

        // 尝试通过provider名称查找
        for (LlmProvider p : providers.values()) {
            if (p.getProviderName().equalsIgnoreCase(name)) {
                return p;
            }
        }

        return null;
    }

    private SttClient findSttClientByName(Map<String, SttClient> clients, String name) {
        // 尝试通过bean名称查找
        String beanName = name.toLowerCase() + "SttClient";
        SttClient client = clients.get(beanName);

        if (client != null) {
            return client;
        }

        // 尝试通过provider名称查找
        for (SttClient c : clients.values()) {
            if (c.getProviderName().toLowerCase().contains(name.toLowerCase())) {
                return c;
            }
        }

        return null;
    }

    private TtsClient findTtsClientByName(Map<String, TtsClient> clients, String name) {
        // 尝试通过bean名称查找
        String beanName = name.toLowerCase() + "TtsClient";
        TtsClient client = clients.get(beanName);

        if (client != null) {
            return client;
        }

        // 尝试通过provider名称查找
        for (TtsClient c : clients.values()) {
            if (c.getProviderName().toLowerCase().contains(name.toLowerCase())) {
                return c;
            }
        }

        return null;
    }
}