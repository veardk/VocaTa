package com.vocata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许所有来源
        config.addAllowedOriginPattern("*");

        // 允许所有HTTP方法
        config.addAllowedMethod("*");

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许携带凭证
        config.setAllowCredentials(true);

        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    /**
     * JSON序列化配置
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 创建时间模块并配置LocalDateTime序列化格式
        JavaTimeModule timeModule = new JavaTimeModule();
        
        // 配置LocalDateTime的序列化格式为ISO-8601标准格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        
        // 注册时间模块
        mapper.registerModule(timeModule);

        // 禁用时间戳序列化，使用字符串格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 使用驼峰命名策略
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        return mapper;
    }
}
