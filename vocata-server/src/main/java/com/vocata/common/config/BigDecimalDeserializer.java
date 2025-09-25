package com.vocata.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * BigDecimal自定义反序列化器
 * 支持从字符串形式的Java代码反序列化BigDecimal
 */
public class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        // 处理 "new BigDecimal("0.7")" 格式
        if (value.startsWith("new BigDecimal(\"") && value.endsWith("\")")) {
            String numberStr = value.substring(16, value.length() - 2);
            return new BigDecimal(numberStr);
        }

        // 处理普通数字字符串
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IOException("Cannot deserialize BigDecimal from: " + value, e);
        }
    }
}