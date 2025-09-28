package com.vocata.auth.service;

import com.vocata.auth.dto.LoginRequest;
import com.vocata.auth.dto.LoginResponse;
import com.vocata.user.dto.UserRegisterRequest;
import com.vocata.user.dto.UserResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    UserResponse register(UserRegisterRequest request);

    /**
     * 用户登录（用户名/邮箱 + 密码）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 重置密码
     */
    void resetPassword(String email, String newPassword, String verificationCode);

    /**
     * 修改密码
     */
    void changePassword(String oldPassword, String newPassword);

    /**
     * 发送注册验证码
     */
    void sendRegisterCode(String email);

    /**
     * 发送重置密码验证码
     */
    void sendResetPasswordCode(String email);
}