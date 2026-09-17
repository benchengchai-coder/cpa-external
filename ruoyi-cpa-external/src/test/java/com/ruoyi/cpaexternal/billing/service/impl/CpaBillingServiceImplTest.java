package com.ruoyi.cpaexternal.billing.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingConstants;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingReserveRequest;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingReserveResult;
import com.ruoyi.cpaexternal.billing.mapper.CpaBillingRecordMapper;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysConfigService;

/** CLIProxyAPI 预占入口的校验与幂等回放测试。 */
class CpaBillingServiceImplTest
{
    private static final String REQUEST_ID = "1a2b3c4d";
    private static final String PLAIN_KEY = "sk-plain-key-12345";
    private static final Long USER_ID = 100L;
    private static final Long KEY_ID = 9L;

    private SysUserMapper sysUserMapper;
    private AiUserSubscriptionMapper userSubscriptionMapper;
    private CpaBillingRecordMapper billingRecordMapper;
    private ICpaApiKeyService apiKeyService;
    private ISysConfigService configService;
    private CpaBillingServiceImpl service;

    @BeforeEach
    void setUp() throws Exception
    {
        sysUserMapper = mock(SysUserMapper.class);
        userSubscriptionMapper = mock(AiUserSubscriptionMapper.class);
        billingRecordMapper = mock(CpaBillingRecordMapper.class);
        apiKeyService = mock(ICpaApiKeyService.class);
        configService = mock(ISysConfigService.class);
        service = new CpaBillingServiceImpl();
        setField("sysUserMapper", sysUserMapper);
        setField("userSubscriptionMapper", userSubscriptionMapper);
        setField("billingRecordMapper", billingRecordMapper);
        setField("apiKeyService", apiKeyService);
        setField("configService", configService);
        setField("billingProperties", new CpaBillingProperties());
    }

    /** 同一 request_id 重复预占且账单可结算时，回放既有预占结果且不重复冻结。 */
    @Test
    void reserveShouldReplayExistingReservedBill()
    {
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(activeKey());
        when(sysUserMapper.selectUserBillingSnapshot(USER_ID)).thenReturn(normalUser());
        when(configService.selectConfigByKey(CpaBillingConstants.CONFIG_RESERVE_AMOUNT)).thenReturn("0.01");
        CpaBillingRecord existing = new CpaBillingRecord();
        existing.setRequestId(REQUEST_ID);
        existing.setUserId(USER_ID);
        existing.setKeyId(KEY_ID);
        existing.setStatus(CpaBillingConstants.STATUS_RESERVED);
        existing.setReservedAmount(new BigDecimal("0.01"));
        existing.setWalletReservedAmount(new BigDecimal("0.01"));
        existing.setKeyReservedAmount(new BigDecimal("0.01"));
        when(billingRecordMapper.insertIgnore(any(CpaBillingRecord.class))).thenReturn(0);
        when(billingRecordMapper.selectByRequestId(REQUEST_ID)).thenReturn(existing);

        CpaBillingReserveResult result = service.reserve(request());

        assertTrue(result.isAllowed());
        assertEquals(new BigDecimal("0.01"), result.getReservedAmount());
        verify(sysUserMapper, never()).freezeUserBalance(any(), any());
        // 回放不重复占用并发额度
        verify(sysUserMapper, never()).incrementUserActiveRequestCount(USER_ID);
    }

