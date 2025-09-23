package com.vocata.common.utils;

import java.util.UUID;

/**
 * 设备ID工具类
 */
public class DeviceIdUtils {

    /**
     * 生成UUID格式的设备ID
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成带前缀的设备ID
     */
    public static String generateWithPrefix(String prefix) {
        return prefix + "_" + generateUUID();
    }

    /**
     * 验证设备ID格式
     */
    public static boolean isValidDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return false;
        }

        // 基本长度检查
        String trimmed = deviceId.trim();
        return trimmed.length() >= 8 && trimmed.length() <= 64;
    }
}