package com.vocata.auth.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     */
    void sendVerificationCode(String to, String code, String type);

    /**
     * 发送注册验证码
     */
    void sendRegisterVerificationCode(String to, String code);

    /**
     * 发送登录验证码
     */
    void sendLoginVerificationCode(String to, String code);

    /**
     * 发送重置密码验证码
     */
    void sendResetPasswordCode(String to, String code);

    /**
     * 发送修改邮箱验证码
     */
    void sendChangeEmailCode(String to, String code);

    /**
     * 发送普通邮件
     */
    void sendSimpleMail(String to, String subject, String content);

    /**
     * 发送HTML邮件
     */
    void sendHtmlMail(String to, String subject, String content);
}