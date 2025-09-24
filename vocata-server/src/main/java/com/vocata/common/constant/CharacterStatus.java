package com.vocata.common.constant;

/**
 * 角色状态常量类
 * 对应vocata_character表的status字段
 */
public class CharacterStatus {

    /**
     * 已发布
     */
    public static final int PUBLISHED = 1;

    /**
     * 审核中
     */
    public static final int UNDER_REVIEW = 2;

    /**
     * 已下架
     */
    public static final int OFFLINE = 3;

    /**
     * 获取状态名称
     * @param status 状态值
     * @return 状态名称
     */
    public static String getStatusName(int status) {
        switch (status) {
            case PUBLISHED:
                return "已发布";
            case UNDER_REVIEW:
                return "审核中";
            case OFFLINE:
                return "已下架";
            default:
                return "未知状态";
        }
    }

    /**
     * 检验状态值是否有效
     * @param status 状态值
     * @return 是否有效
     */
    public static boolean isValidStatus(int status) {
        return status == PUBLISHED || status == UNDER_REVIEW || status == OFFLINE;
    }
}