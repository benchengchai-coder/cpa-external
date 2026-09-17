package com.ruoyi.framework.web.service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.PasswordResetBody;
import com.ruoyi.common.core.domain.model.PasswordResetEmailCodeBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysMailConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 匿名密码重置服务。
 */
@Component
public class SysPasswordResetService
{
    private static final Logger log = LoggerFactory.getLogger(SysPasswordResetService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern ANY_PASSWORD_PATTERN = Pattern.compile("^[^<>\\\"'|\\\\]+$");

    private static final Pattern NUMBER_PASSWORD_PATTERN = Pattern.compile("^[0-9]+$");

    private static final Pattern LETTER_PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    private static final Pattern LETTER_NUMBER_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]+$");

    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()\\-=_+])[A-Za-z\\d~!@#$%^&*()\\-=_+]+$");

    private static final int PASSWORD_MIN_LENGTH = 6;

    private static final int PASSWORD_MAX_LENGTH = 20;

    private static final int SEND_COOLDOWN_SECONDS = 60;

    private static final int MAX_CODE_ATTEMPTS = 5;

    private static final long CODE_NOT_FOUND = 0L;

    private static final long CODE_MISMATCH = -1L;

    private static final long CODE_ATTEMPTS_EXCEEDED = -2L;

    private static final DefaultRedisScript<Long> VERIFY_CODE_SCRIPT = buildVerifyCodeScript();

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysMailConfigService mailConfigService;

    @Autowired
    private SysMailService mailService;

    @Autowired
    private SysPasswordService passwordService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 判断密码重置功能是否可用。
     */
    public boolean isResetEnabled()
    {
        return mailConfigService.isMailServiceAvailable();
    }

    /**
     * 获取图形验证码开关。
     */
    public boolean isCaptchaEnabled()
    {
        return configService.selectCaptchaEnabled();
    }

    /**
     * 获取密码字符规则。
     */
    public String getPasswordChrtype()
    {
        return StringUtils.nvl(configService.selectConfigByKey("sys.account.chrtype"), "0");
    }

    /**
     * 发送密码重置邮箱验证码。
     */
    public void sendEmailCode(PasswordResetEmailCodeBody body)
    {
        ensureResetEnabled();
        if (body == null)
        {
            throw new ServiceException("请求信息不能为空");
        }
        String username = normalizeUsername(body.getUsername());
        validateCaptcha(username, body.getCode(), body.getUuid());
        SysUser user = validateAccount(username, body.getEmail());
        Long userId = user.getUserId();
        String cooldownKey = CacheConstants.PASSWORD_RESET_SEND_COOLDOWN_KEY + userId;
        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(cooldownKey, Boolean.TRUE,
                SEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(reserved))
        {
            fail(username, "验证码发送过于频繁，请稍候再试");
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        String codeKey = CacheConstants.PASSWORD_RESET_EMAIL_CODE_KEY + userId;
        String attemptKey = CacheConstants.PASSWORD_RESET_CODE_ATTEMPT_KEY + userId;
        redisCache.deleteObject(attemptKey);
        redisCache.setCacheObject(codeKey, code, Constants.PASSWORD_RESET_EMAIL_CODE_EXPIRATION, TimeUnit.MINUTES);
        try
        {
            mailService.sendPasswordResetCode(normalizeEmail(user.getEmail()), code);
            log.info("密码重置验证码发送成功，用户ID：{}，账号：{}，IP：{}", userId, username, getRequestIp());
        }
        catch (RuntimeException e)
        {
            redisCache.deleteObject(codeKey);
            redisCache.deleteObject(cooldownKey);
            throw e;
        }
    }

    /**
     * 重置用户密码。
     */
    public void resetPassword(PasswordResetBody body)
    {
        ensureResetEnabled();
        if (body == null)
        {
            throw new ServiceException("请求信息不能为空");
        }
        String username = normalizeUsername(body.getUsername());
        SysUser user = validateAccount(username, body.getEmail());
        validateNewPassword(body.getNewPassword());
        if (SecurityUtils.matchesPassword(body.getNewPassword(), user.getPassword()))
        {
            fail(username, "新密码不能与原密码相同");
        }
        consumeEmailCode(user.getUserId(), body.getEmailCode(), username);

        passwordService.clearLoginRecordCache(username);
        int deletedSessions = tokenService.delLoginUsersByUserId(user.getUserId());
        String encryptedPassword = SecurityUtils.encryptPassword(body.getNewPassword());
        if (userService.resetUserPwd(user.getUserId(), encryptedPassword) <= 0)
        {
            fail(username, "密码重置失败，请重新获取验证码后再试");
        }
        log.info("密码重置成功，用户ID：{}，账号：{}，下线会话数：{}，IP：{}", user.getUserId(), username,
                deletedSessions, getRequestIp());
    }

    private void ensureResetEnabled()
    {
        if (!isResetEnabled())
        {
            throw new ServiceException("密码找回功能未启用，请联系管理员");
        }
    }

    private SysUser validateAccount(String username, String rawEmail)
    {
        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("账号不能为空");
        }
        SysUser user = userService.selectUserByUserName(username);
        if (user == null)
        {
            fail(username, "账号不存在");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            fail(username, "账号已停用，请联系管理员");
        }
        if (StringUtils.isEmpty(user.getEmail()))
        {
            fail(username, "账号未绑定邮箱，请联系管理员");
        }
        String email = normalizeEmail(rawEmail);
        validateEmail(email);
        if (!normalizeEmail(user.getEmail()).equals(email))
        {
            fail(username, "账号与邮箱不匹配");
        }
        return user;
    }

    private void validateCaptcha(String username, String code, String uuid)
    {
        if (!isCaptchaEnabled())
        {
            return;
        }
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("图形验证码不能为空");
        }
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (StringUtils.isEmpty(captcha))
        {
            fail(username, "图形验证码已失效");
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            fail(username, "图形验证码错误");
        }
    }

    private void consumeEmailCode(Long userId, String emailCode, String username)
    {
        if (StringUtils.isEmpty(emailCode))
        {
            throw new ServiceException("邮箱验证码不能为空");
        }
        String codeKey = CacheConstants.PASSWORD_RESET_EMAIL_CODE_KEY + userId;
        String attemptKey = CacheConstants.PASSWORD_RESET_CODE_ATTEMPT_KEY + userId;
        List<Object> keys = Arrays.asList(codeKey, attemptKey);
        Long result = redisTemplate.execute(VERIFY_CODE_SCRIPT, keys, StringUtils.trim(emailCode), MAX_CODE_ATTEMPTS,
                Constants.PASSWORD_RESET_EMAIL_CODE_EXPIRATION * 60);
        if (result == null || result.longValue() == CODE_NOT_FOUND)
        {
            fail(username, "邮箱验证码已失效");
        }
        if (result.longValue() == CODE_MISMATCH)
        {
            fail(username, "邮箱验证码错误");
        }
        if (result.longValue() == CODE_ATTEMPTS_EXCEEDED)
        {
            fail(username, "邮箱验证码错误次数过多，请重新获取");
        }
    }

    private void validateEmail(String email)
    {
        if (StringUtils.isEmpty(email))
        {
            throw new ServiceException("邮箱不能为空");
        }
        if (email.length() > 50)
        {
            throw new ServiceException("邮箱长度不能超过50个字符");
        }
        if (!EMAIL_PATTERN.matcher(email).matches())
        {
            throw new ServiceException("邮箱格式错误");
        }
    }

    private void validateNewPassword(String password)
    {
        if (StringUtils.isEmpty(password))
        {
            throw new ServiceException("新密码不能为空");
        }
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("新密码长度必须介于6和20之间");
        }
        String chrtype = getPasswordChrtype();
        Pattern pattern = ANY_PASSWORD_PATTERN;
        String message = "密码不能包含非法字符：< > \" ' \\ |";
        if ("1".equals(chrtype))
        {
            pattern = NUMBER_PASSWORD_PATTERN;
            message = "密码只能为数字（0-9）";
        }
        else if ("2".equals(chrtype))
        {
            pattern = LETTER_PASSWORD_PATTERN;
            message = "密码只能为英文字母（a-z、A-Z）";
        }
        else if ("3".equals(chrtype))
        {
            pattern = LETTER_NUMBER_PASSWORD_PATTERN;
            message = "密码必须同时包含字母和数字";
        }
        else if ("4".equals(chrtype))
        {
            pattern = STRONG_PASSWORD_PATTERN;
            message = "密码必须同时包含字母、数字和特殊字符（~!@#$%^&*()-=_+）";
        }
        if (!pattern.matcher(password).matches())
        {
            throw new ServiceException(message);
        }
    }

    private String normalizeUsername(String username)
    {
        return StringUtils.trim(username);
    }

    private String normalizeEmail(String email)
    {
        return StringUtils.trim(email).toLowerCase(Locale.ROOT);
    }

    private void fail(String username, String message)
    {
        log.warn("密码重置校验失败，账号：{}，原因：{}，IP：{}", username, message, getRequestIp());
        throw new ServiceException(message);
    }

    private String getRequestIp()
    {
        try
        {
            return IpUtils.getIpAddr();
        }
        catch (RuntimeException e)
        {
            return "unknown";
        }
    }

    private static DefaultRedisScript<Long> buildVerifyCodeScript()
    {
        DefaultRedisScript<Long> script = new DefaultRedisScript<Long>();
        script.setResultType(Long.class);
        script.setScriptText("local stored = redis.call('get', KEYS[1])\n" +
                "if not stored then return 0 end\n" +
                "if stored ~= ARGV[1] then\n" +
                "    local attempts = redis.call('incr', KEYS[2])\n" +
                "    if attempts == 1 then redis.call('expire', KEYS[2], tonumber(ARGV[3])) end\n" +
                "    if attempts >= tonumber(ARGV[2]) then\n" +
                "        redis.call('del', KEYS[1])\n" +
                "        redis.call('del', KEYS[2])\n" +
                "        return -2\n" +
                "    end\n" +
                "    return -1\n" +
                "end\n" +
                "redis.call('del', KEYS[1])\n" +
                "redis.call('del', KEYS[2])\n" +
                "return 1");
        return script;
    }
}
