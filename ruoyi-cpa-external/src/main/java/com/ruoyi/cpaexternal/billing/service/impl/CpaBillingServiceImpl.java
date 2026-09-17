package com.ruoyi.cpaexternal.billing.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingConstants;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingReserveRequest;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingReserveResult;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingConcurrencyDriftVO;
import com.ruoyi.cpaexternal.billing.mapper.CpaBillingRecordMapper;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingService;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysConfigService;

/**
 * CLIProxyAPI 额度预占与释放服务实现。
 *
 * <p>由 RelayContext 内部调用迁移为公开账务能力：CLIProxyAPI 在处理 AI 请求前
 * 通过公开 HTTP API 发起预占，钱包与订阅以原子条件更新冻结。</p>
 */
@Service
public class CpaBillingServiceImpl implements ICpaBillingService
{
    private static final Logger log = LoggerFactory.getLogger(CpaBillingServiceImpl.class);

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final String KEY_STATUS_NORMAL = "0";

    private static final String USER_STATUS_NORMAL = "0";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AiUserSubscriptionMapper userSubscriptionMapper;

    @Autowired
    private CpaBillingRecordMapper billingRecordMapper;

    @Autowired
    private ICpaApiKeyService apiKeyService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private CpaBillingProperties billingProperties;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private CpaBillingTransactionRetryExecutor transactionRetryExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CpaBillingReserveResult reserve(CpaBillingReserveRequest request)
    {
        if (request == null || StrUtil.isBlank(request.getRequestId()))
        {
            throw new ServiceException("request_id不能为空");
        }
        if (StrUtil.isBlank(request.getApiKey()))
        {
            throw new ServiceException("api_key不能为空");
        }

        CpaApiKey apiKey = resolveApiKey(request.getApiKey());
        SysUser user = resolveUser(apiKey.getUserId());
        BigDecimal reserveAmount = resolveReserveAmount();

        CpaBillingRecord processingRecord = new CpaBillingRecord();
        processingRecord.setRequestId(request.getRequestId());
        processingRecord.setUserId(apiKey.getUserId());
        processingRecord.setKeyId(apiKey.getKeyId());
        processingRecord.setAmount(ZERO);
        processingRecord.setReservedAmount(ZERO);
        processingRecord.setWalletReservedAmount(ZERO);
        processingRecord.setSubscriptionReservedAmount(ZERO);
        processingRecord.setKeyReservedAmount(ZERO);
        processingRecord.setWalletChargedAmount(ZERO);
        processingRecord.setSubscriptionChargedAmount(ZERO);
        processingRecord.setKeyChargedAmount(ZERO);
        processingRecord.setUncoveredAmount(ZERO);
        processingRecord.setStatus(CpaBillingConstants.STATUS_PROCESSING);
        if (billingRecordMapper.insertIgnore(processingRecord) != 1)
        {
            CpaBillingRecord existing = billingRecordMapper.selectByRequestId(request.getRequestId());
            if (existing != null && isReservationAvailable(existing.getStatus()))
            {
                return buildReserveResult(true, existing, null, user, false);
            }
            throw new ServiceException("request_id已存在，无法重复预占");
        }

        // 并发占位：条件UPDATE一次完成"检查+自增"，影响0行表示已达上限或上限为0被禁用。
        // 必须位于冻结之前：无条件锁定用户行，串行化同一用户的并发预占，且UPDATE为当前读，
        // 规避可重复读快照下漏看刚提交同类账单的幻读；拒绝时整个预占事务回滚。
        if (sysUserMapper.incrementUserActiveRequestCount(apiKey.getUserId()) != 1)
        {
            throw newConcurrencyLimitedException(user);
        }

        String preference = normalizePreference(user.getBillingPreference());
        AiUserSubscription subscription = requiresSubscription(preference)
                ? userSubscriptionMapper.selectActiveSubscriptionSnapshotForBilling(apiKey.getUserId()) : null;

        ReserveAllocation allocation = reserveFromSingleSource(
                apiKey.getUserId(), subscription, preference, reserveAmount);

        CpaBillingRecord reservedRecord = new CpaBillingRecord();
        reservedRecord.setRequestId(request.getRequestId());
        reservedRecord.setSubscriptionId(subscription == null ? null : subscription.getSubscriptionId());
        reservedRecord.setSubscriptionPlanTitle(subscription == null ? null : subscription.getPlanTitle());
        reservedRecord.setBillingPreference(preference);
        reservedRecord.setReservedAmount(reserveAmount);
        reservedRecord.setWalletReservedAmount(allocation.getWalletAmount());
        reservedRecord.setSubscriptionReservedAmount(allocation.getSubscriptionAmount());
        reservedRecord.setKeyReservedAmount(ZERO);
        reservedRecord.setReserveExpireTime(resolveReserveExpireTime());
        if (billingRecordMapper.updateReserved(reservedRecord) != 1)
        {
            throw new ServiceException("更新额度预占记录失败");
        }
        reservedRecord.setUserId(apiKey.getUserId());
        reservedRecord.setKeyId(apiKey.getKeyId());
        reservedRecord.setBillingId(processingRecord.getBillingId());
        return buildReserveResult(true, reservedRecord, null, user, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(String requestId, String reason)
    {
        if (StrUtil.isBlank(requestId))
        {
            return;
        }
        CpaBillingRecord record = billingRecordMapper.selectByRequestIdForUpdate(requestId);
        if (record == null || !CpaBillingConstants.STATUS_RESERVED.equals(record.getStatus()))
        {
            return;
        }
        releaseLockedRecord(record, CpaBillingConstants.STATUS_RELEASED, reason);
    }

    @Override
    public CpaBillingRecord selectByRequestId(String requestId)
    {
        return billingRecordMapper.selectByRequestId(requestId);
    }

    @Override
    public int releaseExpiredReservations(int limit)
    {
        int actualLimit = limit <= 0 ? 100 : limit;
        List<Long> billingIds = billingRecordMapper.selectExpiredReservedIds(actualLimit);
        if (CollUtil.isEmpty(billingIds))
        {
            return 0;
        }
        int released = 0;
        for (Long billingId : billingIds)
        {
            try
            {
                Boolean success = transactionRetryExecutor.execute("释放过期预占", () ->
                    executeNewTransaction(() -> releaseOneExpiredReservation(billingId)));
                if (Boolean.TRUE.equals(success))
                {
                    released++;
                }
            }
            catch (Exception e)
            {
                // 单行失败（死锁重试耗尽等）跳过，下一轮清理仍会选到该记录
                log.warn("释放过期CLIProxyAPI计费预占失败，将在下轮重试: billingId={}", billingId, e);
            }
        }
        return released;
    }

    @Override
    public int logConcurrencyDriftIfAny()
    {
        List<CpaBillingConcurrencyDriftVO> drifts = billingRecordMapper.selectConcurrencyDrift();
        for (CpaBillingConcurrencyDriftVO drift : drifts)
        {
            log.error("AI并发计数与在途账单不一致（计数列={}, reserved账单数={}, userId={}），"
                    + "请检查账单状态出口是否遗漏并发递减",
                drift.getCounterCount(), drift.getRecordCount(), drift.getUserId());
        }
        return drifts.size();
    }

    /**
     * 在独立短事务内释放单条过期预占。
     * 批查到执行期间记录可能已被结算提交改为终态，必须重新点查校验状态，
     * 否则会把已结算的冻结额度错误退还。
     */
    private Boolean releaseOneExpiredReservation(Long billingId)
    {
        CpaBillingRecord record = billingRecordMapper.selectByIdForUpdate(billingId);
        if (record == null)
        {
            return false;
        }
        if (!CpaBillingConstants.STATUS_RESERVED.equals(record.getStatus()))
        {
            return false;
        }
        releaseLockedRecord(record, CpaBillingConstants.STATUS_EXPIRED, "额度预占超时自动释放");
        return true;
    }

    private void releaseLockedRecord(CpaBillingRecord record, String status, String reason)
    {
        BigDecimal walletReserved = nvl(record.getWalletReservedAmount());
        if (walletReserved.compareTo(ZERO) > 0
                && sysUserMapper.releaseUserFrozenBalance(record.getUserId(), walletReserved) != 1)
        {
            throw new ServiceException("释放钱包冻结额度失败");
        }

        BigDecimal subscriptionReserved = nvl(record.getSubscriptionReservedAmount());
        if (subscriptionReserved.compareTo(ZERO) > 0)
        {
            if (record.getSubscriptionId() == null
                    || userSubscriptionMapper.releaseAiUserSubscriptionFrozenBalance(
                    record.getSubscriptionId(), subscriptionReserved) != 1)
            {
                throw new ServiceException("释放订阅冻结额度失败");
            }
        }
        if (billingRecordMapper.updateTerminal(record.getBillingId(), status,
                StrUtil.nullToEmpty(reason)) != 1)
        {
            throw new ServiceException("更新预占释放状态失败");
        }
        // reserved→released/expired 出口：释放并发占位（与状态迁移同事务，CAS保证恰好一次）
        sysUserMapper.decrementUserActiveRequestCount(record.getUserId());
    }

    /** 解析并校验 API Key：存在且启用。 */
    private CpaApiKey resolveApiKey(String plainApiKey)
    {
        CpaApiKey apiKey = apiKeyService.selectByPlainKey(plainApiKey);
        if (apiKey == null)
        {
            throw new ServiceException("API Key无效");
        }
        if (!KEY_STATUS_NORMAL.equals(apiKey.getStatus()))
        {
            throw new ServiceException("API Key已停用");
        }
        return apiKey;
    }

    private SysUser resolveUser(Long userId)
    {
        SysUser user = sysUserMapper.selectUserBillingSnapshot(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }
        if (!USER_STATUS_NORMAL.equals(user.getStatus()))
        {
            throw new ServiceException("用户已停用");
        }
        return user;
    }

    /** 读取固定预占金额配置，缺失或非法视为服务端配置错误。 */
    private BigDecimal resolveReserveAmount()
    {
        String value = configService.selectConfigByKey(CpaBillingConstants.CONFIG_RESERVE_AMOUNT);
        if (StrUtil.isBlank(value))
        {
            throw new ServiceException("预占金额配置缺失");
        }
        try
        {
            BigDecimal amount = new BigDecimal(value.trim());
            if (amount.compareTo(ZERO) <= 0)
            {
                throw new ServiceException("预占金额配置必须大于0");
            }
            return amount;
        }
        catch (NumberFormatException e)
        {
            throw new ServiceException("预占金额配置无效");
        }
    }

    /** 预占阶段不允许跨资金源拆分，按偏好顺序在单一资金源全额冻结。 */
    private ReserveAllocation reserveFromSingleSource(Long userId, AiUserSubscription subscription,
                                                      String preference, BigDecimal reserveAmount)
    {
        ReserveAllocation result = new ReserveAllocation();
        if (AiSubscriptionConstants.PREFERENCE_WALLET_ONLY.equals(preference))
        {
            if (tryFreezeWallet(userId, reserveAmount))
            {
                result.setWalletAmount(reserveAmount);
                return result;
            }
            throw insufficientSingleSource(reserveAmount);
        }
        if (AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_ONLY.equals(preference))
        {
            if (tryFreezeSubscription(subscription, reserveAmount))
            {
                result.setSubscriptionAmount(reserveAmount);
                return result;
            }
            throw insufficientSingleSource(reserveAmount);
        }
        if (AiSubscriptionConstants.PREFERENCE_WALLET_FIRST.equals(preference))
        {
            if (tryFreezeWallet(userId, reserveAmount))
            {
                result.setWalletAmount(reserveAmount);
                return result;
            }
            if (tryFreezeSubscription(subscription, reserveAmount))
            {
                result.setSubscriptionAmount(reserveAmount);
                return result;
            }
            throw insufficientSingleSource(reserveAmount);
        }
        if (tryFreezeSubscription(subscription, reserveAmount))
        {
            result.setSubscriptionAmount(reserveAmount);
            return result;
        }
        if (tryFreezeWallet(userId, reserveAmount))
        {
            result.setWalletAmount(reserveAmount);
            return result;
        }
        throw insufficientSingleSource(reserveAmount);
    }

    private boolean tryFreezeWallet(Long userId, BigDecimal reserveAmount)
    {
        return sysUserMapper.freezeUserBalance(userId, reserveAmount) == 1;
    }

    private boolean tryFreezeSubscription(AiUserSubscription subscription, BigDecimal reserveAmount)
    {
        return subscription != null && userSubscriptionMapper.freezeAiUserSubscriptionBalance(
                subscription.getSubscriptionId(), reserveAmount) == 1;
    }

    private ServiceException insufficientSingleSource(BigDecimal reserveAmount)
    {
        return new ServiceException("单一资金源可用额度不足，需完整预占 " + reserveAmount.toPlainString());
    }

    private Date resolveReserveExpireTime()
    {
        long configuredSeconds = Math.max(1L, billingProperties.getReservationTimeoutSeconds());
        return new Date(System.currentTimeMillis() + configuredSeconds * 1000L);
    }

    private CpaBillingReserveResult buildReserveResult(boolean allowed, CpaBillingRecord record, String reason,
                                                       SysUser user, boolean incrementApplied)
    {
        CpaBillingReserveResult result = new CpaBillingReserveResult();
        result.setAllowed(allowed);
        result.setRequestId(record.getRequestId());
        result.setBillingId(record.getBillingId());
        result.setUserId(record.getUserId());
        result.setKeyId(record.getKeyId());
        result.setSubscriptionId(record.getSubscriptionId());
        result.setBillingPreference(record.getBillingPreference());
        result.setReservedAmount(nvl(record.getReservedAmount()));
        result.setWalletReservedAmount(nvl(record.getWalletReservedAmount()));
        result.setSubscriptionReservedAmount(nvl(record.getSubscriptionReservedAmount()));
        result.setKeyReservedAmount(nvl(record.getKeyReservedAmount()));
        result.setConcurrencyLimit(user.getAiConcurrencyLimit());
        // 在途并发为参考值：快照计数，新占位路径加一，仅供诊断，权威判定以本结果 allowed 为准
        int snapshotActive = user.getActiveRequestCount() == null ? 0 : user.getActiveRequestCount();
        result.setActiveRequestCount(incrementApplied ? snapshotActive + 1 : snapshotActive);
        result.setReason(reason);
        return result;
    }

    /** 并发拒绝文案：上限为0视为禁用；数值取自事务开始的用户快照，仅作提示，判定以条件UPDATE为准。 */
    private ServiceException newConcurrencyLimitedException(SysUser user)
    {
        Integer limit = user.getAiConcurrencyLimit();
        if (limit != null && limit <= 0)
        {
            return new ServiceException("AI并发上限为0，用户已被禁用AI访问");
        }
        return new ServiceException("AI并发数已达上限" + (limit == null ? "" : "(" + limit + ")"));
    }

    /** 构造预占被拒的结果（不落账单，由公开 API 在捕获 ServiceException 时使用）。 */
    public CpaBillingReserveResult buildRejectedResult(String requestId, String reason)
    {
        CpaBillingReserveResult result = new CpaBillingReserveResult();
        result.setAllowed(false);
        result.setRequestId(requestId);
        result.setReason(reason);
        return result;
    }

    private boolean isReservationAvailable(String status)
    {
        return CpaBillingConstants.STATUS_RESERVED.equals(status)
                || CpaBillingConstants.STATUS_PENDING_SETTLEMENT.equals(status)
                || CpaBillingConstants.STATUS_SUCCESS.equals(status)
                || CpaBillingConstants.STATUS_PARTIAL.equals(status);
    }

    private boolean requiresSubscription(String preference)
    {
        return !AiSubscriptionConstants.PREFERENCE_WALLET_ONLY.equals(preference);
    }

    private String normalizePreference(String preference)
    {
        if (AiSubscriptionConstants.PREFERENCE_WALLET_FIRST.equals(preference)
                || AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_ONLY.equals(preference)
                || AiSubscriptionConstants.PREFERENCE_WALLET_ONLY.equals(preference))
        {
            return preference;
        }
        return AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_FIRST;
    }

    private <T> T executeNewTransaction(Supplier<T> action)
    {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> action.get());
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? ZERO : value;
    }

    private static class ReserveAllocation
    {
        private BigDecimal walletAmount = ZERO;

        private BigDecimal subscriptionAmount = ZERO;

        public BigDecimal getWalletAmount()
        {
            return walletAmount;
        }

        public void setWalletAmount(BigDecimal walletAmount)
        {
            this.walletAmount = walletAmount;
        }

        public BigDecimal getSubscriptionAmount()
        {
            return subscriptionAmount;
        }

        public void setSubscriptionAmount(BigDecimal subscriptionAmount)
        {
            this.subscriptionAmount = subscriptionAmount;
        }

        public BigDecimal getTotal()
        {
            return walletAmount.add(subscriptionAmount);
        }
    }
}
