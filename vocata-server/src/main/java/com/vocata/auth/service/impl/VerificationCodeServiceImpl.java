package com.vocata.auth.service.impl;

import com.vocata.auth.constants.AuthConstants;
import com.vocata.auth.service.EmailService;
import com.vocata.auth.service.VerificationCodeService;
import com.vocata.common.exception.BizException;
import com.vocata.common.result.ApiCode;
import com.vocata.common.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现 - 基于Redis缓存
 */
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeServiceImpl.class);

    // Redis Key 前缀
    private static final String CODE_KEY_PREFIX = "auth:code:";
    private static final String SEND_LIMIT_KEY_PREFIX = "auth:send_limit:";
    private static final String IP_LIMIT_KEY_PREFIX = "auth:ip_limit:";
    private static final String EMAIL_DAILY_LIMIT_KEY_PREFIX = "auth:daily_limit:";
    private static final String ATTEMPT_KEY_PREFIX = "auth:attempt:";

    // 限流配置
    private static final int CODE_EXPIRE_MINUTES = 5;           // 验证码过期时间(分钟)
    private static final int SEND_INTERVAL_SECONDS = 60;       // 发送间隔(秒)
    private static final int MAX_ATTEMPTS = 3;                 // 最大尝试次数
    private static final int EMAIL_DAILY_LIMIT = 10;           // 邮箱每日限制
    private static final int IP_HOURLY_LIMIT = 20;             // IP每小时限制
    private static final int CODE_LENGTH = 6;                  // 验证码长度

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private HttpServletRequest request;

    private final SecureRandom random = new SecureRandom();

    @Override
    public void sendRegisterCode(String email) {
        sendCode(email, AuthConstants.VERIFICATION_CODE_TYPE_REGISTER);
    }

    @Override
    public void sendLoginCode(String email) {
        sendCode(email, AuthConstants.VERIFICATION_CODE_TYPE_LOGIN);
    }

    @Override
    public void sendResetPasswordCode(String email) {
        sendCode(email, AuthConstants.VERIFICATION_CODE_TYPE_RESET_PASSWORD);
    }

    @Override
    public void sendChangeEmailCode(String email) {
        sendCode(email, AuthConstants.VERIFICATION_CODE_TYPE_CHANGE_EMAIL);
    }

    @Override
    public boolean verifyCode(String email, String code, Integer type) {
        String cacheKey = buildCodeKey(email, type);
        String storedCode = redisTemplate.opsForValue().get(cacheKey);

        if (storedCode == null) {
            log.warn("验证码不存在或已过期，邮箱：{}，类型：{}", email, type);
            return false;
        }

        // 增加尝试次数
        incrementAttemptCount(email, type);

        boolean isValid = storedCode.equals(code);
        if (isValid) {
            log.info("验证码验证成功，邮箱：{}，类型：{}", email, type);
        } else {
            log.warn("验证码错误，邮箱：{}，类型：{}，输入：{}", email, type, code);
        }

        return isValid;
    }

    @Override
    public boolean verifyAndUseCode(String email, String code, Integer type) {
        if (!verifyCode(email, code, type)) {
            return false;
        }

        // 验证成功后立即删除验证码
        useCode(email, type);

        log.info("验证码验证并使用成功，邮箱：{}，类型：{}", email, type);
        return true;
    }

    @Override
    public void useCode(String email, Integer type) {
        String cacheKey = buildCodeKey(email, type);
        redisTemplate.delete(cacheKey);

        // 清除尝试次数
        String attemptKey = buildAttemptKey(email, type);
        redisTemplate.delete(attemptKey);

        log.info("验证码使用成功，邮箱：{}，类型：{}", email, type);
    }

    @Override
    public String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    @Override
    public boolean checkSendLimit(String email) {
        String sendLimitKey = buildSendLimitKey(email);
        String lastSendTime = redisTemplate.opsForValue().get(sendLimitKey);
        return lastSendTime == null;
    }

    /**
     * 发送验证码 - 核心方法
     */
    private void sendCode(String email, Integer type) {
        String clientIp = IpUtils.getClientIp(request);

        // 1. 多重限流检查
        checkAllLimits(email, clientIp);

        // 2. 生成验证码
        String code = generateCode();

        // 3. 存储到Redis
        storeCodeToRedis(email, type, code);

        // 4. 发送邮件
        sendEmailByType(email, code, type);

        // 5. 更新限流计数
        updateLimitCounters(email, clientIp);

        log.info("验证码发送成功，邮箱：{}，类型：{}，IP：{}，验证码：{}",
            email, type, clientIp, code);
    }

    /**
     * 多重限流检查
     */
    private void checkAllLimits(String email, String clientIp) {
        // 1. 检查发送间隔
        checkSendInterval(email);

        // 2. 检查邮箱每日限制
        checkEmailDailyLimit(email);

        // 3. 检查IP每小时限制
        checkIpHourlyLimit(clientIp);

        // 4. 检查尝试次数限制
        checkAttemptLimit(email);
    }

    /**
     * 检查发送间隔
     */
    private void checkSendInterval(String email) {
        String sendLimitKey = buildSendLimitKey(email);
        String lastSendTime = redisTemplate.opsForValue().get(sendLimitKey);

        if (lastSendTime != null) {
            long remainingSeconds = redisTemplate.getExpire(sendLimitKey, TimeUnit.SECONDS);
            throw new BizException(ApiCode.TOO_MANY_REQUESTS.getCode(),
                String.format("验证码发送过于频繁，请%d秒后再试", remainingSeconds));
        }
    }

    /**
     * 检查邮箱每日限制
     */
    private void checkEmailDailyLimit(String email) {
        String dailyLimitKey = buildEmailDailyLimitKey(email);
        String countStr = redisTemplate.opsForValue().get(dailyLimitKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= EMAIL_DAILY_LIMIT) {
            throw new BizException(ApiCode.TOO_MANY_REQUESTS.getCode(),
                "今日验证码发送次数已达上限，请明天再试");
        }
    }

    /**
     * 检查IP每小时限制
     */
    private void checkIpHourlyLimit(String clientIp) {
        String ipLimitKey = buildIpLimitKey(clientIp);
        String countStr = redisTemplate.opsForValue().get(ipLimitKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= IP_HOURLY_LIMIT) {
            throw new BizException(ApiCode.TOO_MANY_REQUESTS.getCode(),
                "当前IP验证码请求过于频繁，请稍后再试");
        }
    }

    /**
     * 检查尝试次数限制
     */
    private void checkAttemptLimit(String email) {
        // 检查是否有过多的验证码类型在尝试
        int[] types = {
            AuthConstants.VERIFICATION_CODE_TYPE_REGISTER,
            AuthConstants.VERIFICATION_CODE_TYPE_LOGIN,
            AuthConstants.VERIFICATION_CODE_TYPE_RESET_PASSWORD,
            AuthConstants.VERIFICATION_CODE_TYPE_CHANGE_EMAIL
        };

        for (int type : types) {
            String attemptKey = buildAttemptKey(email, type);
            String attemptCountStr = redisTemplate.opsForValue().get(attemptKey);
            int attemptCount = attemptCountStr != null ? Integer.parseInt(attemptCountStr) : 0;

            if (attemptCount >= MAX_ATTEMPTS) {
                long remainingSeconds = redisTemplate.getExpire(attemptKey, TimeUnit.SECONDS);
                throw new BizException(ApiCode.TOO_MANY_REQUESTS.getCode(),
                    String.format("验证次数过多，请%d分钟后再试", remainingSeconds / 60 + 1));
            }
        }
    }

    /**
     * 存储验证码到Redis
     */
    private void storeCodeToRedis(String email, Integer type, String code) {
        String cacheKey = buildCodeKey(email, type);
        redisTemplate.opsForValue().set(cacheKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.debug("验证码已存储到Redis，key：{}，过期时间：{}分钟", cacheKey, CODE_EXPIRE_MINUTES);
    }

    /**
     * 更新限流计数器
     */
    private void updateLimitCounters(String email, String clientIp) {
        // 更新发送间隔计数
        String sendLimitKey = buildSendLimitKey(email);
        redisTemplate.opsForValue().set(sendLimitKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 更新邮箱每日计数
        String dailyLimitKey = buildEmailDailyLimitKey(email);
        redisTemplate.opsForValue().increment(dailyLimitKey);
        redisTemplate.expire(dailyLimitKey, 24, TimeUnit.HOURS);

        // 更新IP每小时计数
        String ipLimitKey = buildIpLimitKey(clientIp);
        redisTemplate.opsForValue().increment(ipLimitKey);
        redisTemplate.expire(ipLimitKey, 1, TimeUnit.HOURS);
    }

    /**
     * 增加尝试次数
     */
    private void incrementAttemptCount(String email, Integer type) {
        String attemptKey = buildAttemptKey(email, type);
        redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 根据类型发送邮件
     */
    private void sendEmailByType(String email, String code, Integer type) {
        try {
            switch (type) {
                case AuthConstants.VERIFICATION_CODE_TYPE_REGISTER:
                    emailService.sendRegisterVerificationCode(email, code);
                    break;
                case AuthConstants.VERIFICATION_CODE_TYPE_LOGIN:
                    emailService.sendLoginVerificationCode(email, code);
                    break;
                case AuthConstants.VERIFICATION_CODE_TYPE_RESET_PASSWORD:
                    emailService.sendResetPasswordCode(email, code);
                    break;
                case AuthConstants.VERIFICATION_CODE_TYPE_CHANGE_EMAIL:
                    emailService.sendChangeEmailCode(email, code);
                    break;
                default:
                    emailService.sendVerificationCode(email, code, "其他");
                    break;
            }
        } catch (Exception e) {
            log.error("发送验证码邮件失败，邮箱：{}，类型：{}", email, type, e);
            // 邮件发送失败时，删除已存储的验证码
            String cacheKey = buildCodeKey(email, type);
            redisTemplate.delete(cacheKey);
            throw new BizException(ApiCode.INTERNAL_SERVER_ERROR.getCode(), "邮件发送失败，请稍后重试");
        }
    }

    // ============ Redis Key 构建方法 ============

    private String buildCodeKey(String email, Integer type) {
        return CODE_KEY_PREFIX + email + ":" + type;
    }

    private String buildSendLimitKey(String email) {
        return SEND_LIMIT_KEY_PREFIX + email;
    }

    private String buildEmailDailyLimitKey(String email) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return EMAIL_DAILY_LIMIT_KEY_PREFIX + email + ":" + today;
    }

    private String buildIpLimitKey(String clientIp) {
        String currentHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        return IP_LIMIT_KEY_PREFIX + clientIp + ":" + currentHour;
    }

    private String buildAttemptKey(String email, Integer type) {
        return ATTEMPT_KEY_PREFIX + email + ":" + type;
    }

    /**
     * 获取验证码剩余有效时间（秒）
     */
    public Long getCodeRemainingTime(String email, Integer type) {
        String cacheKey = buildCodeKey(email, type);
        return redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
    }

    /**
     * 获取发送间隔剩余时间（秒）
     */
    public Long getSendRemainingTime(String email) {
        String sendLimitKey = buildSendLimitKey(email);
        return redisTemplate.getExpire(sendLimitKey, TimeUnit.SECONDS);
    }
}