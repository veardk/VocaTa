package com.vocata.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import cn.hutool.json.JSONUtil;

import java.io.IOException;

/**
 * JSON数组字符串反序列化器
 * 将JSON数组或字符串转换为JSON字符串存储
 */
public class JsonArrayDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node == null || node.isNull()) {
            return "[]";
        }

        if (node.isArray()) {
            // 如果是JSON数组，直接转换为字符串
            return node.toString();
        } else if (node.isTextual()) {
            // 如果是字符串，尝试解析为JSON
            String text = node.textValue();
            if ("string".equals(text) || text == null || text.trim().isEmpty()) {
                return "[]";
            }
            try {
                // 验证是否为有效JSON
                Object parsed = JSONUtil.parse(text);
                return JSONUtil.toJsonStr(parsed);
            } catch (Exception e) {
                // 如果不是有效JSON，包装成数组
                return JSONUtil.toJsonStr(new String[]{text});
            }
        } else {
            // 其他类型，转换为字符串后包装成数组
            return JSONUtil.toJsonStr(new String[]{node.toString()});
        }
    }
}