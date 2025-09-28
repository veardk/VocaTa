package com.vocata.ai.config;

import com.vocata.ai.llm.LlmProvider;
import com.vocata.ai.stt.SttClient;
import com.vocata.ai.tts.TtsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * AI服务配置
 * 根据配置选择合适的LLM、STT和TTS提供者
 * 修复版本：使用直接参数注入避免循环依赖
 */
@Configuration
public class AiServiceConfig {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceConfig.class);

    @Value("${ai.llm.provider:qiniu}")
    private String preferredProvider;

    @Value("${ai.stt.provider:qiniu}")
    private String preferredSttProvider;

    @Value("${ai.tts.provider:volcan}")
    private String preferredTtsProvider;

    /**
     * 选择并配置主要的LLM提供者
     * 使用直接参数注入避免循环依赖
     */
    @Bean
    @Primary
    public LlmProvider primaryLlmProvider(
            @Qualifier("qiniuLlmProvider") LlmProvider qiniuLlmProvider,
            @Qualifier("geminiLlmProvider") LlmProvider geminiLlmProvider,
            @Qualifier("openAiLlmProvider") LlmProvider openAiLlmProvider,
            @Qualifier("siliconFlowLlmProvider") LlmProvider siliconFlowLlmProvider) {

        logger.info("开始选择LLM提供者，首选: {}", preferredProvider);

        // 构建提供者列表
        List<LlmProvider> providers = List.of(qiniuLlmProvider, geminiLlmProvider, openAiLlmProvider, siliconFlowLlmProvider);

        // 记录检测到的提供者
        providers.forEach(provider -> {
            logger.info("检测到LLM提供者: {} - 可用状态: {}",
                provider.getProviderName(), provider.isAvailable());
        });

        // 首先尝试使用配置的首选提供者
        LlmProvider preferredLlmProvider = findProviderByName(providers, preferredProvider);
        if (preferredLlmProvider != null && preferredLlmProvider.isAvailable()) {
            logger.info("使用首选LLM提供者: {}", preferredLlmProvider.getProviderName());
            return preferredLlmProvider;
        }

        // 如果首选提供者不可用，按优先级选择可用的提供者
        String[] providerPriority = {"qiniu", "gemini", "openai", "siliconflow"};

        for (String providerName : providerPriority) {
            LlmProvider provider = findProviderByName(providers, providerName);
            if (provider != null && provider.isAvailable()) {
                logger.info("使用备用LLM提供者: {}", provider.getProviderName());
                return provider;
            }
        }

        // 如果没有找到可用的提供者，返回第一个提供者作为默认值
        logger.warn("未找到可用的LLM提供者，使用默认提供者: {}", qiniuLlmProvider.getProviderName());
        return qiniuLlmProvider;
    }

    /**
     * 选择并配置主要的STT提供者
     */
    @Bean
    @Primary
    public SttClient primarySttClient(List<SttClient> sttClients) {
        logger.info("开始选择STT提供者，首选: {}", preferredSttProvider);
        
        // 记录检测到的提供者
        sttClients.forEach(client -> {
            logger.info("检测到STT提供者: {} - 可用状态: {}", 
                client.getProviderName(), client.isAvailable());
        });

        // 首先尝试使用配置的首选提供者
        SttClient preferredSttClient = findSttClientByName(sttClients, preferredSttProvider);
        if (preferredSttClient != null && preferredSttClient.isAvailable()) {
            logger.info("使用首选STT提供者: {}", preferredSttClient.getProviderName());
            return preferredSttClient;
        }

        // 如果首选提供者不可用，按优先级选择可用的提供者
        String[] sttProviderPriority = {"qiniu", "xunfei", "mock"};

        for (String providerName : sttProviderPriority) {
            SttClient client = findSttClientByName(sttClients, providerName);
            if (client != null && client.isAvailable()) {
                logger.info("使用备用STT提供者: {}", client.getProviderName());
                return client;
            }
        }

        // 如果没有找到可用的提供者，返回第一个提供者作为默认值
        if (!sttClients.isEmpty()) {
            logger.warn("未找到可用的STT提供者，使用默认提供者: {}", sttClients.get(0).getProviderName());
            return sttClients.get(0);
        }

        throw new RuntimeException("未找到任何STT提供者。请检查配置。");
    }

    /**
     * 选择并配置主要的TTS提供者
     */
    @Bean
    @Primary
    public TtsClient primaryTtsClient(List<TtsClient> ttsClients) {
        logger.info("开始选择TTS提供者，首选: {}", preferredTtsProvider);
        
        // 记录检测到的提供者
        ttsClients.forEach(client -> {
            logger.info("检测到TTS提供者: {} - 可用状态: {}", 
                client.getProviderName(), client.isAvailable());
        });

        // 首先尝试使用配置的首选提供者
        TtsClient preferredTtsClient = findTtsClientByName(ttsClients, preferredTtsProvider);
        if (preferredTtsClient != null && preferredTtsClient.isAvailable()) {
            logger.info("使用首选TTS提供者: {}", preferredTtsClient.getProviderName());
            return preferredTtsClient;
        }

        // 如果首选提供者不可用，按优先级选择可用的提供者
        String[] ttsProviderPriority = {"xunfei", "volcan", "mock"};

        for (String providerName : ttsProviderPriority) {
            TtsClient client = findTtsClientByName(ttsClients, providerName);
            if (client != null && client.isAvailable()) {
                logger.info("使用备用TTS提供者: {}", client.getProviderName());
                return client;
            }
        }

        // 如果没有找到可用的提供者，返回第一个提供者作为默认值
        if (!ttsClients.isEmpty()) {
            logger.warn("未找到可用的TTS提供者，使用默认提供者: {}", ttsClients.get(0).getProviderName());
            return ttsClients.get(0);
        }

        throw new RuntimeException("未找到任何TTS提供者。请检查配置。");
    }

    private LlmProvider findProviderByName(List<LlmProvider> providers, String name) {
        // 尝试通过provider名称查找
        for (LlmProvider p : providers) {
            if (p.getProviderName().toLowerCase().contains(name.toLowerCase())) {
                return p;
            }
        }
        return null;
    }

    private SttClient findSttClientByName(List<SttClient> clients, String name) {
        // 尝试通过provider名称查找
        for (SttClient c : clients) {
            String providerName = c.getProviderName().toLowerCase();
            String searchName = name.toLowerCase();

            // 直接包含匹配
            if (providerName.contains(searchName)) {
                return c;
            }

            // 特殊匹配规则 - 科大讯飞
            if ("xunfei".equals(searchName) && (providerName.contains("科大讯飞") || providerName.contains("xunfei"))) {
                return c;
            }

            // 特殊匹配规则 - 七牛云
            if ("qiniu".equals(searchName) && (providerName.contains("七牛") || providerName.contains("qiniu"))) {
                return c;
            }
        }
        return null;
    }

    private TtsClient findTtsClientByName(List<TtsClient> clients, String name) {
        // 尝试通过provider名称查找
        for (TtsClient c : clients) {
            String providerName = c.getProviderName().toLowerCase();
            String searchName = name.toLowerCase();

            // 直接包含匹配
            if (providerName.contains(searchName)) {
                return c;
            }

            // 特殊匹配规则 - 科大讯飞
            if ("xunfei".equals(searchName) && (providerName.contains("科大讯飞") || providerName.contains("xunfei"))) {
                return c;
            }

            // 特殊匹配规则 - 火山引擎
            if ("volcan".equals(searchName) && (providerName.contains("火山") || providerName.contains("volcan"))) {
                return c;
            }
        }
        return null;
    }
}
