package com.vocata.user.constants;

/**
 * 用户相关常量定义
 *
 * @author vocata
 * @since 2025-09-24
 */
public class UserConstants {

    /**
     * 用户状态
     */
    public static class Status {
        /** 正常 */
        public static final Integer NORMAL = 1;
        /** 禁用 */
        public static final Integer DISABLED = 2;
    }

    /**
     * 性别
     */
    public static class Gender {
        /** 未知 */
        public static final Integer UNKNOWN = 0;
        /** 男 */
        public static final Integer MALE = 1;
        /** 女 */
        public static final Integer FEMALE = 2;
    }

    /**
     * 软删除标记
     */
    public static class DeleteFlag {
        /** 未删除 */
        public static final Integer NOT_DELETED = 0;
        /** 已删除 */
        public static final Integer DELETED = 1;
    }

    /**
     * 管理员标记
     */
    public static class AdminFlag {
        /** 普通用户 */
        public static final Boolean NORMAL_USER = false;
        /** 管理员 */
        public static final Boolean ADMIN = true;
    }
}
