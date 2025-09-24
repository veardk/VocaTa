package com.vocata.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.result.PageResult;
import com.vocata.common.utils.UserContext;
import com.vocata.user.dto.AdminUserListRequest;
import com.vocata.user.dto.AdminUserResponse;
import com.vocata.user.dto.UpdateUserStatusRequest;
import com.vocata.user.entity.User;
import com.vocata.user.mapper.UserMapper;
import com.vocata.user.service.AdminUserService;
import com.vocata.user.constants.UserConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台用户服务实现
 *
 * @author vocata
 * @since 2025-09-24
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult<AdminUserResponse> getUserList(AdminUserListRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getIsAdmin, UserConstants.AdminFlag.NORMAL_USER) // 排除管理员用户
                .eq(request.getStatus() != null, User::getStatus, request.getStatus())
                .eq(request.getGender() != null, User::getGender, request.getGender())
                .and(StringUtils.hasText(request.getKeyword()), wrapper ->
                        wrapper.like(User::getUsername, request.getKeyword())
                                .or().like(User::getNickname, request.getKeyword())
                                .or().like(User::getEmail, request.getKeyword())
                );

        // 设置排序
        if (StringUtils.hasText(request.getOrderBy())) {
            boolean isAsc = "asc".equalsIgnoreCase(request.getOrderDirection());
            switch (request.getOrderBy()) {
                case "create_date":
                    queryWrapper.orderBy(true, isAsc, User::getCreateDate);
                    break;
                case "last_login_time":
                    queryWrapper.orderBy(true, isAsc, User::getLastLoginTime);
                    break;
                default:
                    queryWrapper.orderByDesc(User::getCreateDate);
            }
        } else {
            queryWrapper.orderByDesc(User::getCreateDate);
        }

        // 分页查询
        Page<User> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<User> userPage = userMapper.selectPage(page, queryWrapper);

        // 转换为响应对象
        List<AdminUserResponse> userList = userPage.getRecords().stream()
                .map(this::convertToAdminUserResponse)
                .collect(Collectors.toList());

        return PageResult.of(
                request.getPageNum(),
                request.getPageSize(),
                userPage.getTotal(),
                userList
        );
    }

    @Override
    public AdminUserResponse getUserDetail(Long userId) {
        if (userId == null) {
            throw new BizException(ApiCode.PARAM_ERROR.getCode(), "用户ID不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDelete().equals(UserConstants.DeleteFlag.DELETED)) {
            throw new BizException(ApiCode.USER_NOT_EXIST.getCode(), "用户不存在");
        }

        // 管理员不能查看其他管理员的详情（除非是自己）
        if (user.getIsAdmin() && !user.getId().equals(UserContext.getUserId())) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "无法查看管理员用户信息");
        }

        return convertToAdminUserResponse(user);
    }

    @Override
    public void deleteUser(Long userId) {
        if (userId == null) {
            throw new BizException(ApiCode.PARAM_ERROR.getCode(), "用户ID不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDelete().equals(UserConstants.DeleteFlag.DELETED)) {
            throw new BizException(ApiCode.USER_NOT_EXIST.getCode(), "用户不存在");
        }

        // 不能删除管理员用户
        if (user.getIsAdmin()) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "不能删除管理员用户");
        }

        // 不能删除自己
        if (user.getId().equals(UserContext.getUserId())) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "不能删除自己");
        }

        // 执行软删除
        user.setIsDelete(UserConstants.DeleteFlag.DELETED);
        user.setUpdateId(UserContext.getUserId());
        user.setUpdateDate(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        if (userId == null) {
            throw new BizException(ApiCode.PARAM_ERROR.getCode(), "用户ID不能为空");
        }

        // 验证状态值
        if (!Arrays.asList(UserConstants.Status.NORMAL, UserConstants.Status.DISABLED).contains(request.getStatus())) {
            throw new BizException(ApiCode.PARAM_ERROR.getCode(), "用户状态值无效");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDelete().equals(UserConstants.DeleteFlag.DELETED)) {
            throw new BizException(ApiCode.USER_NOT_EXIST.getCode(), "用户不存在");
        }

        // 不能修改管理员用户状态
        if (user.getIsAdmin()) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "不能修改管理员用户状态");
        }

        // 不能修改自己的状态
        if (user.getId().equals(UserContext.getUserId())) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "不能修改自己的状态");
        }

        // 更新状态
        user.setStatus(request.getStatus());
        user.setUpdateId(UserContext.getUserId());
        user.setUpdateDate(LocalDateTime.now());

        // 如果是禁用状态，清除锁定时间和登录失败次数
        if (request.getStatus().equals(UserConstants.Status.DISABLED)) {
            user.setLockTime(null);
            user.setLoginFailCount(0);
        }

        userMapper.updateById(user);
    }

    /**
     * 转换为管理后台用户响应对象
     */
    private AdminUserResponse convertToAdminUserResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        BeanUtils.copyProperties(user, response);
        return response;
    }
}
