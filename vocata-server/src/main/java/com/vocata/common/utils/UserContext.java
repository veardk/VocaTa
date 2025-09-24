package com.vocata.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;

/**
 * 用户上下文 - 基于ThreadLocal存储用户信息
 * 可在任何地方安全获取当前登录用户信息
 */
public class UserContext {

    private static final ThreadLocal<UserContextDTO> USER_CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置用户上下文
     */
    public static void set(UserContextDTO userContext) {
        USER_CONTEXT_HOLDER.set(userContext);
    }

    /**
     * 获取用户上下文
     */
    public static UserContextDTO get() {
        UserContextDTO userContext = USER_CONTEXT_HOLDER.get();
        if (userContext == null) {
            throw new BizException(ApiCode.UNAUTHORIZED.getCode(), "用户未登录");
        }
        return userContext;
    }

    /**
     * 安全获取用户上下文
     */
    public static UserContextDTO getSafely() {
        UserContextDTO userContext = USER_CONTEXT_HOLDER.get();
        if (userContext == null) {
            // 返回访客用户上下文
            return UserContextDTO.guest();
        }
        return userContext;
    }

    /**
     * 获取用户上下文（可为空）
     */
    public static UserContextDTO getOrNull() {
        return USER_CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return get().getUserId();
    }

    /**
     * 安全获取用户ID（未登录返回访客ID）
     */
    public static Long getUserIdSafely() {
        UserContextDTO userContext = getSafely();
        return userContext.getUserId();
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return get().getUsername();
    }

    /**
     * 判断是否为管理员
     */
    public static boolean isAdmin() {
        UserContextDTO userContext = getOrNull();
        return userContext != null && userContext.getIsAdmin();
    }

    /**
     * 检查管理员权限（不满足抛异常）
     */
    public static void checkAdmin() {
        if (!isAdmin()) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "需要管理员权限");
        }
    }

    /**
     * 检查非管理员权限（管理员不能访问普通用户功能）
     */
    public static void checkNotAdmin() {
        if (isAdmin()) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "管理员不能访问此功能，请使用管理后台");
        }
    }

    /**
     * 检查是否为当前用户或管理员
     */
    public static void checkUserOrAdmin(Long userId) {
        UserContextDTO userContext = getOrNull();
        if (userContext == null) {
            throw new BizException(ApiCode.UNAUTHORIZED.getCode(), "用户未登录");
        }

        if (!userContext.getUserId().equals(userId) && !userContext.getIsAdmin()) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "权限不足");
        }
    }

    /**
     * 清除用户上下文
     */
    public static void clear() {
        USER_CONTEXT_HOLDER.remove();
    }

    /**
     * 判断是否已登录
     */
    public static boolean isLogin() {
        return USER_CONTEXT_HOLDER.get() != null;
    }

    /**
     * 用户上下文DTO
     */
    public static class UserContextDTO {
        private Long userId;
        private String username;
        private Boolean isAdmin;
        private String email;

        public UserContextDTO() {}

        public UserContextDTO(Long userId, String username, Boolean isAdmin, String email) {
            this.userId = userId;
            this.username = username;
            this.isAdmin = isAdmin;
            this.email = email;
        }

        public static UserContextDTO guest() {
            return new UserContextDTO(-1L, "guest", false, null);
        }

        // Getters and Setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Boolean getIsAdmin() {
            return isAdmin;
        }

        public void setIsAdmin(Boolean isAdmin) {
            this.isAdmin = isAdmin;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}