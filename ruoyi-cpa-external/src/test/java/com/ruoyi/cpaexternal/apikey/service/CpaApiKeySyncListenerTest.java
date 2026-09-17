package com.ruoyi.cpaexternal.apikey.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cpaexternal.apikey.event.CpaApiKeySyncEvent;

/** API Key 推送监听器测试。 */
class CpaApiKeySyncListenerTest
{
    @Test
    void shouldSkipWhenPushDisabled()
    {
        CpaApiKeyPushService pushService = mock(CpaApiKeyPushService.class);
        when(pushService.isEnabled()).thenReturn(false);
        ICpaApiKeyService apiKeyService = mock(ICpaApiKeyService.class);
        CpaApiKeySyncListener listener = new CpaApiKeySyncListener(pushService, apiKeyService);

        listener.onApiKeyChanged(CpaApiKeySyncEvent.create(1L, "sk-new"));

        verify(pushService, never()).upsertKey(anyString());
        verify(apiKeyService, never()).evictCpaKeysCache();
    }

    @Test
    void shouldDispatchEachOperation()
    {
        CpaApiKeyPushService pushService = mock(CpaApiKeyPushService.class);
        when(pushService.isEnabled()).thenReturn(true);
        ICpaApiKeyService apiKeyService = mock(ICpaApiKeyService.class);
        CpaApiKeySyncListener listener = new CpaApiKeySyncListener(pushService, apiKeyService);

        listener.onApiKeyChanged(CpaApiKeySyncEvent.create(1L, "sk-new"));
        listener.onApiKeyChanged(CpaApiKeySyncEvent.replace(1L, "sk-old", "sk-new"));
        listener.onApiKeyChanged(CpaApiKeySyncEvent.remove(1L, "sk-old"));

        verify(pushService).upsertKey("sk-new");
        verify(pushService).replaceKey("sk-old", "sk-new");
        verify(pushService).removeKey("sk-old");
        // 每次推送成功都应失效列表展示缓存
        verify(apiKeyService, times(3)).evictCpaKeysCache();
    }

    @Test
    void shouldThrowWithCompensationHintWhenPushFails()
    {
        CpaApiKeyPushService pushService = mock(CpaApiKeyPushService.class);
        when(pushService.isEnabled()).thenReturn(true);
        doThrow(new RuntimeException("connection refused")).when(pushService).upsertKey(anyString());
        ICpaApiKeyService apiKeyService = mock(ICpaApiKeyService.class);
        CpaApiKeySyncListener listener = new CpaApiKeySyncListener(pushService, apiKeyService);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> listener.onApiKeyChanged(CpaApiKeySyncEvent.create(1L, "sk-new")));
        assertTrue(exception.getMessage().contains("已保存"));
        assertTrue(exception.getMessage().contains("自动重试同步"));
        verify(apiKeyService, never()).evictCpaKeysCache();
    }
}
