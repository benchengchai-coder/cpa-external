package com.ruoyi.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 注册邮箱验证码 redis key
     */
    public static final String REGISTER_EMAIL_CODE_KEY = "register_email_codes:";

    /**
     * 密码重置邮箱验证码 redis key
     */
    public static final String PASSWORD_RESET_EMAIL_CODE_KEY = "password_reset_email_codes:";

    /**
     * 密码重置邮件发送冷却 redis key
     */
    public static final String PASSWORD_RESET_SEND_COOLDOWN_KEY = "password_reset_send_cooldown:";

    /**
     * 密码重置验证码错误次数 redis key
     */
    public static final String PASSWORD_RESET_CODE_ATTEMPT_KEY = "password_reset_code_attempts:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";
}
