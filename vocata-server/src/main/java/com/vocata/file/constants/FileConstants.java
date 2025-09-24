package com.vocata.file.constants;

/**
 * 文件相关常量
 */
public class FileConstants {

    public static final String AVATAR_PREFIX = "avatar/";

    public static final String[] ALLOWED_IMAGE_TYPES = {
        "image/jpeg", "image/png", "image/gif", "image/webp"
    };

    public static final String[] ALLOWED_IMAGE_EXTENSIONS = {
        ".jpg", ".jpeg", ".png", ".gif", ".webp"
    };

    public static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB
}