package com.vocata.character.constants;

import java.math.BigDecimal;

/**
 * 角色管理常量类
 */
public class CharacterConstants {

    /**
     * 默认配置值
     */
    public static final int DEFAULT_CONTEXT_WINDOW = 10;
    public static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.7");
    public static final String DEFAULT_LANGUAGE = "zh-CN";
    public static final int DEFAULT_SORT_WEIGHT = 0;

    /**
     * 角色编码正则表达式
     */
    public static final String CHARACTER_CODE_PATTERN = "^[a-zA-Z0-9_-]{3,50}$";

    /**
     * 字段长度限制
     */
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_GREETING_LENGTH = 1000;
    public static final int MAX_PERSONA_LENGTH = 5000;
    public static final int MAX_SPEAKING_STYLE_LENGTH = 1000;
    public static final int MAX_SEARCH_KEYWORDS_LENGTH = 500;

    /**
     * JSON字段长度限制
     */
    public static final int MAX_PERSONALITY_TRAITS_LENGTH = 2000;
    public static final int MAX_TAGS_LENGTH = 1000;
    public static final int MAX_TAG_WEIGHTS_LENGTH = 2000;
    public static final int MAX_EXAMPLE_DIALOGUES_LENGTH = 10000;

    /**
     * 参数范围
     */
    public static final BigDecimal MIN_TEMPERATURE = new BigDecimal("0.0");
    public static final BigDecimal MAX_TEMPERATURE = new BigDecimal("2.0");
    public static final int MIN_CONTEXT_WINDOW = 1;
    public static final int MAX_CONTEXT_WINDOW = 100;

    /**
     * 热门角色相关
     */
    public static final int DEFAULT_TRENDING_LIMIT = 10;
    public static final int MAX_TRENDING_LIMIT = 50;
    public static final int DEFAULT_FEATURED_LIMIT = 10;
    public static final int MAX_FEATURED_LIMIT = 50;

    /**
     * 布尔值常量（数据库存储）
     */
    public static final int BOOLEAN_FALSE = 0;
    public static final int BOOLEAN_TRUE = 1;

    /**
     * 支持的语言列表
     */
    public static final String[] SUPPORTED_LANGUAGES = {
        "zh-CN",    // 简体中文
        "en-US",    // 英语
        "ja-JP",    // 日语
        "ko-KR"     // 韩语
    };

    /**
     * 默认分页参数
     */
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 批量操作限制
     */
    public static final int MAX_BATCH_SIZE = 100;

    private CharacterConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 检查语言是否受支持
     */
    public static boolean isSupportedLanguage(String language) {
        if (language == null) {
            return false;
        }
        for (String supportedLang : SUPPORTED_LANGUAGES) {
            if (supportedLang.equals(language)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查温度参数是否有效
     */
    public static boolean isValidTemperature(BigDecimal temperature) {
        return temperature != null &&
               temperature.compareTo(MIN_TEMPERATURE) >= 0 &&
               temperature.compareTo(MAX_TEMPERATURE) <= 0;
    }

    /**
     * 检查上下文窗口是否有效
     */
    public static boolean isValidContextWindow(Integer contextWindow) {
        return contextWindow != null &&
               contextWindow >= MIN_CONTEXT_WINDOW &&
               contextWindow <= MAX_CONTEXT_WINDOW;
    }

    /**
     * 检查角色编码格式是否有效
     */
    public static boolean isValidCharacterCode(String characterCode) {
        return characterCode != null && characterCode.matches(CHARACTER_CODE_PATTERN);
    }
}