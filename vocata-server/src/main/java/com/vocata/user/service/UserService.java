package com.vocata.user.service;

import com.vocata.user.dto.UpdateUserProfileRequest;
import com.vocata.user.dto.UserProfileResponse;

/**
 * 用户服务接口
 *
 * @author vocata
 * @since 2025-09-24
 */
public interface UserService {

    /**
     * 获取当前用户个人信息
     *
     * @return 用户个人信息
     */
    UserProfileResponse getCurrentUserProfile();

    /**
     * 更新当前用户个人信息
     *
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request);

    /**
     * 更新用户头像
     *
     * @param avatarUrl 头像URL
     * @return 更新后的用户信息
     */
    UserProfileResponse updateUserAvatar(String avatarUrl);
}