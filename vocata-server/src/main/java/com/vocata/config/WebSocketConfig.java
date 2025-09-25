package com.vocata.config;

import com.vocata.ai.websocket.AiChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket配置类
 * 配置AI聊天的WebSocket路由和处理器
 */
@Configuration
public class WebSocketConfig {

    @Autowired
    private AiChatWebSocketHandler aiChatWebSocketHandler;

    /**
     * WebSocket路由映射
     */
    @Bean
    public HandlerMapping handlerMapping() {
        Map<String, WebSocketHandler> urlMap = new HashMap<>();

        // AI聊天WebSocket端点
        // 路径格式: ws://localhost:9009/ws/chat/{conversation_uuid}
        urlMap.put("/ws/chat/*", aiChatWebSocketHandler);

        // 可以添加其他WebSocket端点
        // urlMap.put("/ws/admin/*", adminWebSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(urlMap);
        mapping.setOrder(-1); // 设置高优先级

        return mapping;
    }

    /**
     * WebSocket处理器适配器
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}