package com.ruoyi.cpaexternal.apikey.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKeySyncStatus;
import com.ruoyi.cpaexternal.apikey.domain.vo.CpaApiKeySyncVO;
import com.ruoyi.cpaexternal.apikey.event.CpaApiKeySyncEvent;
import com.ruoyi.cpaexternal.apikey.mapper.CpaApiKeyMapper;
import com.ruoyi.cpaexternal.apikey.client.CpaManagementClient;
import com.ruoyi.cpaexternal.apikey.client.CpaManagementException;
import com.ruoyi.cpaexternal.apikey.config.CpaManagementProperties;
import com.ruoyi.common.exception.ServiceException;

/** API Key 自定义密钥逻辑测试。 */
class CpaApiKeyServiceImplTest
{
    private static final String CUSTOM_KEY = "sk-custom-key-12345";

    private CpaApiKeyMapper apiKeyMapper;
    private ApplicationEventPublisher eventPublisher;
    private CpaManagementClient managementClient;
    private CpaManagementProperties managementProperties;
    private CpaApiKeyServiceImpl service;

    @BeforeEach
    void setUp() throws Exception
    {
        apiKeyMapper = mock(CpaApiKeyMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        managementClient = mock(CpaManagementClient.class);
        managementProperties = new CpaManagementProperties();
        service = new CpaApiKeyServiceImpl();
        setField("apiKeyMapper", apiKeyMapper);
        setField("eventPublisher", eventPublisher);
        setField("managementClient", managementClient);
        setField("managementProperties", managementProperties);
    }

    private void setField(String name, Object value) throws Exception
    {
        Field field = CpaApiKeyServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    @Test
    void shouldUseCustomKeyWhenCreating()
    {
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setUserId(1L);
        apiKey.setApiKey(CUSTOM_KEY);
        when(apiKeyMapper.countByUserId(1L)).thenReturn(0);
        when(apiKeyMapper.selectByPlainKey(CUSTOM_KEY)).thenReturn(null);
        AtomicReference<String> insertedKey = new AtomicReference<>();
        when(apiKeyMapper.insert(any(CpaApiKey.class))).thenAnswer(invocation ->
        {
            CpaApiKey argument = invocation.getArgument(0);
            insertedKey.set(argument.getApiKey());
            return 1;
        });

        CpaApiKey result = service.create(apiKey);

        assertEquals(CUSTOM_KEY, result.getApiKey());
        // 明文存储：入库值与明文一致。
        assertEquals(CUSTOM_KEY, insertedKey.get());
    }

    @Test
    void shouldGenerateUniqueKeyWhenCreating()
    {
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setUserId(1L);
        when(apiKeyMapper.countByUserId(1L)).thenReturn(0);
        when(apiKeyMapper.selectByPlainKey(anyString())).thenReturn(null);
        AtomicReference<String> insertedKey = new AtomicReference<>();
        when(apiKeyMapper.insert(any(CpaApiKey.class))).thenAnswer(invocation ->
        {
            CpaApiKey argument = invocation.getArgument(0);
            insertedKey.set(argument.getApiKey());
            return 1;
        });

        CpaApiKey result = service.create(apiKey);

        assertEquals(insertedKey.get(), result.getApiKey());
        verify(apiKeyMapper).selectByPlainKey(insertedKey.get());
    }

    @Test
    void shouldQueryByPlainKeyDirectly()
    {
        CpaApiKey expected = new CpaApiKey();
        when(apiKeyMapper.selectByPlainKey(CUSTOM_KEY)).thenReturn(expected);

        assertEquals(expected, service.selectByPlainKey(CUSTOM_KEY));
        verify(apiKeyMapper).selectByPlainKey(CUSTOM_KEY);
    }

    @Test
    void shouldRejectInvalidCustomKeyWhenCreating()
    {
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setUserId(1L);
        apiKey.setApiKey("no-prefix-key");
        when(apiKeyMapper.countByUserId(1L)).thenReturn(0);

        assertThrows(ServiceException.class, () -> service.create(apiKey));
        verify(apiKeyMapper, never()).insert(any(CpaApiKey.class));
    }

    @Test
    void shouldRejectDuplicatedCustomKeyWhenCreating()
    {
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setUserId(1L);
        apiKey.setApiKey(CUSTOM_KEY);
        CpaApiKey existing = new CpaApiKey();
        existing.setKeyId(9L);
        when(apiKeyMapper.countByUserId(1L)).thenReturn(0);
        when(apiKeyMapper.selectByPlainKey(CUSTOM_KEY)).thenReturn(existing);

        assertThrows(ServiceException.class, () -> service.create(apiKey));
        verify(apiKeyMapper, never()).insert(any(CpaApiKey.class));
    }

    @Test
    void shouldCustomizeSecret()
    {
        CpaApiKey current = new CpaApiKey();
        current.setKeyId(5L);
        current.setUserId(1L);
        when(apiKeyMapper.selectById(5L)).thenReturn(current);
        when(apiKeyMapper.selectByPlainKey(CUSTOM_KEY)).thenReturn(null);
        when(apiKeyMapper.updateSecret(eq(5L), anyString())).thenReturn(1);

        CpaApiKey result = service.customizeSecret(5L, CUSTOM_KEY);

        assertEquals(CUSTOM_KEY, result.getApiKey());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(apiKeyMapper).updateSecret(eq(5L), captor.capture());
        // 明文存储：更新值与明文一致。
        assertEquals(CUSTOM_KEY, captor.getValue());
    }

    @Test
    void shouldRejectCustomizeWhenKeyMissing()
    {
        when(apiKeyMapper.selectById(5L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.customizeSecret(5L, CUSTOM_KEY));
        verify(apiKeyMapper, never()).updateSecret(any(), anyString());
    }

    @Test
    void shouldRejectCustomizeWhenDuplicated()
    {
        CpaApiKey current = new CpaApiKey();
        current.setKeyId(5L);
        CpaApiKey existing = new CpaApiKey();
        existing.setKeyId(9L);
        when(apiKeyMapper.selectById(5L)).thenReturn(current);
        when(apiKeyMapper.selectByPlainKey(CUSTOM_KEY)).thenReturn(existing);

        assertThrows(ServiceException.class, () -> service.customizeSecret(5L, CUSTOM_KEY));
        verify(apiKeyMapper, never()).updateSecret(any(), anyString());
    }

    @Test
    void shouldAllowSameCustomKeyForSameRecord()
    {
        CpaApiKey current = new CpaApiKey();
        current.setKeyId(5L);
        when(apiKeyMapper.selectById(5L)).thenReturn(current);
        // 命中的是当前记录自身，不算冲突。
        when(apiKeyMapper.selectByPlainKey(CUSTOM_KEY)).thenReturn(current);
        when(apiKeyMapper.updateSecret(eq(5L), anyString())).thenReturn(1);

        CpaApiKey result = service.customizeSecret(5L, CUSTOM_KEY);

        assertEquals(CUSTOM_KEY, result.getApiKey());
    }

    @Test
    void shouldRejectCustomizeWhenFormatInvalid()
    {
        CpaApiKey current = new CpaApiKey();
        current.setKeyId(5L);
        when(apiKeyMapper.selectById(5L)).thenReturn(current);

        assertThrows(ServiceException.class, () -> service.customizeSecret(5L, "bad key with space"));
        assertThrows(ServiceException.class, () -> service.customizeSecret(5L, "sk-ab"));
        verify(apiKeyMapper, never()).updateSecret(any(), anyString());
    }

    @Test
    void shouldPublishCreateEventWhenCreating()
    {
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setUserId(1L);
        apiKey.setKeyId(7L);
        when(apiKeyMapper.countByUserId(1L)).thenReturn(0);
        when(apiKeyMapper.selectByPlainKey(anyString())).thenReturn(null);
        when(apiKeyMapper.insert(any(CpaApiKey.class))).thenAnswer(invocation ->
        {
            CpaApiKey argument = invocation.getArgument(0);
            argument.setKeyId(7L);
            return 1;
        });

        CpaApiKey result = service.create(apiKey);

        ArgumentCaptor<CpaApiKeySyncEvent> captor = ArgumentCaptor.forClass(CpaApiKeySyncEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(CpaApiKeySyncEvent.Operation.CREATE, captor.getValue().getOperation());
        assertEquals(Long.valueOf(7L), captor.getValue().getKeyId());
        assertEquals(result.getApiKey(), captor.getValue().getNewPlainKey());
    }

    @Test
    void shouldPublishReplaceEventWhenRotating()
    {
        CpaApiKey current = new CpaApiKey();
        current.setKeyId(5L);
        current.setUserId(1L);
        current.setApiKey("sk-old-value");
        when(apiKeyMapper.selectByUserId(1L)).thenReturn(current);
        when(apiKeyMapper.selectByPlainKey(anyString())).thenReturn(null);
        when(apiKeyMapper.updateSecret(eq(5L), anyString())).thenReturn(1);

        CpaApiKey result = service.rotate(1L);

        ArgumentCaptor<CpaApiKeySyncEvent> captor = ArgumentCaptor.forClass(CpaApiKeySyncEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(CpaApiKeySyncEvent.Operation.REPLACE, captor.getValue().getOperation());
        assertEquals("sk-old-value", captor.getValue().getOldPlainKey());
        assertEquals(result.getApiKey(), captor.getValue().getNewPlainKey());
    }

    @Test
    void shouldPublishReplaceEventWhenCustomizing()
    {
        CpaApiKey current = new CpaApiKey();
        current.setKeyId(5L);
        current.setApiKey("sk-old-value");
        when(apiKeyMapper.selectById(5L)).thenReturn(current);
        when(apiKeyMapper.selectByPlainKey(CUSTOM_KEY)).thenReturn(null);
        when(apiKeyMapper.updateSecret(eq(5L), anyString())).thenReturn(1);

        service.customizeSecret(5L, CUSTOM_KEY);

        ArgumentCaptor<CpaApiKeySyncEvent> captor = ArgumentCaptor.forClass(CpaApiKeySyncEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(CpaApiKeySyncEvent.Operation.REPLACE, captor.getValue().getOperation());
        assertEquals("sk-old-value", captor.getValue().getOldPlainKey());
        assertEquals(CUSTOM_KEY, captor.getValue().getNewPlainKey());
    }

    @Test
    void shouldPublishRemoveEventOnlyForActuallyDeletedKeys()
    {
        CpaApiKey first = buildPersistedKey(1L, "sk-first");
        CpaApiKey second = buildPersistedKey(2L, "sk-second");
        // 删除前两条都存在；删除后仅第一条被删除（frozen_balance=0 限制导致第二条残留）。
        when(apiKeyMapper.selectById(1L)).thenReturn(first, null);
        when(apiKeyMapper.selectById(2L)).thenReturn(second, second);
        when(apiKeyMapper.deleteByIds(any(Long[].class))).thenReturn(1);

        int rows = service.deleteByIds(new Long[] { 1L, 2L });

        assertEquals(1, rows);
        ArgumentCaptor<CpaApiKeySyncEvent> captor = ArgumentCaptor.forClass(CpaApiKeySyncEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertEquals(CpaApiKeySyncEvent.Operation.REMOVE, captor.getValue().getOperation());
        assertEquals("sk-first", captor.getValue().getOldPlainKey());
    }

    @Test
    void shouldPublishRemoveEventWhenDisablingKey()
    {
        CpaApiKey current = buildPersistedKey(5L, "sk-live");
        current.setStatus("0");
        when(apiKeyMapper.selectById(5L)).thenReturn(current);
        when(apiKeyMapper.update(any(CpaApiKey.class))).thenReturn(1);

        CpaApiKey incoming = new CpaApiKey();
        incoming.setKeyId(5L);
        incoming.setStatus("1");
        service.update(incoming);

        ArgumentCaptor<CpaApiKeySyncEvent> captor = ArgumentCaptor.forClass(CpaApiKeySyncEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(CpaApiKeySyncEvent.Operation.REMOVE, captor.getValue().getOperation());
        assertEquals("sk-live", captor.getValue().getOldPlainKey());
    }

    @Test
    void shouldPublishCreateEventWhenEnablingKey()
    {
        CpaApiKey current = buildPersistedKey(5L, "sk-live");
        current.setStatus("1");
        when(apiKeyMapper.selectById(5L)).thenReturn(current);
        when(apiKeyMapper.update(any(CpaApiKey.class))).thenReturn(1);

        CpaApiKey incoming = new CpaApiKey();
        incoming.setKeyId(5L);
        incoming.setStatus("0");
        service.update(incoming);

        ArgumentCaptor<CpaApiKeySyncEvent> captor = ArgumentCaptor.forClass(CpaApiKeySyncEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(CpaApiKeySyncEvent.Operation.CREATE, captor.getValue().getOperation());
        assertEquals("sk-live", captor.getValue().getNewPlainKey());
    }

    @Test
    void shouldNotPublishEventWhenStatusUnchangedOrAbsent()
    {
        CpaApiKey current = buildPersistedKey(5L, "sk-live");
        current.setStatus("0");
        when(apiKeyMapper.selectById(5L)).thenReturn(current);
        when(apiKeyMapper.update(any(CpaApiKey.class))).thenReturn(1);

        CpaApiKey renameOnly = new CpaApiKey();
        renameOnly.setKeyId(5L);
        renameOnly.setKeyName("新名称");
        service.update(renameOnly);

        CpaApiKey sameStatus = new CpaApiKey();
        sameStatus.setKeyId(5L);
        sameStatus.setStatus("0");
        service.update(sameStatus);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldRejectUpdateWhenKeyMissing()
    {
        when(apiKeyMapper.selectById(5L)).thenReturn(null);

        assertThrows(ServiceException.class, () -> service.update(new CpaApiKey()));
        verify(apiKeyMapper, never()).update(any(CpaApiKey.class));
    }

    private CpaApiKey buildPersistedKey(Long keyId, String plainKey)
    {
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setKeyId(keyId);
        apiKey.setUserId(1L);
        apiKey.setApiKey(plainKey);
        apiKey.setStatus("0");
        return apiKey;
    }

    @Test
    void shouldClassifyCpaSyncStatusDiffs()
    {
        // CPA 有：已同步、停用残留、未登记孤儿；平台启用另有缺失 Key
        when(managementClient.listApiKeys()).thenReturn(List.of(
                "sk-synced-key", "sk-residual-key", "sk-orphan-manual-123456"));
        CpaApiKey synced = buildPersistedKey(1L, "sk-synced-key");
        CpaApiKey missing = buildPersistedKey(2L, "sk-missing-key");
        missing.setKeyName("未同步");
        CpaApiKey residual = buildPersistedKey(3L, "sk-residual-key");
        residual.setStatus("1");
        when(apiKeyMapper.selectList(any(CpaApiKey.class))).thenReturn(List.of(synced, missing, residual));
        managementProperties.setApiKeyPushEnabled(true);

        CpaApiKeySyncStatus status = service.selectCpaSyncStatus();

        assertEquals(3, status.getCpaKeyCount());
        assertEquals(3, status.getPlatformKeyCount());
        assertEquals(1, status.getSyncedCount());
        assertEquals(1, status.getMissingInCpa().size());
        assertEquals(Long.valueOf(2L), status.getMissingInCpa().get(0).getKeyId());
        assertEquals("未同步", status.getMissingInCpa().get(0).getKeyName());
        assertEquals(1, status.getDisabledResidual().size());
        assertEquals(Long.valueOf(3L), status.getDisabledResidual().get(0).getKeyId());
        assertEquals(1, status.getOrphanInCpa().size());
        // 未登记 Key 只展示脱敏明文：前8后4，中间 ****
        assertEquals("sk-orpha****3456", status.getOrphanInCpa().get(0));
        assertTrue(status.isPushEnabled());
    }

    @Test
    void shouldReportAllSyncedWhenNoDiff()
    {
        when(managementClient.listApiKeys()).thenReturn(List.of("sk-a", "sk-b"));
        when(apiKeyMapper.selectList(any(CpaApiKey.class)))
                .thenReturn(List.of(buildPersistedKey(1L, "sk-a"), buildPersistedKey(2L, "sk-b")));

        CpaApiKeySyncStatus status = service.selectCpaSyncStatus();

        assertEquals(2, status.getSyncedCount());
        assertTrue(status.getMissingInCpa().isEmpty());
        assertTrue(status.getDisabledResidual().isEmpty());
        assertTrue(status.getOrphanInCpa().isEmpty());
    }

    @Test
    void shouldWrapCpaFailureWithReadableMessage()
    {
        when(managementClient.listApiKeys()).thenThrow(new CpaManagementException("HTTP 401: invalid management key"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.selectCpaSyncStatus());
        assertTrue(exception.getMessage().contains("无法连接CLIProxyAPI"));
        assertTrue(exception.getMessage().contains("HTTP 401"));
    }

    @Test
    void shouldDecorateListRowsWithCpaSyncStatus()
    {
        when(managementClient.listApiKeys()).thenReturn(List.of("sk-synced", "sk-residual"));
        CpaApiKey synced = buildPersistedKey(1L, "sk-synced");
        CpaApiKey missing = buildPersistedKey(2L, "sk-missing");
        CpaApiKey residual = buildPersistedKey(3L, "sk-residual");
        residual.setStatus("1");
        CpaApiKey disabledClean = buildPersistedKey(4L, "sk-clean");
        disabledClean.setStatus("1");
        when(apiKeyMapper.selectList(any(CpaApiKey.class)))
                .thenReturn(List.of(synced, missing, residual, disabledClean));

        List<CpaApiKeySyncVO> rows = service.selectListWithCpaSync(new CpaApiKey());

        assertEquals(4, rows.size());
        assertEquals("synced", rows.get(0).getCpaSyncStatus());
        assertEquals("missing", rows.get(1).getCpaSyncStatus());
        assertEquals("residual", rows.get(2).getCpaSyncStatus());
        // 停用且不在 CPA = 状态正确，视为已同步
        assertEquals("synced", rows.get(3).getCpaSyncStatus());
        assertEquals(Long.valueOf(1L), rows.get(0).getKeyId());
        assertEquals("sk-synced", rows.get(0).getApiKey());
    }

    @Test
    void shouldReuseCachedCpaKeysWithinTtl()
    {
        when(managementClient.listApiKeys()).thenReturn(List.of("sk-a"));
        when(apiKeyMapper.selectList(any(CpaApiKey.class))).thenReturn(List.of(buildPersistedKey(1L, "sk-a")));

        service.selectListWithCpaSync(new CpaApiKey());
        service.selectListWithCpaSync(new CpaApiKey());

        verify(managementClient, times(1)).listApiKeys();
    }

    @Test
    void shouldReloadCpaKeysAfterEvict()
    {
        when(managementClient.listApiKeys()).thenReturn(List.of("sk-a"));
        when(apiKeyMapper.selectList(any(CpaApiKey.class))).thenReturn(List.of(buildPersistedKey(1L, "sk-a")));

        service.selectListWithCpaSync(new CpaApiKey());
        service.evictCpaKeysCache();
        service.selectListWithCpaSync(new CpaApiKey());

        verify(managementClient, times(2)).listApiKeys();
    }

    @Test
    void shouldMarkUnknownWhenCpaUnavailableInList()
    {
        when(managementClient.listApiKeys()).thenThrow(new CpaManagementException("connect timeout"));
        when(apiKeyMapper.selectList(any(CpaApiKey.class))).thenReturn(List.of(buildPersistedKey(1L, "sk-a")));

        List<CpaApiKeySyncVO> rows = service.selectListWithCpaSync(new CpaApiKey());

        assertEquals(1, rows.size());
        assertEquals("unknown", rows.get(0).getCpaSyncStatus());
    }
}
