package com.vocata.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vocata.common.utils.UserContext;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.user.dto.UpdateUserProfileRequest;
import com.vocata.user.dto.UserProfileResponse;
import com.vocata.user.entity.User;
import com.vocata.user.mapper.UserMapper;
import com.vocata.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 *
 * @author vocata
 * @since 2025-09-24
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserProfileResponse getCurrentUserProfile() {
        Long userId = UserContext.getUserId();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ApiCode.USER_NOT_EXIST);
        }

        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .gender(user.getGender())
                .phone(user.getPhone())
                .birthday(user.getBirthday())
                .createDate(user.getCreateDate())
                .build();
    }

    @Override
    public UserProfileResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
        Long userId = UserContext.getUserId();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ApiCode.USER_NOT_EXIST);
        }

        // 更新用户信息
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }

        // 更新到数据库
        int updated = userMapper.updateById(user);
        if (updated == 0) {
            throw new BizException(ApiCode.ERROR);
        }

        log.info("用户 {} 更新个人信息成功", userId);

        // 返回更新后的用户信息
        return getCurrentUserProfile();
    }

    @Override
    public UserProfileResponse updateUserAvatar(String avatarUrl) {
        Long userId = UserContext.getUserId();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ApiCode.USER_NOT_EXIST);
        }

        user.setAvatar(avatarUrl);
        int updated = userMapper.updateById(user);
        if (updated == 0) {
            throw new BizException(ApiCode.ERROR);
        }

        log.info("用户 {} 更新头像成功: {}", userId, avatarUrl);

        return getCurrentUserProfile();
    }
}