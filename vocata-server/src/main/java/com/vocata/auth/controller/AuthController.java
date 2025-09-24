package com.vocata.auth.controller;

import com.vocata.auth.dto.LoginRequest;
import com.vocata.auth.dto.LoginResponse;
import com.vocata.auth.service.AuthService;
import com.vocata.common.result.ApiResponse;
import com.vocata.user.dto.UserRegisterRequest;
import com.vocata.user.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/client/auth")
@Validated
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse response = authService.register(request);
        return ApiResponse.success("用户注册成功", response);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success("登出成功");
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody String refreshToken) {
        LoginResponse response = authService.refreshToken(refreshToken);
        return ApiResponse.success("Token刷新成功", response);
    }

    /**
     * 发送注册验证码
     */
    @PostMapping("/send-register-code")
    public ApiResponse<Void> sendRegisterCode(@RequestParam @NotBlank @Email String email) {
        authService.sendRegisterCode(email);
        return ApiResponse.success("验证码发送成功");
    }

    /**
     * 发送重置密码验证码
     */
    @PostMapping("/send-reset-code")
    public ApiResponse<Void> sendResetPasswordCode(@RequestParam @NotBlank @Email String email) {
        authService.sendResetPasswordCode(email);
        return ApiResponse.success("重置密码验证码发送成功");
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank String newPassword,
            @RequestParam @NotBlank String verificationCode) {
        authService.resetPassword(email, newPassword, verificationCode);
        return ApiResponse.success("密码重置成功");
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @RequestParam @NotBlank String oldPassword,
            @RequestParam @NotBlank String newPassword) {
        authService.changePassword(oldPassword, newPassword);
        return ApiResponse.success("密码修改成功");
    }
}