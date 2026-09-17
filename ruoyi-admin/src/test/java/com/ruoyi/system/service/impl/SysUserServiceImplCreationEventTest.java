package com.ruoyi.system.service.impl;

import java.util.Collections;
import java.util.List;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.UserCreatedEvent;
import com.ruoyi.system.mapper.SysPostMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserPostMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysDeptService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserServiceImplCreationEventTest
{
    private SysUserServiceImpl userService;

    private SysUserMapper userMapper;

    private ISysConfigService configService;

    private Validator validator;

    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp()
    {
        SysUserServiceImpl target = new SysUserServiceImpl();
        userMapper = mock(SysUserMapper.class);
        configService = mock(ISysConfigService.class);
        validator = mock(Validator.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        ReflectionTestUtils.setField(target, "userMapper", userMapper);
        ReflectionTestUtils.setField(target, "roleMapper", mock(SysRoleMapper.class));
        ReflectionTestUtils.setField(target, "postMapper", mock(SysPostMapper.class));
        ReflectionTestUtils.setField(target, "userRoleMapper", mock(SysUserRoleMapper.class));
        ReflectionTestUtils.setField(target, "userPostMapper", mock(SysUserPostMapper.class));
        ReflectionTestUtils.setField(target, "configService", configService);
        ReflectionTestUtils.setField(target, "deptService", mock(ISysDeptService.class));
        ReflectionTestUtils.setField(target, "validator", validator);
        ReflectionTestUtils.setField(target, "eventPublisher", eventPublisher);

        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.setExposeProxy(true);
        userService = (SysUserServiceImpl) proxyFactory.getProxy();

        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            if (user.getUserId() == null)
            {
                user.setUserId(100L);
            }
            return 1;
        }).when(userMapper).insertUser(any(SysUser.class));
    }

    @Test
    void shouldPublishEventForAdminCreatedUser()
    {
        SysUser user = newUser("admin-created");

        assertEquals(1, userService.insertUser(user));

        assertPublished(user.getUserId(), "admin-created");
    }

    @Test
    void shouldPublishEventForRegisteredUser()
    {
        SysUser user = newUser("registered");

        assertTrue(userService.registerUser(user));

        assertPublished(user.getUserId(), "registered");
    }

    @Test
    void shouldPublishEventForImportedUser()
    {
        SysUser user = newUser("imported");
        when(userMapper.selectUserByUserName("imported")).thenReturn(null);
        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("123456");
        doReturn(Collections.emptySet()).when(validator).validate(any(SysUser.class));

        userService.importUser(List.of(user), false, "admin");

        assertPublished(user.getUserId(), "imported");
    }

    private SysUser newUser(String username)
    {
        SysUser user = new SysUser();
        user.setUserName(username);
        user.setNickName(username);
        return user;
    }

    private void assertPublished(Long userId, String username)
    {
        ArgumentCaptor<UserCreatedEvent> captor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals(username, captor.getValue().getUsername());
    }
}
