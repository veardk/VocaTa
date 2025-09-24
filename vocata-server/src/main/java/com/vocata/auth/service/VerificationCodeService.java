package com.vocata.auth.service;

/**
 * 验证码服务接口
 */
public interface VerificationCodeService {

    /**
     * 发送注册验证码
     */
    void sendRegisterCode(String email);

    /**
     * 发送登录验证码
     */
    void sendLoginCode(String email);

    /**
     * 发送重置密码验证码
     */
    void sendResetPasswordCode(String email);

    /**
     * 发送修改邮箱验证码
     */
    void sendChangeEmailCode(String email);

    /**
     * 验证验证码
     */
    boolean verifyCode(String email, String code, Integer type);

    /**
     * 验证并使用验证码
     */
    boolean verifyAndUseCode(String email, String code, Integer type);

    /**
     * 仅使用验证码（删除已验证的验证码）
     */
    void useCode(String email, Integer type);

    /**
     * 生成验证码
     */
    String generateCode();

    /**
     * 检查验证码发送限制
     */
    boolean checkSendLimit(String email);
}