package com.vocata.config;

import com.vocata.ai.websocket.AiChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.context.annotation.Bean;

/**
 * MVC WebSocket配置类
 * 专门处理AI语音对话WebSocket连接
 * 端点: ws://localhost:9009/ws/chat/{conversation_uuid}
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AiChatWebSocketHandler aiChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册AI语音对话WebSocket处理器
        registry.addHandler(aiChatWebSocketHandler, "/ws/chat/**")
                .setAllowedOrigins("*");
    }

    /**
     * 配置WebSocket消息缓冲区大小
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 设置文本消息缓冲区为1MB
        container.setMaxTextMessageBufferSize(1024 * 1024);
        // 设置二进制消息缓冲区为5MB - 支持大音频文件
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        // 设置会话空闲超时为10分钟
        container.setMaxSessionIdleTimeout(10 * 60 * 1000L);
        return container;
    }
}