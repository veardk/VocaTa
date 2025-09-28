package com.vocata.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

/**
 * JSON字段工具类
 * 用于处理PostgreSQL JSONB字段的验证和转换
 */
public class JsonFieldUtil {

    /**
     * 验证并格式化JSON字符串
     * @param jsonStr 原始JSON字符串
     * @return 格式化后的JSON字符串，如果为空则返回null
     */
    public static String validateAndFormat(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }

        // 如果是普通字符串"string"，转换为JSON格式
        if ("string".equals(jsonStr.trim())) {
            return null; // 忽略测试占位符
        }

        try {
            // 验证是否为有效JSON
            Object parsed = JSONUtil.parse(jsonStr);
            return JSONUtil.toJsonStr(parsed);
        } catch (Exception e) {
            // 如果不是有效JSON，将其作为字符串处理
            return JSONUtil.toJsonStr(jsonStr);
        }
    }

    /**
     * 验证JSON数组格式
     * @param jsonArrayStr JSON数组字符串
     * @return 格式化后的JSON数组字符串
     */
    public static String validateJsonArray(String jsonArrayStr) {
        if (StrUtil.isBlank(jsonArrayStr) || "string".equals(jsonArrayStr.trim())) {
            return "[]"; // 返回空数组
        }

        try {
            // 验证是否为有效JSON数组
            Object parsed = JSONUtil.parse(jsonArrayStr);
            if (parsed instanceof java.util.List) {
                return JSONUtil.toJsonStr(parsed);
            } else {
                // 如果不是数组，包装成数组
                return JSONUtil.toJsonStr(java.util.Arrays.asList(jsonArrayStr));
            }
        } catch (Exception e) {
            // 解析失败，包装成数组
            return JSONUtil.toJsonStr(java.util.Arrays.asList(jsonArrayStr));
        }
    }

    /**
     * 验证JSON对象格式
     * @param jsonObjStr JSON对象字符串
     * @return 格式化后的JSON对象字符串
     */
    public static String validateJsonObject(String jsonObjStr) {
        if (StrUtil.isBlank(jsonObjStr) || "string".equals(jsonObjStr.trim())) {
            return "{}"; // 返回空对象
        }

        try {
            // 验证是否为有效JSON对象
            Object parsed = JSONUtil.parse(jsonObjStr);
            if (parsed instanceof java.util.Map) {
                return JSONUtil.toJsonStr(parsed);
            } else {
                return "{}"; // 返回空对象
            }
        } catch (Exception e) {
            return "{}"; // 返回空对象
        }
    }
}