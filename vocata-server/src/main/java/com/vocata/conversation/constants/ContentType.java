package com.vocata.conversation.constants;

/**
 * 消息内容类型枚举
 */
public enum ContentType {
    /**
     * 文本消息
     */
    TEXT(1, "TEXT"),

    /**
     * 图片消息
     */
    IMAGE(2, "IMAGE"),

    /**
     * 音频消息
     */
    AUDIO(3, "AUDIO");

    private final int code;
    private final String description;

    ContentType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举值
     */
    public static ContentType fromCode(int code) {
        for (ContentType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown content type code: " + code);
    }
}