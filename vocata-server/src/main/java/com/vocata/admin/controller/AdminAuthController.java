package com.vocata.admin.controller;

import com.vocata.auth.dto.LoginRequest;
import com.vocata.auth.dto.LoginResponse;
import com.vocata.admin.service.AdminAuthService;
import com.vocata.common.result.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 管理员认证控制器
 */
@RestController
@RequestMapping("/api/admin/auth")
@Validated
public class AdminAuthController {

    @Autowired
    private AdminAuthService adminAuthService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = adminAuthService.adminLogin(request);
        return ApiResponse.success("管理员登录成功", response);
    }

    /**
     * 管理员登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        adminAuthService.adminLogout();
        return ApiResponse.success("管理员登出成功");
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody String refreshToken) {
        LoginResponse response = adminAuthService.refreshToken(refreshToken);
        return ApiResponse.success("Token刷新成功", response);
    }

    /**
     * 获取当前管理员信息
     */
    @GetMapping("/current")
    public ApiResponse<LoginResponse> getCurrentAdmin() {
        LoginResponse response = adminAuthService.getCurrentAdmin();
        return ApiResponse.success("获取管理员信息成功", response);
    }
}