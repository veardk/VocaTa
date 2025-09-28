package com.vocata.conversation.constants;

/**
 * 会话状态枚举
 */
public enum ConversationStatus {
    /**
     * 活跃状态
     */
    ACTIVE(0, "ACTIVE"),

    /**
     * 已归档状态
     */
    ARCHIVED(1, "ARCHIVED");

    private final int code;
    private final String description;

    ConversationStatus(int code, String description) {
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
    public static ConversationStatus fromCode(int code) {
        for (ConversationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown conversation status code: " + code);
    }
}