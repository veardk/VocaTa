package com.vocata.admin.service;

import com.vocata.admin.dto.UserAdminResponse;
import com.vocata.admin.dto.UserQueryRequest;
import com.vocata.common.result.PageResult;

/**
 * 管理后台用户服务接口
 *
 * @author vocata
 * @since 2025-09-24
 */
public interface AdminUserService {

    /**
     * 获取用户列表（分页查询）
     *
     * @param queryRequest 查询条件
     * @return 用户列表分页结果
     */
    PageResult<UserAdminResponse> getUserList(UserQueryRequest queryRequest);

    /**
     * 根据用户ID获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情信息
     */
    UserAdminResponse getUserDetail(String userId);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 用户状态 (1:正常 2:禁用)
     * @return 是否更新成功
     */
    boolean updateUserStatus(String userId, Integer status);
}