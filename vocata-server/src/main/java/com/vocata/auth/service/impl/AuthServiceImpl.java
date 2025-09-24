package com.vocata.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vocata.auth.constants.AuthConstants;
import com.vocata.auth.dto.LoginRequest;
import com.vocata.auth.dto.LoginResponse;
import com.vocata.auth.service.AuthService;
import com.vocata.auth.service.VerificationCodeService;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.utils.IpUtils;
import com.vocata.common.utils.PasswordEncoder;
import com.vocata.user.dto.UserRegisterRequest;
import com.vocata.user.dto.UserResponse;
import com.vocata.user.entity.User;
import com.vocata.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 认证服务实现
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest registerRequest) {
        // 1. 验证参数
        validateRegisterRequest(registerRequest);

        // 2. 先验证验证码
        if (!verificationCodeService.verifyCode(
                registerRequest.getEmail(),
                registerRequest.getVerificationCode(),
                AuthConstants.VERIFICATION_CODE_TYPE_REGISTER)) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "验证码错误或已过期");
        }

        // 3. 获取邮箱注册锁，防止并发注册
        String registrationLock = acquireRegistrationLock(registerRequest.getEmail());
        try {
            // 4. 再次检查用户名和邮箱是否已存在（防止并发创建）
            checkUserExists(registerRequest.getUsername(), registerRequest.getEmail());

            // 5. 创建用户
            User user = createUser(registerRequest);
            userMapper.insert(user);

            // 6. 用户创建成功后，删除验证码
            verificationCodeService.useCode(registerRequest.getEmail(),
                AuthConstants.VERIFICATION_CODE_TYPE_REGISTER);

            // 7. 返回用户信息
            UserResponse response = new UserResponse();
            BeanUtils.copyProperties(user, response);

            log.info("用户注册成功，用户ID：{}，用户名：{}，邮箱：{}", user.getId(), user.getUsername(), user.getEmail());
            return response;
        } finally {
            // 8. 释放注册锁
            releaseRegistrationLock(registrationLock);
        }
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 1. 查找用户
        User user = findUserByLoginName(loginRequest.getLoginName());
        if (user == null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // 增加登录失败次数
            incrementLoginFailedCount(user.getId());
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "用户名或密码错误");
        }

        // 3. 检查用户状态
        checkUserStatus(user);

        // 4. 重置登录失败次数
        resetLoginFailedCount(user.getId());

        // 5. 更新最后登录信息
        String clientIp = IpUtils.getClientIp(request);
        updateLastLoginInfo(user.getId(), clientIp);

        // 6. 生成Token
        StpUtil.login(user.getId());

        // 7. 设置Session信息
        setUserSession(user);

        // 8. 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setExpiresIn(StpUtil.getTokenTimeout());

        UserResponse userResponse = new UserResponse();
        BeanUtils.copyProperties(user, userResponse);
        response.setUser(userResponse);

        log.info("用户登录成功，用户ID：{}，用户名：{}，IP：{}", user.getId(), user.getUsername(), clientIp);
        return response;
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            log.info("用户登出成功，用户ID：{}", userId);
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

        // 检查用户状态
        checkUserStatus(user);

        // 刷新Token
        StpUtil.renewTimeout(StpUtil.getTokenTimeout());

        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setExpiresIn(StpUtil.getTokenTimeout());

        UserResponse userResponse = new UserResponse();
        BeanUtils.copyProperties(user, userResponse);
        response.setUser(userResponse);

        return response;
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword, String verificationCode) {
        // 1. 验证验证码
        if (!verificationCodeService.verifyAndUseCode(
                email, verificationCode, AuthConstants.VERIFICATION_CODE_TYPE_RESET_PASSWORD)) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "验证码错误或已过期");
        }

        // 2. 查找用户
        User user = getUserByEmail(email);
        if (user == null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "用户不存在");
        }

        // 3. 更新密码
        String encodedPassword = passwordEncoder.encode(newPassword);
        updatePassword(user.getId(), encodedPassword);

        // 4. 强制登出所有设备
        StpUtil.kickout(user.getId());

        log.info("密码重置成功，用户ID：{}，邮箱：{}", user.getId(), email);
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = getUserById(userId);

        if (user == null) {
            throw new BizException(ApiCode.UNAUTHORIZED.getCode(), "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "原密码错误");
        }

        // 更新密码
        String encodedPassword = passwordEncoder.encode(newPassword);
        updatePassword(userId, encodedPassword);

        log.info("密码修改成功，用户ID：{}", userId);
    }

    @Override
    public void sendRegisterCode(String email) {
        // 检查邮箱是否已被注册
        if (getUserByEmail(email) != null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "邮箱已被注册");
        }

        verificationCodeService.sendRegisterCode(email);
    }

    @Override
    public void sendResetPasswordCode(String email) {
        // 检查用户是否存在
        if (getUserByEmail(email) == null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "邮箱未注册");
        }

        verificationCodeService.sendResetPasswordCode(email);
    }

    /**
     * 验证注册请求参数
     */
    private void validateRegisterRequest(UserRegisterRequest request) {
        // 验证密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "两次输入的密码不一致");
        }

        // 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "邮箱格式不正确");
        }

        // 验证密码强度
        if (request.getPassword().length() < AuthConstants.PASSWORD_MIN_LENGTH) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(),
                "密码长度不能少于" + AuthConstants.PASSWORD_MIN_LENGTH + "位");
        }
    }

    /**
     * 检查用户是否已存在
     */
    private void checkUserExists(String username, String email) {
        if (getUserByUsername(username) != null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        if (getUserByEmail(email) != null) {
            throw new BizException(ApiCode.BAD_REQUEST.getCode(), "邮箱已被注册");
        }
    }

    /**
     * 创建用户
     */
    private User createUser(UserRegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ?
            request.getNickname() : request.getUsername());
        user.setGender(request.getGender() != null ? request.getGender() : AuthConstants.GENDER_UNSET);
        user.setStatus(AuthConstants.USER_STATUS_NORMAL);
        user.setIsAdmin(false);
        user.setLoginFailCount(0);

        return user;
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
     * 检查用户状态
     */
    private void checkUserStatus(User user) {
        if (user.getStatus() == AuthConstants.USER_STATUS_DISABLED) {
            throw new BizException(ApiCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        if (user.getStatus() == AuthConstants.USER_STATUS_LOCKED) {
            // 检查锁定时间是否已过期
            if (user.getLockTime() != null &&
                user.getLockTime().plusMinutes(AuthConstants.ACCOUNT_LOCK_MINUTES).isAfter(LocalDateTime.now())) {
                throw new BizException(ApiCode.FORBIDDEN.getCode(), "账号已被锁定，请稍后再试");
            } else {
                // 锁定时间已过，自动解锁
                unlockUserAccount(user.getId());
            }
        }
    }

    /**
     * 设置用户Session信息
     */
    private void setUserSession(User user) {
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("email", user.getEmail());
        StpUtil.getSession().set("isAdmin", user.getIsAdmin());
        StpUtil.getSession().set("loginTime", System.currentTimeMillis());
        StpUtil.getSession().set("loginIp", IpUtils.getClientIp(request));
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
     * 更新密码
     */
    private void updatePassword(Long userId, String encodedPassword) {
        User user = new User();
        user.setId(userId);
        user.setPassword(encodedPassword);
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

    // ============ 注册锁相关方法 ============

    private static final String REGISTRATION_LOCK_PREFIX = "auth:register_lock:";
    private static final int REGISTRATION_LOCK_SECONDS = 30; // 注册锁30秒超时

    /**
     * 获取邮箱注册锁
     */
    private String acquireRegistrationLock(String email) {
        String lockKey = REGISTRATION_LOCK_PREFIX + email;
        String lockValue = String.valueOf(System.currentTimeMillis());

        // 尝试获取锁，30秒超时
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue,
            REGISTRATION_LOCK_SECONDS, TimeUnit.SECONDS);

        if (!acquired) {
            throw new BizException(ApiCode.TOO_MANY_REQUESTS.getCode(),
                "该邮箱正在注册中，请稍后再试");
        }

        log.debug("获取注册锁成功，邮箱：{}，锁值：{}", email, lockValue);
        return lockKey;
    }

    /**
     * 释放邮箱注册锁
     */
    private void releaseRegistrationLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
            log.debug("释放注册锁成功，锁键：{}", lockKey);
        } catch (Exception e) {
            log.warn("释放注册锁失败，锁键：{}", lockKey, e);
        }
    }
}