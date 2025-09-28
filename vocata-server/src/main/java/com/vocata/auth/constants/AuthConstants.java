package com.vocata.auth.constants;

/**
 * 认证模块常量类
 * 
 * @author levon
 */
public class AuthConstants {
    
    // 验证码相关常量
    public static final String EMAIL_CODE_KEY_PREFIX = "email_code:";
    public static final String EMAIL_CODE_LIMIT_KEY_PREFIX = "email_code_limit:";
    public static final String RESET_PASSWORD_KEY_PREFIX = "reset_password:";
    public static final String LOGIN_FAIL_COUNT_KEY_PREFIX = "login_fail:";
    
    // 验证码有效期（分钟）
    public static final int EMAIL_CODE_EXPIRE_MINUTES = 5;
    
    // 验证码长度
    public static final int EMAIL_CODE_LENGTH = 6;
    
    // 登录失败限制
    public static final int MAX_LOGIN_FAIL_COUNT = 5;
    public static final int LOGIN_LOCK_MINUTES = 30;
    public static final int ACCOUNT_LOCK_MINUTES = 30;
    
    // 验证码发送限制
    public static final int EMAIL_CODE_LIMIT_COUNT = 5;
    public static final int EMAIL_CODE_LIMIT_MINUTES = 60;
    
    // 密码重置令牌有效期（分钟）
    public static final int RESET_PASSWORD_EXPIRE_MINUTES = 30;
    
    // 邮件模板类型
    public static final String EMAIL_TEMPLATE_REGISTER = "register";
    public static final String EMAIL_TEMPLATE_LOGIN = "login";
    public static final String EMAIL_TEMPLATE_RESET_PASSWORD = "reset_password";
    public static final String EMAIL_TEMPLATE_CHANGE_EMAIL = "change_email";
    
    // 验证码类型 (Integer类型)
    public static final int VERIFICATION_CODE_TYPE_REGISTER = 1;
    public static final int VERIFICATION_CODE_TYPE_LOGIN = 2;
    public static final int VERIFICATION_CODE_TYPE_RESET_PASSWORD = 3;
    public static final int VERIFICATION_CODE_TYPE_CHANGE_EMAIL = 4;
    
    // 密码要求
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 20;
    
    // 用户状态
    public static final Integer USER_STATUS_NORMAL = 1;
    public static final Integer USER_STATUS_DISABLED = 0;
    public static final Integer USER_STATUS_LOCKED = 2;
    
    // 性别
    public static final Integer GENDER_UNSET = 0;
    public static final Integer GENDER_MALE = 1;
    public static final Integer GENDER_FEMALE = 2;
    
    // 私有构造函数防止实例化
    private AuthConstants() {
        throw new IllegalStateException("Utility class");
    }
}
