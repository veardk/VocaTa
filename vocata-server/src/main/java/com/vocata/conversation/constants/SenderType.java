package com.vocata.conversation.constants;

/**
 * 消息发送方类型枚举
 */
public enum SenderType {
    /**
     * 用户发送的消息
     */
    USER(1, "USER"),

    /**
     * AI角色发送的消息
     */
    CHARACTER(2, "CHARACTER");

    private final int code;
    private final String description;

    SenderType(int code, String description) {
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
    public static SenderType fromCode(int code) {
        for (SenderType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sender type code: " + code);
    }
}