package com.vocata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient配置
 * 用于AI服务的HTTP调用
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> {
                    // 增加内存缓冲区大小，支持大的响应
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
                });
    }
}