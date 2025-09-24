package com.vocata.admin.service;

import com.vocata.auth.dto.LoginRequest;
import com.vocata.auth.dto.LoginResponse;

/**
 * 管理员认证服务接口
 */
public interface AdminAuthService {

    /**
     * 管理员登录
     */
    LoginResponse adminLogin(LoginRequest loginRequest);

    /**
     * 管理员登出
     */
    void adminLogout();

    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 获取当前管理员信息
     */
    LoginResponse getCurrentAdmin();
}