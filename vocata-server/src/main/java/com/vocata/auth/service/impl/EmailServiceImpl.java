package com.vocata.auth.service.impl;

import com.vocata.auth.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * 邮件服务实现
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendVerificationCode(String to, String code, String type) {
        String subject = getSubjectByType(type);
        String content = buildVerificationCodeContent(code, type);
        sendHtmlMail(to, subject, content);
    }

    @Override
    public void sendRegisterVerificationCode(String to, String code) {
        sendVerificationCode(to, code, "register");
    }

    @Override
    public void sendLoginVerificationCode(String to, String code) {
        sendVerificationCode(to, code, "login");
    }

    @Override
    public void sendResetPasswordCode(String to, String code) {
        sendVerificationCode(to, code, "reset");
    }

    @Override
    public void sendChangeEmailCode(String to, String code) {
        sendVerificationCode(to, code, "change");
    }

    @Override
    public void sendSimpleMail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("普通邮件发送成功，收件人：{}，主题：{}", to, subject);
        } catch (Exception e) {
            log.error("普通邮件发送失败，收件人：{}，主题：{}", to, subject, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtmlMail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("HTML邮件发送成功，收件人：{}，主题：{}", to, subject);
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败，收件人：{}，主题：{}", to, subject, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 根据类型获取邮件主题
     */
    private String getSubjectByType(String type) {
        switch (type) {
            case "register":
                return "【VocaTa】注册验证码";
            case "login":
                return "【VocaTa】登录验证码";
            case "reset":
                return "【VocaTa】重置密码验证码";
            case "change":
                return "【VocaTa】修改邮箱验证码";
            default:
                return "【VocaTa】验证码";
        }
    }

    /**
     * 构建验证码邮件内容
     */
    private String buildVerificationCodeContent(String code, String type) {
        String operation = getOperationByType(type);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>VocaTa验证码</title>
                <style>
                    body { font-family: 'Microsoft YaHei', Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: normal; }
                    .content { padding: 40px 30px; }
                    .code-box { background-color: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                    .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; margin: 10px 0; }
                    .note { color: #666; font-size: 14px; line-height: 1.6; margin: 20px 0; }
                    .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
                    .warning { color: #e74c3c; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎭 VocaTa AI角色平台</h1>
                    </div>
                    <div class="content">
                        <h2>您好！</h2>
                        <p>您正在进行<strong>%s</strong>操作，请使用以下验证码完成验证：</p>

                        <div class="code-box">
                            <div class="code">%s</div>
                            <div style="color: #666; margin-top: 10px;">请在5分钟内使用此验证码</div>
                        </div>

                        <div class="note">
                            <p><strong>安全提示：</strong></p>
                            <ul>
                                <li>验证码5分钟内有效，请及时使用</li>
                                <li>请勿将验证码告诉他人</li>
                                <li>如果您没有进行此操作，请忽略此邮件</li>
                            </ul>
                        </div>

                        <p class="warning">⚠️ 此邮件为系统自动发送，请勿回复</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 VocaTa AI角色平台 | 让每一次对话都充满魅力</p>
                    </div>
                </div>
            </body>
            </html>
            """, operation, code);
    }

    /**
     * 根据类型获取操作名称
     */
    private String getOperationByType(String type) {
        switch (type) {
            case "register":
                return "账号注册";
            case "login":
                return "账号登录";
            case "reset":
                return "重置密码";
            case "change":
                return "修改邮箱";
            default:
                return "身份验证";
        }
    }
}