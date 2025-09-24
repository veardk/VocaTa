package com.vocata.user.service;

import com.vocata.common.result.PageResult;
import com.vocata.user.dto.AdminUserListRequest;
import com.vocata.user.dto.AdminUserResponse;
import com.vocata.user.dto.UpdateUserStatusRequest;

/**
 * 管理后台用户服务接口
 *
 * @author vocata
 * @since 2025-09-24
 */
public interface AdminUserService {

    /**
     * 分页获取用户列表（排除管理员）
     *
     * @param request 查询请求
     * @return 用户列表分页结果
     */
    PageResult<AdminUserResponse> getUserList(AdminUserListRequest request);

    /**
     * 获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    AdminUserResponse getUserDetail(Long userId);

    /**
     * 软删除用户
     *
     * @param userId 用户ID
     */
    void deleteUser(Long userId);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param request 状态更新请求
     */
    void updateUserStatus(Long userId, UpdateUserStatusRequest request);
}
