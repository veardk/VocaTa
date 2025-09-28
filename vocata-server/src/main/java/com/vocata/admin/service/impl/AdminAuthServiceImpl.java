package com.vocata.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vocata.auth.constants.AuthConstants;
import com.vocata.auth.dto.LoginRequest;
import com.vocata.auth.dto.LoginResponse;
import com.vocata.admin.service.AdminAuthService;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.utils.IpUtils;
import com.vocata.common.utils.PasswordEncoder;
import com.vocata.user.dto.UserResponse;
import com.vocata.user.entity.User;
import com.vocata.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 管理员认证服务实现
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthServiceImpl.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HttpServletRequest request;

    @Override
    public LoginResponse adminLogin(LoginRequest loginRequest) {
        // 1. 查找用户
        User user = findUserByLoginName(loginRequest.getLoginName());
        if (user == null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        // 2. 检查是否为管理员
        if (!user.getIsAdmin()) {
            // 记录非管理员尝试登录管理后台的行为
            log.warn("非管理员用户尝试登录管理后台，用户ID：{}，用户名：{}，IP：{}",
                user.getId(), user.getUsername(), IpUtils.getClientIp(request));
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "权限不足，无法访问管理后台");
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // 增加登录失败次数
            incrementLoginFailedCount(user.getId());
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        // 4. 检查管理员账户状态
        checkAdminStatus(user);

        // 5. 重置登录失败次数
        resetLoginFailedCount(user.getId());

        // 6. 更新最后登录信息
        String clientIp = IpUtils.getClientIp(request);
        updateLastLoginInfo(user.getId(), clientIp);

        // 7. 生成Token并标记为管理员会话
        StpUtil.login(user.getId());

        // 8. 设置管理员Session信息
        setAdminSession(user);

        // 9. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setExpiresIn(StpUtil.getTokenTimeout());

        UserResponse userResponse = new UserResponse();
        BeanUtils.copyProperties(user, userResponse);
        userResponse.setId(user.getId().toString());
        response.setUser(userResponse);

        log.info("管理员登录成功，管理员ID：{}，用户名：{}，IP：{}", user.getId(), user.getUsername(), clientIp);
        return response;
    }

    @Override
    public void adminLogout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            log.info("管理员登出成功，管理员ID：{}", userId);
        }
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // Sa-Token会自动处理Token刷新
        if (!StpUtil.isLogin()) {
            throw new BizException(ApiCode.UNAUTHORIZED.getCode(), "请重新登录");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = getUserById(userId);
        if (user == null) {
            throw new BizException(ApiCode.UNAUTHORIZED.getCode(), "用户不存在");
        }

        // 检查是否为管理员
        if (!user.getIsAdmin()) {
            StpUtil.logout();
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "权限不足");
        }

        // 检查管理员状态
        checkAdminStatus(user);

        // 刷新Token
        StpUtil.renewTimeout(StpUtil.getTokenTimeout());

        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setExpiresIn(StpUtil.getTokenTimeout());

        UserResponse userResponse = new UserResponse();
        BeanUtils.copyProperties(user, userResponse);
        userResponse.setId(user.getId().toString());
        response.setUser(userResponse);

        return response;
    }

    @Override
    public LoginResponse getCurrentAdmin() {
        if (!StpUtil.isLogin()) {
            throw new BizException(ApiCode.UNAUTHORIZED.getCode(), "未登录");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = getUserById(userId);
        if (user == null || !user.getIsAdmin()) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "权限不足");
        }

        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setExpiresIn(StpUtil.getTokenTimeout());

        UserResponse userResponse = new UserResponse();
        BeanUtils.copyProperties(user, userResponse);
        userResponse.setId(user.getId().toString());
        response.setUser(userResponse);

        return response;
    }

    /**
     * 根据登录名查找用户
     */
    private User findUserByLoginName(String loginName) {
        // 判断是邮箱还是用户名
        if (EMAIL_PATTERN.matcher(loginName).matches()) {
            return getUserByEmail(loginName);
        } else {
            return getUserByUsername(loginName);
        }
    }

    /**
     * 检查管理员状态
     */
    private void checkAdminStatus(User user) {
        if (user.getStatus() == AuthConstants.USER_STATUS_DISABLED) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "管理员账号已被禁用");
        }

        if (user.getStatus() == AuthConstants.USER_STATUS_LOCKED) {
            // 检查锁定时间是否已过期
            if (user.getLockTime() != null &&
                user.getLockTime().plusMinutes(AuthConstants.ACCOUNT_LOCK_MINUTES).isAfter(LocalDateTime.now())) {
                throw new BizException(ApiCode.FORBIDDEN.getCode(), "管理员账号已被锁定，请稍后再试");
            } else {
                // 锁定时间已过，自动解锁
                unlockUserAccount(user.getId());
            }
        }
    }

    /**
     * 设置管理员Session信息
     */
    private void setAdminSession(User user) {
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("email", user.getEmail());
        StpUtil.getSession().set("isAdmin", true);
        StpUtil.getSession().set("loginTime", System.currentTimeMillis());
        StpUtil.getSession().set("loginIp", IpUtils.getClientIp(request));
        StpUtil.getSession().set("loginType", "admin"); // 标记为管理员登录
    }

    // ============ 用户数据访问辅助方法 ============

    /**
     * 根据邮箱查找用户
     */
    private User getUserByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 根据用户名查找用户
     */
    private User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 根据ID查找用户
     */
    private User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 更新最后登录信息
     */
    private void updateLastLoginInfo(Long userId, String clientIp) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.updateById(user);
    }

    /**
     * 增加登录失败次数
     */
    private void incrementLoginFailedCount(Long userId) {
        User user = getUserById(userId);
        if (user != null) {
            user.setLoginFailCount(user.getLoginFailCount() + 1);
            userMapper.updateById(user);
        }
    }

    /**
     * 重置登录失败次数
     */
    private void resetLoginFailedCount(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLoginFailCount(0);
        userMapper.updateById(user);
    }

    /**
     * 解锁用户账户
     */
    private void unlockUserAccount(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setStatus(AuthConstants.USER_STATUS_NORMAL);
        user.setLoginFailCount(0);
        user.setLockTime(null);
        userMapper.updateById(user);
    }
}