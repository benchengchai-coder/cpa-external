package com.ruoyi.cpaexternal.apikey.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.cpaexternal.apikey.client.CpaManagementClient;
import com.ruoyi.cpaexternal.apikey.config.CpaManagementProperties;

/** API Key 推送同步服务测试：GET+合并+PUT 的读改写逻辑。 */
class CpaApiKeyPushServiceTest
{
    private CpaManagementProperties properties;
    private CpaManagementClient client;
    private CpaApiKeyPushService service;

    @BeforeEach
    void setUp()
    {
        properties = new CpaManagementProperties();
        client = mock(CpaManagementClient.class);
        service = new CpaApiKeyPushService(properties, client);
    }

    @Test
    void shouldReflectPushSwitch()
    {
        assertFalse(service.isEnabled());
        properties.setApiKeyPushEnabled(true);
        assertTrue(service.isEnabled());
    }

    @Test
    void shouldAppendNewKeyKeepingExistingOrder()
    {
        when(client.listApiKeys()).thenReturn(List.of("sk-1", "sk-2"));

        service.upsertKey("sk-3");

        verify(client).putApiKeys(List.of("sk-1", "sk-2", "sk-3"));
    }

    @Test
    void shouldSkipPutWhenKeyAlreadyPresent()
    {
        when(client.listApiKeys()).thenReturn(List.of("sk-1", "sk-2"));

        service.upsertKey("sk-2");

        verify(client, never()).putApiKeys(anyList());
    }

    @Test
    void shouldReplaceOldKeyWithNewKey()
    {
        when(client.listApiKeys()).thenReturn(List.of("sk-1", "sk-old", "sk-2"));

        service.replaceKey("sk-old", "sk-new");

        // Key 在列表中的位置对 CPA 无语义，替换实现为移除旧值后追加新值。
        verify(client).putApiKeys(List.of("sk-1", "sk-2", "sk-new"));
    }

    @Test
    void shouldAppendNewKeyWhenOldKeyMissingInCpa()
    {
        // 历史数据场景：旧明文从未推送过 CPA，替换退化为追加新 Key。
        when(client.listApiKeys()).thenReturn(List.of("sk-1"));

        service.replaceKey("sk-unknown", "sk-new");

        verify(client).putApiKeys(List.of("sk-1", "sk-new"));
    }

    @Test
    void shouldRemoveExistingKey()
    {
        when(client.listApiKeys()).thenReturn(List.of("sk-1", "sk-2"));

        service.removeKey("sk-1");

        verify(client).putApiKeys(List.of("sk-2"));
    }

    @Test
    void shouldSkipPutWhenRemovingAbsentKey()
    {
        when(client.listApiKeys()).thenReturn(List.of("sk-1"));

        service.removeKey("sk-gone");

        verify(client, never()).putApiKeys(anyList());
    }
}
