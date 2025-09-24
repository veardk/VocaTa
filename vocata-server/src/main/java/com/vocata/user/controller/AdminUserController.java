package com.vocata.user.controller;

import com.vocata.common.result.ApiResponse;
import com.vocata.common.result.PageResult;
import com.vocata.common.utils.UserContext;
import com.vocata.user.dto.AdminUserListRequest;
import com.vocata.user.dto.AdminUserResponse;
import com.vocata.user.dto.UpdateUserStatusRequest;
import com.vocata.user.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台用户控制器
 * 提供用户管理相关功能
 *
 * @author vocata
 * @since 2025-09-24
 */
@RestController
@RequestMapping("/api/admin/user")
@Validated
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * 获取用户列表（分页查询，排除管理员用户）
     *
     * @param request 查询请求参数
     * @return 用户列表分页结果
     */
    @GetMapping("/list")
    public ApiResponse<PageResult<AdminUserResponse>> getUserList(@Validated AdminUserListRequest request) {
        // 检查管理员权限
        UserContext.checkAdmin();
        
        PageResult<AdminUserResponse> result = adminUserService.getUserList(request);
        return ApiResponse.success("获取用户列表成功", result);
    }

    /**
     * 获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情信息
     */
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUserDetail(@PathVariable Long userId) {
        // 检查管理员权限
        UserContext.checkAdmin();
        
        AdminUserResponse response = adminUserService.getUserDetail(userId);
        return ApiResponse.success("获取用户详情成功", response);
    }

    /**
     * 删除用户（软删除）
     *
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        // 检查管理员权限
        UserContext.checkAdmin();
        
        adminUserService.deleteUser(userId);
        return ApiResponse.success("删除用户成功");
    }

    /**
     * 切换用户账号状态（启用/禁用）
     *
     * @param userId 用户ID
     * @param request 状态更新请求
     * @return 更新结果
     */
    @PutMapping("/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId, 
                                             @RequestBody @Validated UpdateUserStatusRequest request) {
        // 检查管理员权限
        UserContext.checkAdmin();
        
        adminUserService.updateUserStatus(userId, request);
        return ApiResponse.success("更新用户状态成功");
    }
}
