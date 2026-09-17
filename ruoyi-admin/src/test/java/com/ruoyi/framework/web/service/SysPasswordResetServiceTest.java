package com.ruoyi.framework.web.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.PasswordResetBody;
import com.ruoyi.common.core.domain.model.PasswordResetEmailCodeBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysMailConfigService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.domain.SysMailConfig;
import com.ruoyi.system.mapper.SysMailConfigMapper;
import com.ruoyi.system.service.impl.SysMailConfigServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 密码重置服务测试。
 */
@ExtendWith(MockitoExtension.class)
class SysPasswordResetServiceTest
{
    @Mock
    private ISysUserService userService;

    @Mock
    private ISysConfigService configService;

    @Mock
    private ISysMailConfigService mailConfigService;

    @Mock
    private SysMailService mailService;

    @Mock
    private SysPasswordService passwordService;

    @Mock
    private TokenService tokenService;

    @Mock
    private RedisCache redisCache;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ValueOperations<Object, Object> valueOperations;

    @InjectMocks
    private SysPasswordResetService service;

    @BeforeEach
    void setUp()
    {
        lenient().when(mailConfigService.isMailServiceAvailable()).thenReturn(true);
        lenient().when(configService.selectCaptchaEnabled()).thenReturn(false);
        lenient().when(configService.selectConfigByKey("sys.account.chrtype")).thenReturn("3");
    }

    @Test
    void statusShouldFollowMailAvailability()
    {
        assertTrue(service.isResetEnabled());
        when(mailConfigService.isMailServiceAvailable()).thenReturn(false);
        assertEquals(false, service.isResetEnabled());
    }

    @Test
    void mailAvailabilityShouldRequireEnabledAndCompleteConfiguration()
    {
        SysMailConfigMapper mapper = org.mockito.Mockito.mock(SysMailConfigMapper.class);
        SysMailConfigServiceImpl mailConfigServiceImpl = new SysMailConfigServiceImpl();
        ReflectionTestUtils.setField(mailConfigServiceImpl, "mailConfigMapper", mapper);

        when(mapper.selectMailConfig()).thenReturn(null);
        assertEquals(false, mailConfigServiceImpl.isMailServiceAvailable());

        SysMailConfig incomplete = new SysMailConfig();
        incomplete.setEnabled(UserConstants.YES);
        incomplete.setAuthEnable("N");
        when(mapper.selectMailConfig()).thenReturn(incomplete);
        assertEquals(false, mailConfigServiceImpl.isMailServiceAvailable());

        SysMailConfig complete = new SysMailConfig();
        complete.setEnabled(UserConstants.YES);
        complete.setAuthEnable("N");
        complete.setHost("smtp.example.com");
        complete.setPort(465);
        complete.setFromEmail("system@example.com");
        when(mapper.selectMailConfig()).thenReturn(complete);
        assertTrue(mailConfigServiceImpl.isMailServiceAvailable());
    }

