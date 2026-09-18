package com.ruoyi.cpaexternal.log.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettleCommand;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingSettlementService;
import com.ruoyi.cpaexternal.log.billing.CpaAiLogCostCalculator;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogPayload;
import com.ruoyi.cpaexternal.log.mapper.CpaAiLogMapper;
import com.ruoyi.system.service.ISysUserService;
import tools.jackson.databind.ObjectMapper;

/** CLIProxyAPI 日志入库的计费倍率快照与时间格式化测试。 */
class CpaAiLogServiceImplTest
{
    private static final String PLAIN_KEY = "sk-plain-key-12345";
    private static final Long USER_ID = 100L;

    private CpaAiLogMapper aiLogMapper;
    private ICpaApiKeyService apiKeyService;
    private ISysUserService sysUserService;
    private CpaAiLogCostCalculator costCalculator;
    private ICpaBillingSettlementService billingSettlementService;
    private CpaAiLogServiceImpl service;

    @BeforeEach
    void setUp() throws Exception
    {
        aiLogMapper = mock(CpaAiLogMapper.class);
        apiKeyService = mock(ICpaApiKeyService.class);
        sysUserService = mock(ISysUserService.class);
        costCalculator = mock(CpaAiLogCostCalculator.class);
        billingSettlementService = mock(ICpaBillingSettlementService.class);
        service = new CpaAiLogServiceImpl();
        setField("aiLogMapper", aiLogMapper);
        setField("apiKeyService", apiKeyService);
        setField("sysUserService", sysUserService);
        setField("costCalculator", costCalculator);
        setField("billingSettlementService", billingSettlementService);
        setField("objectMapper", mock(ObjectMapper.class));
    }

