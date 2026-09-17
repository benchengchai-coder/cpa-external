package com.ruoyi.framework.web.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.core.domain.model.UserRegisteredEvent;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysMailConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 注册校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysRegisterService
{
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /** 注册邮箱仅允许使用以下域名 */
    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of("qq.com", "163.com", "gmail.com");

    /** 注册用户名只允许字母和数字 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    /** 注册用户名长度限制（独立于登录用的 UserConstants，避免影响登录校验） */
    private static final int REGISTER_USERNAME_MIN_LENGTH = 5;
    private static final int REGISTER_USERNAME_MAX_LENGTH = 12;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Logger log = LoggerFactory.getLogger(SysRegisterService.class);

    /**
     * 注册用户默认角色参数键（值为角色键 role_key，留空则不分配角色）
     */
    public static final String REGISTER_ROLE_CONFIG_KEY = "sys.user.registerRole";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysMailConfigService mailConfigService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private SysMailService mailService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 发送注册邮箱验证码
     */
    public String sendEmailCode(String rawEmail)
    {
        String email = normalizeEmail(rawEmail);
        String msg = validateRegisterEmail(email);
        if (StringUtils.isNotEmpty(msg))
        {
            return msg;
        }
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        String verifyKey = CacheConstants.REGISTER_EMAIL_CODE_KEY + email;
        redisCache.setCacheObject(verifyKey, code, Constants.REGISTER_EMAIL_CODE_EXPIRATION, TimeUnit.MINUTES);
        try
        {
            mailService.sendRegisterCode(email, code);
        }
        catch (RuntimeException e)
        {
            redisCache.deleteObject(verifyKey);
            throw e;
        }
        return "";
    }

    /**
     * 注册
     */
    public String register(RegisterBody registerBody)
    {
        if (StringUtils.isNull(registerBody))
        {
            return "注册信息不能为空";
        }
        String msg = "", username = registerBody.getUsername(), password = registerBody.getPassword();
        String email = normalizeEmail(registerBody.getEmail());
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setEmail(email);

        // 验证码开关
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        if (StringUtils.isEmpty(username))
        {
            msg = "用户名不能为空";
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = "用户密码不能为空";
        }
        else if (username.length() < REGISTER_USERNAME_MIN_LENGTH
                || username.length() > REGISTER_USERNAME_MAX_LENGTH)
        {
            msg = "账户长度必须在5到12个字符之间";
        }
        else if (!USERNAME_PATTERN.matcher(username).matches())
        {
            msg = "账户只能包含字母和数字";
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            msg = "密码长度必须在5到20个字符之间";
        }
        else if (!userService.checkUserNameUnique(sysUser))
        {
            msg = "保存用户'" + username + "'失败，注册账号已存在";
        }
        else if (mailConfigService.selectEmailVerifyEnabled())
        {
            // 邮箱验证已开启：邮箱必填 + 验证码必填
            String emailMsg = validateRegisterEmail(email);
            if (StringUtils.isNotEmpty(emailMsg))
            {
                msg = emailMsg;
            }
            else if (StringUtils.isEmpty(registerBody.getEmailCode()))
            {
                msg = "邮箱验证码不能为空";
            }
            else
            {
                String emailCodeMsg = validateEmailCode(email, registerBody.getEmailCode());
                if (StringUtils.isNotEmpty(emailCodeMsg))
                {
                    msg = emailCodeMsg;
                }
            }
        }
        else
        {
            // 邮箱验证已关闭：邮箱可选，如果提供了邮箱则校验格式
            if (StringUtils.isNotEmpty(email))
            {
                String emailMsg = validateRegisterEmail(email);
                if (StringUtils.isNotEmpty(emailMsg))
                {
                    msg = emailMsg;
                }
            }
        }

        if (StringUtils.isEmpty(msg))
        {
            sysUser.setNickName(username);
            sysUser.setEmail(email);
            sysUser.setPwdUpdateDate(DateUtils.getNowDate());
            sysUser.setPassword(SecurityUtils.encryptPassword(password));
            boolean regFlag = userService.registerUser(sysUser);
            if (!regFlag)
            {
                msg = "注册失败,请联系系统管理人员";
            }
            else
            {
                // 分配默认角色（由参数配置 sys.user.registerRole 控制，失败不影响注册结果）
                assignDefaultRole(sysUser.getUserId());
                // 发布注册成功事件：邀请绑定、注册试用订阅等业务域通过 @EventListener 自行处理，
                // 监听方自行 try/catch，确保异常不阻断注册主流程
                eventPublisher.publishEvent(
                        new UserRegisteredEvent(sysUser.getUserId(), username, registerBody.getInviteCode()));
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success")));
            }
        }
        return msg;
    }

    /**
     * 为新注册用户分配默认角色
     * <p>
     * 读取参数配置 sys.user.registerRole（角色键 role_key），
     * 配置为空、角色不存在或已停用时均不分配，保持原有零角色行为。
     * 角色分配失败仅记录日志，不影响注册结果。
     *
     * @param userId 新注册用户ID
     */
    private void assignDefaultRole(Long userId)
    {
        String roleKey = configService.selectConfigByKey(REGISTER_ROLE_CONFIG_KEY);
        if (StringUtils.isEmpty(roleKey))
        {
            return;
        }
        SysRole role = roleMapper.checkRoleKeyUnique(roleKey);
        if (StringUtils.isNull(role) || !UserConstants.ROLE_NORMAL.equals(role.getStatus()))
        {
            log.warn("注册默认角色[{}]不存在或已停用，跳过角色分配", roleKey);
            return;
        }
        try
        {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getRoleId());
            List<SysUserRole> list = new ArrayList<SysUserRole>();
            list.add(userRole);
            userRoleMapper.batchUserRole(list);
        }
        catch (Exception e)
        {
            log.error("为新注册用户[{}]分配默认角色[{}]失败", userId, roleKey, e);
        }
    }

    private String validateRegisterEmail(String email)
    {
        if (StringUtils.isEmpty(email))
        {
            return "邮箱不能为空";
        }
        if (email.length() > 50)
        {
            return "邮箱长度不能超过50个字符";
        }
        if (!EMAIL_PATTERN.matcher(email).matches())
        {
            return "邮箱格式错误";
        }
        String emailDomain = email.substring(email.lastIndexOf('@') + 1);
        if (!ALLOWED_EMAIL_DOMAINS.contains(emailDomain))
        {
            return "仅支持QQ、163、Gmail邮箱";
        }
        SysUser sysUser = new SysUser();
        sysUser.setEmail(email);
        if (!userService.checkEmailUnique(sysUser))
        {
            return "邮箱账号已存在";
        }
        return "";
    }

    private String validateEmailCode(String email, String emailCode)
    {
        String verifyKey = CacheConstants.REGISTER_EMAIL_CODE_KEY + email;
        String code = redisCache.getCacheObject(verifyKey);
        if (StringUtils.isEmpty(code))
        {
            return "邮箱验证码已失效";
        }
        if (!emailCode.equalsIgnoreCase(code))
        {
            return "邮箱验证码错误";
        }
        redisCache.deleteObject(verifyKey);
        return "";
    }

    private String normalizeEmail(String email)
    {
        return StringUtils.trim(email).toLowerCase(Locale.ROOT);
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
