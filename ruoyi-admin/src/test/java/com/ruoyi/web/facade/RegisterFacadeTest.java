package com.ruoyi.web.facade;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.ruoyi.common.core.domain.model.UserRegisteredEvent;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.cpaexternal.subscription.service.IAiSubscriptionService;
import com.ruoyi.system.service.ISysConfigService;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterFacadeTest
{
    @Test
    void shouldEnsureDefaultApiKeyAfterRegister()
    {
        ICpaApiKeyService apiKeyService = mock(ICpaApiKeyService.class);
        RegisterFacade facade = createFacade(apiKeyService, mock(PlatformTransactionManager.class));

        facade.onUserRegistered(new UserRegisteredEvent(7L, "newuser", null));

        verify(apiKeyService).ensureDefault(7L);
    }

    @Test
    void shouldNotBreakRegisterWhenApiKeyCreationFails()
    {
        ICpaApiKeyService apiKeyService = mock(ICpaApiKeyService.class);
        when(apiKeyService.ensureDefault(7L)).thenThrow(new ServiceException("API Key创建失败"));
        RegisterFacade facade = createFacade(apiKeyService, mock(PlatformTransactionManager.class));

        assertDoesNotThrow(() -> facade.onUserRegistered(new UserRegisteredEvent(7L, "newuser", null)));
    }

    @Test
    void shouldNotBreakRegisterWhenCommitFails()
    {
        // 提交阶段抛异常对应 CLIProxyAPI 推送失败的时机（推送在事务提交后执行），
        // 该异常必须被注册流程内部消化，不能冒泡导致注册接口报错。
        ICpaApiKeyService apiKeyService = mock(ICpaApiKeyService.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        doThrow(new ServiceException("API Key已保存，但同步到CLIProxyAPI失败")).when(transactionManager)
                .commit(any(TransactionStatus.class));
        RegisterFacade facade = createFacade(apiKeyService, transactionManager);

        assertDoesNotThrow(() -> facade.onUserRegistered(new UserRegisteredEvent(7L, "newuser", null)));

        verify(apiKeyService).ensureDefault(7L);
    }

    private RegisterFacade createFacade(ICpaApiKeyService apiKeyService,
            PlatformTransactionManager transactionManager)
    {
        RegisterFacade facade = new RegisterFacade();
        ReflectionTestUtils.setField(facade, "inviteFacade", mock(InviteFacade.class));
        ReflectionTestUtils.setField(facade, "subscriptionService", mock(IAiSubscriptionService.class));
        ISysConfigService configService = mock(ISysConfigService.class);
        when(configService.selectConfigByKey(anyString())).thenReturn(null);
        ReflectionTestUtils.setField(facade, "configService", configService);
        ReflectionTestUtils.setField(facade, "apiKeyService", apiKeyService);
        ReflectionTestUtils.setField(facade, "transactionManager", transactionManager);
        return facade;
    }
}