    @Test
    void ingestShouldSnapshotOwnerBillingMultiplier()
    {
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(null);
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(apiKey());
        when(sysUserService.selectUserById(USER_ID)).thenReturn(user(new BigDecimal("1.50")));
        when(costCalculator.calculate(any(CpaAiLogPayload.class), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("0.0123456789"));

        CpaAiLog inserted = captureIngest(payload());

        assertEquals(new BigDecimal("1.50"), inserted.getBillingMultiplier());
        assertEquals("tester", inserted.getUsername());
        // 费用按快照后的倍率计算，保证与页面展示的倍率一致。
        verify(costCalculator).calculate(any(CpaAiLogPayload.class), eq(new BigDecimal("1.50")));
        assertEquals(new BigDecimal("0.0123456789"), inserted.getCost());
    }

    @Test
    void ingestShouldKeepMultiplierNullWhenApiKeyUnresolved()
    {
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(null);
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(null);

        CpaAiLog inserted = captureIngest(payload());

        assertNull(inserted.getBillingMultiplier());
        assertNull(inserted.getUserId());
        // 用户无法归属时倍率未知，按官方价（倍率 1）计算。
        verify(costCalculator).calculate(any(CpaAiLogPayload.class), isNull());
    }

    /** CLIProxyAPI 上报的 ISO-8601 时间需要在入库前格式化成 datetime。 */
    @Test
    void ingestShouldFormatReportedTimestampAsDatetime()
    {
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(null);
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(null);
        CpaAiLogPayload payload = payload();
        payload.setTimestamp("2026-09-10T10:59:53.738095654+08:00");

        CpaAiLog inserted = captureIngest(payload);

        // 纳秒截断到毫秒，+08:00 偏移换算成绝对时间点。
        assertEquals(Instant.parse("2026-09-10T02:59:53.738Z"), inserted.getTimestamp().toInstant());
        assertEquals(inserted.getTimestamp(), inserted.getRequestTime());
    }

    /** 时间缺失或无法解析时回退为入库时间，避免丢掉整条用量记录。 */
    @Test
    void ingestShouldFallbackToCurrentTimeWhenTimestampUnusable()
    {
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(null);
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(null);
        CpaAiLogPayload payload = payload();
        payload.setTimestamp("  ");

        CpaAiLog inserted = captureIngest(payload);

        assertNotNull(inserted.getTimestamp());
    }

    /** 凭据重试的首条失败 usage 应被后续成功 usage 覆盖并重算费用。 */
    @Test
    void ingestShouldReplaceFailedRecordWithSuccessfulRetry()
    {
        CpaAiLog existing = new CpaAiLog();
        existing.setLogId(7L);
        existing.setRequestId("req-1");
        existing.setFailed(true);
        existing.setUserId(USER_ID);
        existing.setKeyId(9L);
        existing.setBillingMultiplier(new BigDecimal("1.50"));
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(existing);
        when(costCalculator.calculate(any(CpaAiLogPayload.class), eq(new BigDecimal("1.50"))))
                .thenReturn(new BigDecimal("0.02"));

        CpaAiLogPayload retry = payload();
        retry.setFailed(false);
        CpaAiLog updated = service.ingest(retry);

        ArgumentCaptor<CpaAiLog> captor = ArgumentCaptor.forClass(CpaAiLog.class);
        verify(aiLogMapper).updateUsageByRequestId(captor.capture());
        verify(aiLogMapper, never()).insert(any(CpaAiLog.class));
        assertEquals(Boolean.FALSE, captor.getValue().getFailed());
        assertEquals(new BigDecimal("0.02"), captor.getValue().getCost());
        // 归属信息沿用首次入库结果，不重复解析。
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(9L, captor.getValue().getKeyId());
        assertEquals(7L, updated.getLogId());

        ArgumentCaptor<CpaBillingSettleCommand> commandCaptor = ArgumentCaptor.forClass(CpaBillingSettleCommand.class);
        verify(billingSettlementService).submitSettlement(commandCaptor.capture());
        assertEquals("req-1", commandCaptor.getValue().getRequestId());
    }

    /** 全部尝试都失败时保留首条记录，不再覆盖。 */
    @Test
    void ingestShouldKeepExistingRecordWhenRetryAlsoFailed()
    {
        CpaAiLog existing = new CpaAiLog();
        existing.setRequestId("req-1");
        existing.setFailed(true);
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(existing);

        CpaAiLogPayload retry = payload();
        retry.setFailed(true);
        CpaAiLog result = service.ingest(retry);

        assertSame(existing, result);
        verify(aiLogMapper, never()).updateUsageByRequestId(any(CpaAiLog.class));
        verify(aiLogMapper, never()).insert(any(CpaAiLog.class));
    }

    /** 失败 usage 即使携带 token 也必须按零费用落库，不能进入计费链路。 */
    @Test
    void ingestShouldForceFailedUsageCostToZero()
    {
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(null);
        CpaAiLogPayload failedPayload = payload();
        failedPayload.setFailed(true);

        CpaAiLog inserted = captureIngest(failedPayload);

        assertEquals(BigDecimal.ZERO, inserted.getCost());
        verify(costCalculator, never()).calculate(any(CpaAiLogPayload.class), any());
    }

    /** 已是成功记录时后续重复 usage 不覆盖。 */
    @Test
    void ingestShouldNotReplaceSuccessfulRecord()
    {
        CpaAiLog existing = new CpaAiLog();
        existing.setRequestId("req-1");
        existing.setFailed(false);
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(existing);

        CpaAiLogPayload duplicate = payload();
        duplicate.setFailed(false);
        CpaAiLog result = service.ingest(duplicate);

        assertSame(existing, result);
        verify(aiLogMapper, never()).updateUsageByRequestId(any(CpaAiLog.class));
    }

    /** CLIProxyAPI 以「HTTP方法 路径」格式上报端点，入库前应剥掉方法前缀只保留路径。 */
    @Test
    void ingestShouldStripHttpMethodFromEndpoint()
    {
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(null);
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(null);
        CpaAiLogPayload payload = payload();
        payload.setEndpoint("POST /v1/responses");

        CpaAiLog inserted = captureIngest(payload);

        assertEquals("/v1/responses", inserted.getEndpoint());
    }

    /** 不含方法前缀的端点值原样保留，兼容上报格式变化。 */
    @Test
    void ingestShouldKeepEndpointWithoutMethodPrefix()
    {
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(null);
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(null);
        CpaAiLogPayload payload = payload();
        payload.setEndpoint("/v1/responses");

        CpaAiLog inserted = captureIngest(payload);

        assertEquals("/v1/responses", inserted.getEndpoint());
    }

    /** 首次入库同样要触发结算提交。 */
    @Test
    void ingestShouldSubmitSettlementAfterFirstInsert()
    {
        when(aiLogMapper.selectByRequestId("req-1")).thenReturn(null);
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(null);

        service.ingest(payload());

        ArgumentCaptor<CpaBillingSettleCommand> commandCaptor = ArgumentCaptor.forClass(CpaBillingSettleCommand.class);
        verify(billingSettlementService).submitSettlement(commandCaptor.capture());
        assertEquals("req-1", commandCaptor.getValue().getRequestId());
    }

    /** 执行一次入库并返回真正传给 Mapper 的日志对象。 */
    private CpaAiLog captureIngest(CpaAiLogPayload payload)
    {
        service.ingest(payload);
        ArgumentCaptor<CpaAiLog> captor = ArgumentCaptor.forClass(CpaAiLog.class);
        verify(aiLogMapper).insert(captor.capture());
        return captor.getValue();
    }

    private CpaAiLogPayload payload()
    {
        CpaAiLogPayload payload = new CpaAiLogPayload();
        payload.setRequestId("req-1");
        payload.setApiKey(PLAIN_KEY);
        return payload;
    }

    private CpaApiKey apiKey()
    {
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setKeyId(9L);
        apiKey.setKeyName("默认密钥");
        apiKey.setUserId(USER_ID);
        return apiKey;
    }

    private SysUser user(BigDecimal billingMultiplier)
    {
        SysUser user = new SysUser();
        user.setUserName("tester");
        user.setBillingMultiplier(billingMultiplier);
        return user;
    }

    private void setField(String name, Object value) throws Exception
    {
        Field field = CpaAiLogServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
