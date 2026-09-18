package com.ruoyi.cpaexternal.billing.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingConstants;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementTask;
import com.ruoyi.cpaexternal.billing.mapper.CpaBillingRecordMapper;
import com.ruoyi.cpaexternal.billing.mapper.CpaBillingSettlementTaskMapper;
import com.ruoyi.cpaexternal.billing.service.CpaBillingMinimumChargeResolver;
import com.ruoyi.cpaexternal.log.mapper.CpaAiLogMapper;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;

/** 结算链路并发计数测试：预占移除后计数列不再递增，递减仅保留在人工核销处置出口清理遗留占位。 */
class CpaBillingSettlementServiceImplTest
{
    private static final Long USER_ID = 100L;
    private static final Long KEY_ID = 9L;
    private static final Long BILLING_ID = 600L;
    private static final Long TASK_ID = 7L;
    private static final String REQUEST_ID = "1a2b3c4d";
    private static final String CLAIM_TOKEN = "claim-token";

    private SysUserMapper sysUserMapper;
    private CpaBillingRecordMapper billingRecordMapper;
    private CpaBillingSettlementTaskMapper settlementTaskMapper;
    private CpaBillingSettlementServiceImpl service;

    @BeforeEach
    void setUp() throws Exception
    {
        sysUserMapper = mock(SysUserMapper.class);
        billingRecordMapper = mock(CpaBillingRecordMapper.class);
        settlementTaskMapper = mock(CpaBillingSettlementTaskMapper.class);
        service = new CpaBillingSettlementServiceImpl();
        setField(service, "sysUserMapper", sysUserMapper);
        setField(service, "userSubscriptionMapper", mock(AiUserSubscriptionMapper.class));
        setField(service, "billingRecordMapper", billingRecordMapper);
        setField(service, "settlementTaskMapper", settlementTaskMapper);
        setField(service, "aiLogMapper", mock(CpaAiLogMapper.class));
        setField(service, "billingProperties", new CpaBillingProperties());
        setField(service, "minimumChargeResolver", mock(CpaBillingMinimumChargeResolver.class));
        CpaBillingTransactionRetryExecutor retryExecutor = new CpaBillingTransactionRetryExecutor();
        setField(retryExecutor, "billingProperties", new CpaBillingProperties());
        setField(service, "transactionRetryExecutor", retryExecutor);
        setField(service, "transactionManager", mock(PlatformTransactionManager.class));
    }

    /** Worker 领取结算：遗留 reserved 账单迁移结算时不再触碰并发计数列，递减仅归人工核销出口。 */
    @Test
    void processClaimedTaskShouldNotDecrementConcurrencyFromReserved()
    {
        when(billingRecordMapper.selectByIdForUpdate(BILLING_ID)).thenReturn(record(CpaBillingConstants.STATUS_RESERVED));
        // 结算日志缺失按零费结算，避开金额计算分支
        when(sysUserMapper.selectUserByIdForUpdate(USER_ID)).thenReturn(settleUser());
        when(sysUserMapper.settleUserBilling(any(Long.class), any(), any(), any(), anyInt())).thenReturn(1);
        when(billingRecordMapper.updatePendingSettlement(any(CpaBillingRecord.class))).thenReturn(1);
        when(billingRecordMapper.updateSettlementFromPending(any(CpaBillingRecord.class))).thenReturn(1);
        when(settlementTaskMapper.markTaskDone(any(Long.class), any(String.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.processClaimedTask(task(), CLAIM_TOKEN));

        verify(sysUserMapper, never()).decrementUserActiveRequestCount(USER_ID);
    }

    /** 账单已处于 pending_settlement（如失败重试后再次领取）时同样不递减并发计数。 */
    @Test
    void processClaimedTaskShouldNotDecrementAgainFromPendingSettlement()
    {
        when(billingRecordMapper.selectByIdForUpdate(BILLING_ID))
                .thenReturn(record(CpaBillingConstants.STATUS_PENDING_SETTLEMENT));
        when(sysUserMapper.selectUserByIdForUpdate(USER_ID)).thenReturn(settleUser());
        when(sysUserMapper.settleUserBilling(any(Long.class), any(), any(), any(), anyInt())).thenReturn(1);
        when(billingRecordMapper.updateSettlementFromPending(any(CpaBillingRecord.class))).thenReturn(1);
        when(settlementTaskMapper.markTaskDone(any(Long.class), any(String.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.processClaimedTask(task(), CLAIM_TOKEN));

        verify(sysUserMapper, never()).decrementUserActiveRequestCount(USER_ID);
    }

    /** 人工核销 reserved 来源遗留账单：核销出口递减并发计数清理旧占位。 */
    @Test
    void writeOffShouldDecrementConcurrencyFromReserved()
    {
        when(settlementTaskMapper.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(task());
        when(billingRecordMapper.selectByIdForUpdate(BILLING_ID)).thenReturn(record(CpaBillingConstants.STATUS_RESERVED));
        when(sysUserMapper.releaseUserFrozenBalance(USER_ID, new BigDecimal("0.01"))).thenReturn(1);
        when(billingRecordMapper.updateWrittenOff(eq(BILLING_ID), any(String.class), any(String.class))).thenReturn(1);
        when(settlementTaskMapper.markTaskResolved(any(Long.class))).thenReturn(1);

        service.writeOffFailedTask(REQUEST_ID, "人工核销", "admin");

        verify(sysUserMapper).decrementUserActiveRequestCount(USER_ID);
    }

    /** 核销 pending_settlement 来源账单：递减仅针对 reserved 来源，此处不再递减。 */
    @Test
    void writeOffShouldNotDecrementAgainFromPendingSettlement()
    {
        when(settlementTaskMapper.selectByRequestIdForUpdate(REQUEST_ID)).thenReturn(task());
        when(billingRecordMapper.selectByIdForUpdate(BILLING_ID))
                .thenReturn(record(CpaBillingConstants.STATUS_PENDING_SETTLEMENT));
        when(sysUserMapper.releaseUserFrozenBalance(USER_ID, new BigDecimal("0.01"))).thenReturn(1);
        when(billingRecordMapper.updateWrittenOff(eq(BILLING_ID), any(String.class), any(String.class))).thenReturn(1);
        when(settlementTaskMapper.markTaskResolved(any(Long.class))).thenReturn(1);

        service.writeOffFailedTask(REQUEST_ID, "人工核销", "admin");

        verify(sysUserMapper, never()).decrementUserActiveRequestCount(USER_ID);
    }

    private CpaBillingSettlementTask task()
    {
        CpaBillingSettlementTask task = new CpaBillingSettlementTask();
        task.setTaskId(TASK_ID);
        task.setBillingId(BILLING_ID);
        task.setRequestId(REQUEST_ID);
        task.setUserId(USER_ID);
        task.setKeyId(KEY_ID);
        task.setStatus(CpaBillingConstants.TASK_FAILED);
        return task;
    }

    private CpaBillingRecord record(String status)
    {
        CpaBillingRecord record = new CpaBillingRecord();
        record.setBillingId(BILLING_ID);
        record.setRequestId(REQUEST_ID);
        record.setUserId(USER_ID);
        record.setKeyId(KEY_ID);
        record.setStatus(status);
        record.setWalletReservedAmount(new BigDecimal("0.01"));
        return record;
    }

    private SysUser settleUser()
    {
        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        user.setStatus("0");
        user.setBalance(new BigDecimal("10.00"));
        user.setFrozenBalance(new BigDecimal("0.01"));
        return user;
    }

    private void setField(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