    @Test
    void sendEmailCodeShouldRejectMismatchedEmail()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        PasswordResetEmailCodeBody body = createEmailCodeBody();
        body.setEmail("other@example.com");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.sendEmailCode(body));

        assertEquals("账号与邮箱不匹配", exception.getMessage());
        verify(mailService, never()).sendPasswordResetCode(anyString(), anyString());
    }

    @Test
    void sendEmailCodeShouldStoreCodeAndSendMail()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(true);

        service.sendEmailCode(createEmailCodeBody());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisCache).setCacheObject(eq(CacheConstants.PASSWORD_RESET_EMAIL_CODE_KEY + 100L),
                codeCaptor.capture(), eq(Constants.PASSWORD_RESET_EMAIL_CODE_EXPIRATION), eq(TimeUnit.MINUTES));
        assertTrue(codeCaptor.getValue().matches("\\d{6}"));
        verify(mailService).sendPasswordResetCode("user@example.com", codeCaptor.getValue());
    }

    @Test
    void sendEmailCodeShouldRejectAccountCooldown()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.sendEmailCode(createEmailCodeBody()));

        assertEquals("验证码发送过于频繁，请稍候再试", exception.getMessage());
        verify(mailService, never()).sendPasswordResetCode(anyString(), anyString());
    }

    @Test
    void sendEmailCodeShouldClearRedisStateWhenMailFails()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(true);
        doThrow(new ServiceException("邮件发送失败")).when(mailService)
                .sendPasswordResetCode(eq("user@example.com"), anyString());

        assertThrows(ServiceException.class, () -> service.sendEmailCode(createEmailCodeBody()));

        verify(redisCache).deleteObject(CacheConstants.PASSWORD_RESET_EMAIL_CODE_KEY + 100L);
        verify(redisCache).deleteObject(CacheConstants.PASSWORD_RESET_SEND_COOLDOWN_KEY + 100L);
    }

    @Test
    void sendEmailCodeShouldValidateCaptchaWhenEnabled()
    {
        when(configService.selectCaptchaEnabled()).thenReturn(true);
        when(redisCache.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + "captcha-uuid")).thenReturn("ABCD");
        PasswordResetEmailCodeBody body = createEmailCodeBody();
        body.setUuid("captcha-uuid");
        body.setCode("WRONG");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.sendEmailCode(body));

        assertEquals("图形验证码错误", exception.getMessage());
        verify(redisCache).deleteObject(CacheConstants.CAPTCHA_CODE_KEY + "captcha-uuid");
        verify(userService, never()).selectUserByUserName(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetPasswordShouldUpdatePasswordAndClearSessions()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any())).thenReturn(1L);
        when(userService.resetUserPwd(eq(100L), anyString())).thenReturn(1);
        when(tokenService.delLoginUsersByUserId(100L)).thenReturn(2);

        PasswordResetBody body = createResetBody();
        service.resetPassword(body);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(userService).resetUserPwd(eq(100L), passwordCaptor.capture());
        assertTrue(SecurityUtils.matchesPassword("Password1", passwordCaptor.getValue()));
        verify(passwordService).clearLoginRecordCache("testUser");
        verify(tokenService).delLoginUsersByUserId(100L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetPasswordShouldRejectInvalidEmailCode()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any())).thenReturn(-1L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.resetPassword(createResetBody()));

        assertEquals("邮箱验证码错误", exception.getMessage());
        verify(userService, never()).resetUserPwd(any(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void resetPasswordShouldInvalidateCodeAfterTooManyAttempts()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any())).thenReturn(-2L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.resetPassword(createResetBody()));

        assertEquals("邮箱验证码错误次数过多，请重新获取", exception.getMessage());
        verify(userService, never()).resetUserPwd(any(), anyString());
    }

    @Test
    void resetPasswordShouldRejectCurrentPassword()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        PasswordResetBody body = createResetBody();
        body.setNewPassword("OldPassword1");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.resetPassword(body));

        assertEquals("新密码不能与原密码相同", exception.getMessage());
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any(), any(), any());
    }

    @Test
    void resetPasswordShouldEnforceConfiguredPasswordRule()
    {
        SysUser user = createUser();
        when(userService.selectUserByUserName("testUser")).thenReturn(user);
        PasswordResetBody body = createResetBody();
        body.setNewPassword("OnlyLetters");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.resetPassword(body));

        assertEquals("密码必须同时包含字母和数字", exception.getMessage());
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any(), any(), any());
    }

    @Test
    void tokenServiceShouldDeleteAllSessionsForUser()
    {
        RedisCache tokenRedisCache = org.mockito.Mockito.mock(RedisCache.class);
        TokenService realTokenService = new TokenService();
        ReflectionTestUtils.setField(realTokenService, "redisCache", tokenRedisCache);
        Collection<String> keys = Arrays.asList("login_tokens:a", "login_tokens:b", "login_tokens:c");
        LoginUser first = new LoginUser();
        first.setUserId(100L);
        LoginUser second = new LoginUser();
        second.setUserId(200L);
        LoginUser third = new LoginUser();
        third.setUserId(100L);
        when(tokenRedisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*")).thenReturn(keys);
        when(tokenRedisCache.getCacheObject("login_tokens:a")).thenReturn(first);
        when(tokenRedisCache.getCacheObject("login_tokens:b")).thenReturn(second);
        when(tokenRedisCache.getCacheObject("login_tokens:c")).thenReturn(third);
        when(tokenRedisCache.deleteObject("login_tokens:a")).thenReturn(true);
        when(tokenRedisCache.deleteObject("login_tokens:c")).thenReturn(true);

        int count = realTokenService.delLoginUsersByUserId(100L);

        assertEquals(2, count);
        verify(tokenRedisCache).deleteObject("login_tokens:a");
        verify(tokenRedisCache, never()).deleteObject("login_tokens:b");
        verify(tokenRedisCache).deleteObject("login_tokens:c");
    }

    private SysUser createUser()
    {
        SysUser user = new SysUser();
        user.setUserId(100L);
        user.setUserName("testUser");
        user.setEmail("User@Example.com");
        user.setStatus("0");
        user.setPassword(SecurityUtils.encryptPassword("OldPassword1"));
        return user;
    }

    private PasswordResetEmailCodeBody createEmailCodeBody()
    {
        PasswordResetEmailCodeBody body = new PasswordResetEmailCodeBody();
        body.setUsername(" testUser ");
        body.setEmail(" user@example.com ");
        body.setCode("");
        body.setUuid("");
        return body;
    }

    private PasswordResetBody createResetBody()
    {
        PasswordResetBody body = new PasswordResetBody();
        body.setUsername(" testUser ");
        body.setEmail(" user@example.com ");
        body.setEmailCode("123456");
        body.setNewPassword("Password1");
        return body;
    }
}
