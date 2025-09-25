package com.vocata.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;

/**
 * Long类型自定义反序列化器
 * 支持从字符串反序列化为Long类型
 */
public class LongDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IOException("Cannot deserialize Long from: " + value, e);
        }
    }
}