    /** 同一 request_id 已被释放（终态）时拒绝重复预占。 */
    @Test
    void reserveShouldRejectReleasedRequestId()
    {
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(activeKey());
        when(sysUserMapper.selectUserBillingSnapshot(USER_ID)).thenReturn(normalUser());
        when(configService.selectConfigByKey(CpaBillingConstants.CONFIG_RESERVE_AMOUNT)).thenReturn("0.01");
        CpaBillingRecord existing = new CpaBillingRecord();
        existing.setStatus(CpaBillingConstants.STATUS_RELEASED);
        when(billingRecordMapper.insertIgnore(any(CpaBillingRecord.class))).thenReturn(0);
        when(billingRecordMapper.selectByRequestId(REQUEST_ID)).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reserve(request()));
        assertEquals("request_id已存在，无法重复预占", exception.getMessage());
    }

    @Test
    void reserveShouldRejectInvalidApiKey()
    {
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reserve(request()));
        assertEquals("API Key无效", exception.getMessage());
    }

    @Test
    void reserveShouldRejectMissingRequestId()
    {
        CpaBillingReserveRequest request = new CpaBillingReserveRequest();
        request.setApiKey(PLAIN_KEY);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reserve(request));
        assertEquals("request_id不能为空", exception.getMessage());
    }

    /** 并发占位失败（条件自增影响0行）时拒绝预占，且不触碰资金冻结。 */
    @Test
    void reserveShouldRejectWhenConcurrencyLimitReached()
    {
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(activeKey());
        when(sysUserMapper.selectUserBillingSnapshot(USER_ID)).thenReturn(userWithConcurrency(5, 5));
        when(configService.selectConfigByKey(CpaBillingConstants.CONFIG_RESERVE_AMOUNT)).thenReturn("0.01");
        when(billingRecordMapper.insertIgnore(any(CpaBillingRecord.class))).thenReturn(1);
        when(sysUserMapper.incrementUserActiveRequestCount(USER_ID)).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reserve(request()));

        assertEquals("AI并发数已达上限(5)", exception.getMessage());
        verify(sysUserMapper, never()).freezeUserBalance(any(), any());
    }

    /** 并发上限为 0 视为禁用该用户 AI 访问。 */
    @Test
    void reserveShouldRejectConcurrencyDisabledUser()
    {
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(activeKey());
        when(sysUserMapper.selectUserBillingSnapshot(USER_ID)).thenReturn(userWithConcurrency(0, 0));
        when(configService.selectConfigByKey(CpaBillingConstants.CONFIG_RESERVE_AMOUNT)).thenReturn("0.01");
        when(billingRecordMapper.insertIgnore(any(CpaBillingRecord.class))).thenReturn(1);
        when(sysUserMapper.incrementUserActiveRequestCount(USER_ID)).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reserve(request()));

        assertEquals("AI并发上限为0，用户已被禁用AI访问", exception.getMessage());
        verify(sysUserMapper, never()).freezeUserBalance(any(), any());
    }

    /** 新占位路径：自增成功后继续冻结，响应携带并发上限与在途参考值。 */
    @Test
    void reserveShouldOccupyConcurrencyAndFreezeOnSuccess()
    {
        when(apiKeyService.selectByPlainKey(PLAIN_KEY)).thenReturn(activeKey());
        SysUser user = userWithConcurrency(5, 2);
        user.setBillingPreference("wallet_only");
        when(sysUserMapper.selectUserBillingSnapshot(USER_ID)).thenReturn(user);
        when(configService.selectConfigByKey(CpaBillingConstants.CONFIG_RESERVE_AMOUNT)).thenReturn("0.01");
        when(billingRecordMapper.insertIgnore(any(CpaBillingRecord.class))).thenReturn(1);
        when(sysUserMapper.incrementUserActiveRequestCount(USER_ID)).thenReturn(1);
        when(sysUserMapper.freezeUserBalance(eq(USER_ID), any())).thenReturn(1);
        when(billingRecordMapper.updateReserved(any(CpaBillingRecord.class))).thenReturn(1);

        CpaBillingReserveResult result = service.reserve(request());

        assertTrue(result.isAllowed());
        verify(sysUserMapper).incrementUserActiveRequestCount(USER_ID);
        assertEquals(Integer.valueOf(5), result.getConcurrencyLimit());
        assertEquals(Integer.valueOf(3), result.getActiveRequestCount());
    }

    /** release 出口（reserved→released）在状态迁移成功后递减并发计数。 */
    @Test
    void releaseShouldDecrementConcurrencyAfterTerminal()
    {
        CpaBillingRecord record = new CpaBillingRecord();
        record.setBillingId(600L);
        record.setRequestId(REQUEST_ID);
        record.setUserId(USER_ID);
        record.setStatus(CpaBillingConstants.STATUS_RESERVED);
        record.setWalletReservedAmount(new BigDecimal("0.01"));
        when(billingRecordMapper.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(record);
        when(sysUserMapper.releaseUserFrozenBalance(USER_ID, new BigDecimal("0.01"))).thenReturn(1);
        when(billingRecordMapper.updateTerminal(eq(600L), eq(CpaBillingConstants.STATUS_RELEASED), any()))
                .thenReturn(1);

        service.release(REQUEST_ID, "rejected_by_gate");

        verify(sysUserMapper).decrementUserActiveRequestCount(USER_ID);
    }

    /** 账单不在 reserved 状态时 release 直接跳过，不递减并发计数。 */
    @Test
    void releaseShouldSkipDecrementWhenNotReserved()
    {
        CpaBillingRecord record = new CpaBillingRecord();
        record.setRequestId(REQUEST_ID);
        record.setUserId(USER_ID);
        record.setStatus(CpaBillingConstants.STATUS_RELEASED);
        when(billingRecordMapper.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(record);

        service.release(REQUEST_ID, "rejected_by_gate");

        verify(sysUserMapper, never()).decrementUserActiveRequestCount(USER_ID);
    }

    private CpaBillingReserveRequest request()
    {
        CpaBillingReserveRequest request = new CpaBillingReserveRequest();
        request.setRequestId(REQUEST_ID);
        request.setApiKey(PLAIN_KEY);
        return request;
    }

    private CpaApiKey activeKey()
    {
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setKeyId(KEY_ID);
        apiKey.setUserId(USER_ID);
        apiKey.setStatus("0");
        return apiKey;
    }

    private SysUser normalUser()
    {
        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        user.setStatus("0");
        user.setBillingPreference("subscription_first");
        return user;
    }

    private SysUser userWithConcurrency(Integer limit, Integer active)
    {
        SysUser user = normalUser();
        user.setAiConcurrencyLimit(limit);
        user.setActiveRequestCount(active);
        return user;
    }

    private void setField(String name, Object value) throws Exception
    {
        Field field = CpaBillingServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
