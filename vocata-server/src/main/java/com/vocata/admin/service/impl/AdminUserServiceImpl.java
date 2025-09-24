package com.vocata.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.admin.dto.UserAdminResponse;
import com.vocata.admin.dto.UserQueryRequest;
import com.vocata.admin.service.AdminUserService;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.result.PageResult;
import com.vocata.user.entity.User;
import com.vocata.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理后台用户服务实现
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult<UserAdminResponse> getUserList(UserQueryRequest request) {
        Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 用户名模糊查询
        if (StringUtils.hasText(request.getUsername())) {
            wrapper.like(User::getUsername, request.getUsername());
        }

        // 邮箱模糊查询
        if (StringUtils.hasText(request.getEmail())) {
            wrapper.like(User::getEmail, request.getEmail());
        }

        // 状态筛选
        if (request.getStatus() != null) {
            wrapper.eq(User::getStatus, request.getStatus());
        }

        // 按创建时间倒序
        wrapper.orderByDesc(User::getCreateDate);

        IPage<User> userPage = userMapper.selectPage(page, wrapper);

        return PageResult.of(
            request.getPageNum(),
            request.getPageSize(),
            userPage.getTotal(),
            userPage.getRecords().stream()
                .map(this::convertToAdminResponse)
                .toList()
        );
    }

    @Override
    public UserAdminResponse getUserDetail(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "用户不存在");
        }

        return convertToAdminResponse(user);
    }

    @Override
    @Transactional
    public boolean updateUserStatus(String userId, Integer status) {
        // 验证状态值
        if (status != 1 && status != 2) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "无效的状态值");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "用户不存在");
        }

        User updateUser = new User();
        updateUser.setId(Long.valueOf(userId));
        updateUser.setStatus(status);

        int result = userMapper.updateById(updateUser);

        if (result > 0) {
            log.info("管理员更新用户状态成功，目标用户ID：{}，新状态：{}", userId, status);
        }

        return result > 0;
    }

    /**
     * 转换为管理后台响应DTO
     */
    private UserAdminResponse convertToAdminResponse(User user) {
        UserAdminResponse response = new UserAdminResponse();
        BeanUtils.copyProperties(user, response);
        response.setId(user.getId().toString());
        return response;
    }
}