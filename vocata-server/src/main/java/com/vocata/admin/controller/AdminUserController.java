package com.vocata.admin.controller;

import com.vocata.admin.dto.UserAdminResponse;
import com.vocata.admin.dto.UserQueryRequest;
import com.vocata.admin.dto.UserStatusUpdateRequest;
import com.vocata.admin.service.AdminUserService;
import com.vocata.common.result.ApiResponse;
import com.vocata.common.result.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 管理后台用户管理控制器
 */
@RestController
@RequestMapping("/api/admin/user")
@Validated
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    public ApiResponse<PageResult<UserAdminResponse>> getUserList(UserQueryRequest request) {
        PageResult<UserAdminResponse> result = adminUserService.getUserList(request);
        return ApiResponse.success("获取用户列表成功", result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<UserAdminResponse> getUserDetail(@PathVariable String id) {
        UserAdminResponse user = adminUserService.getUserDetail(id);
        return ApiResponse.success("获取用户详情成功", user);
    }

    /**
     * 切换用户状态
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Boolean> updateUserStatus(@PathVariable String id,
                                                @Valid @RequestBody UserStatusUpdateRequest request) {
        boolean result = adminUserService.updateUserStatus(id, request.getStatus());
        return ApiResponse.success("用户状态更新成功", result);
    }
}