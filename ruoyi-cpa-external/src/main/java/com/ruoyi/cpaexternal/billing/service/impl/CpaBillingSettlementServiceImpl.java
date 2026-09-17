package com.ruoyi.cpaexternal.billing.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingConstants;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettleCommand;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementFailedQuery;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementTask;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingSettlementFailedVO;
import com.ruoyi.cpaexternal.billing.mapper.CpaBillingRecordMapper;
import com.ruoyi.cpaexternal.billing.mapper.CpaBillingSettlementTaskMapper;
import com.ruoyi.cpaexternal.billing.service.CpaBillingMinimumChargeResolver;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingSettlementService;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;
import com.ruoyi.cpaexternal.log.mapper.CpaAiLogMapper;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.SysUserMapper;

/**
 * CLIProxyAPI 计费异步结算任务服务实现。
 *
 * <p>结算由 Redis usage 消息触发：usage 落库后提交延迟结算任务，
 * Worker 在延迟窗口后领取，领取时重读 ai_log.cost（期间凭据重试产生的
 * 后续 usage 会更新该值）作为最终结算金额，并在同一事务内完成
 * 结算意图持久化与钱包/订阅双源扣费，事务回滚即整体重置，由任务重试兜底。</p>
 */
@Service
public class CpaBillingSettlementServiceImpl implements ICpaBillingSettlementService
{
    private static final Logger log = LoggerFactory.getLogger(CpaBillingSettlementServiceImpl.class);

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AiUserSubscriptionMapper userSubscriptionMapper;

    @Autowired
    private CpaBillingRecordMapper billingRecordMapper;

    @Autowired
    private CpaBillingSettlementTaskMapper settlementTaskMapper;

    @Autowired
    private CpaAiLogMapper aiLogMapper;

    @Autowired
    private CpaBillingProperties billingProperties;

    @Autowired
    private CpaBillingMinimumChargeResolver minimumChargeResolver;

    @Autowired
    private CpaBillingTransactionRetryExecutor transactionRetryExecutor;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public void submitSettlement(CpaBillingSettleCommand command)
    {
        if (command == null || StrUtil.isBlank(command.getRequestId()))
        {
            return;
        }
        String requestId = command.getRequestId();
        try
        {
            transactionRetryExecutor.execute("提交CLIProxyAPI结算意图", () ->
                executeNewTransaction(() ->
                {
                    submitSettlementInTransaction(requestId);
                    return null;
                }));
        }
        catch (Exception e)
        {
            // 提交失败不影响日志落库；补偿任务会在延迟窗口后补建结算任务
            log.error("提交CLIProxyAPI结算意图失败，等待补偿任务补建: requestId={}", requestId, e);
        }
    }

    private void submitSettlementInTransaction(String requestId)
    {
        CpaBillingRecord billingRecord = billingRecordMapper.selectByRequestIdForUpdate(requestId);
        if (billingRecord == null)
        {
            // 无预占账单：未接入预占的请求只记日志，不参与计费
            return;
        }
        if (isTerminal(billingRecord.getStatus()))
        {
            return;
        }
        linkSettlementLog(requestId);
        Date availableTime = new Date(System.currentTimeMillis()
                + Math.max(0, billingProperties.getSettleDelaySeconds()) * 1000L);
        ensureSettlementTaskInTransaction(billingRecord.getBillingId(), availableTime);
    }

    /** 回填账单与结算日志的关联，供用户/管理员按日志查询账单。 */
    private void linkSettlementLog(String requestId)
    {
        CpaAiLog aiLog = aiLogMapper.selectByRequestId(requestId);
        if (aiLog != null && aiLog.getLogId() != null)
        {
            billingRecordMapper.updateLogId(requestId, aiLog.getLogId());
        }
    }

    @Override
    public int repairMissingSettlementTasks(int limit)
    {
        int actualLimit = Math.max(1, limit);
        int repaired = 0;
        repaired += repairBillingIds(billingRecordMapper.selectPendingWithoutTask(actualLimit),
            "补建CLIProxyAPI结算任务(pending_settlement)");

        // 延迟窗口的两倍时间之后仍无任务的 reserved 账单：提交崩溃或消息丢失，立即补建
        long guardSeconds = Math.max(60L, billingProperties.getSettleDelaySeconds() * 2L);
        Date beforeTime = new Date(System.currentTimeMillis() - guardSeconds * 1000L);
        repaired += repairBillingIds(billingRecordMapper.selectReservedWithLogWithoutTask(actualLimit, beforeTime),
            "补建CLIProxyAPI结算任务(reserved)");
        return repaired;
    }

    private int repairBillingIds(List<Long> billingIds, String operation)
    {
        if (billingIds == null || billingIds.isEmpty())
        {
            return 0;
        }
        int repaired = 0;
        Date now = new Date();
        for (Long billingId : billingIds)
        {
            try
            {
                Boolean created = transactionRetryExecutor.execute(operation, () ->
                    executeNewTransaction(() -> ensureSettlementTaskInTransaction(billingId, now)));
                if (Boolean.TRUE.equals(created))
                {
                    repaired++;
                }
            }
            catch (Exception e)
            {
                log.error("{}失败，将在下一轮重试: billingId={}", operation, billingId, e);
            }
        }
        return repaired;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public List<CpaBillingSettlementTask> claimPendingTasks(String claimToken, int limit, long claimTimeoutSeconds)
    {
        if (StrUtil.isBlank(claimToken))
        {
            throw new ServiceException("异步结算领取令牌不能为空");
        }
        int actualLimit = Math.max(1, limit);
        long actualTimeoutSeconds = Math.max(10L, claimTimeoutSeconds);
        settlementTaskMapper.requeueExpiredClaims(actualLimit);
        List<CpaBillingSettlementTask> tasks = settlementTaskMapper.selectPendingTasksForUpdate(actualLimit);
        if (tasks == null || tasks.isEmpty())
        {
            return Collections.emptyList();
        }
        List<Long> taskIds = new ArrayList<>(tasks.size());
        Date claimExpireTime = new Date(System.currentTimeMillis() + actualTimeoutSeconds * 1000L);
        for (CpaBillingSettlementTask task : tasks)
        {
            taskIds.add(task.getTaskId());
            task.setStatus(CpaBillingConstants.TASK_CLAIMED);
            task.setClaimToken(claimToken);
            task.setClaimExpireTime(claimExpireTime);
        }
        if (settlementTaskMapper.claimTasks(taskIds, claimToken, claimExpireTime) != tasks.size())
        {
            throw new ServiceException("领取异步结算任务失败");
        }
        return tasks;
    }

    @Override
    public void processClaimedTask(CpaBillingSettlementTask task, String claimToken)
    {
        validateClaimedTask(task, claimToken);
        transactionRetryExecutor.execute("执行CLIProxyAPI异步结算任务", () ->
            executeNewTransaction(() ->
            {
                processClaimedTaskInTransaction(task, claimToken);
                return null;
            }));
    }

    private void processClaimedTaskInTransaction(CpaBillingSettlementTask task, String claimToken)
    {
        CpaBillingRecord billingRecord = billingRecordMapper.selectByIdForUpdate(task.getBillingId());
        if (billingRecord == null)
        {
            throw new CpaBillingSettlementNonRetryableException("异步结算账单不存在");
        }
        validateTaskConsistency(task, billingRecord);
        if (CpaBillingConstants.STATUS_SUCCESS.equals(billingRecord.getStatus())
                || CpaBillingConstants.STATUS_PARTIAL.equals(billingRecord.getStatus())
                || CpaBillingConstants.STATUS_WRITTEN_OFF.equals(billingRecord.getStatus())
                || CpaBillingConstants.STATUS_RELEASED.equals(billingRecord.getStatus())
                || CpaBillingConstants.STATUS_EXPIRED.equals(billingRecord.getStatus())
                || CpaBillingConstants.STATUS_FAILED.equals(billingRecord.getStatus()))
        {
            markTaskDone(task, claimToken);
            return;
        }
        if (CpaBillingConstants.STATUS_RESERVED.equals(billingRecord.getStatus()))
        {
            BigDecimal finalAmount = resolveFinalAmount(billingRecord.getRequestId());
            billingRecord.setAmount(finalAmount);
            if (billingRecordMapper.updatePendingSettlement(billingRecord) != 1)
            {
                throw new ServiceException("持久化CLIProxyAPI结算意图失败");
            }
            billingRecord.setStatus(CpaBillingConstants.STATUS_PENDING_SETTLEMENT);
            // reserved→pending_settlement 出口：释放并发占位（与状态迁移同事务，CAS保证恰好一次）
            sysUserMapper.decrementUserActiveRequestCount(billingRecord.getUserId());
        }
        if (!CpaBillingConstants.STATUS_PENDING_SETTLEMENT.equals(billingRecord.getStatus()))
        {
            throw new CpaBillingSettlementNonRetryableException(
                "异步结算账单状态异常: " + billingRecord.getStatus());
        }

        BigDecimal actualCost = nvl(billingRecord.getAmount());
        SysUser user = sysUserMapper.selectUserByIdForUpdate(billingRecord.getUserId());
        if (user == null)
        {
            throw new CpaBillingSettlementNonRetryableException("异步结算用户不存在");
        }
        AiUserSubscription subscription = billingRecord.getSubscriptionId() == null ? null
                : userSubscriptionMapper.selectAiUserSubscriptionByIdForUpdate(billingRecord.getSubscriptionId());

        ChargeAllocation allocation = allocateSettlement(user, subscription, billingRecord, actualCost);
        BigDecimal walletReserved = nvl(billingRecord.getWalletReservedAmount());
        BigDecimal subscriptionReserved = nvl(billingRecord.getSubscriptionReservedAmount());
        int requestIncrement = actualCost.compareTo(ZERO) > 0 ? 1 : 0;
        if (sysUserMapper.settleUserBilling(billingRecord.getUserId(), walletReserved,
                allocation.getWalletAmount(), actualCost, requestIncrement) != 1)
        {
            throw new ServiceException("异步结算钱包冻结额度失败");
        }
        if (subscriptionReserved.compareTo(ZERO) > 0
                || allocation.getSubscriptionAmount().compareTo(ZERO) > 0)
        {
            if (subscription == null || userSubscriptionMapper.settleAiUserSubscriptionBilling(
                    subscription.getSubscriptionId(), subscriptionReserved, allocation.getSubscriptionAmount()) != 1)
            {
                throw new ServiceException("异步结算订阅冻结额度失败");
            }
        }

        BigDecimal uncoveredAmount = positive(actualCost.subtract(allocation.getTotal()));
        billingRecord.setBillingSource(resolveBillingSource(allocation));
        billingRecord.setWalletChargedAmount(allocation.getWalletAmount());
        billingRecord.setSubscriptionChargedAmount(allocation.getSubscriptionAmount());
        billingRecord.setKeyChargedAmount(ZERO);
        billingRecord.setUncoveredAmount(uncoveredAmount);
        billingRecord.setStatus(uncoveredAmount.compareTo(ZERO) > 0
                ? CpaBillingConstants.STATUS_PARTIAL
                : CpaBillingConstants.STATUS_SUCCESS);
        billingRecord.setErrorMessage(buildPartialMessage(uncoveredAmount));
        if (billingRecordMapper.updateSettlementFromPending(billingRecord) != 1)
        {
            throw new ServiceException("更新CLIProxyAPI异步结算账单结果失败");
        }
        markTaskDone(task, claimToken);
    }

    /**
     * 领取任务时读取最终结算金额：ai_log.cost 为权威值（延迟窗口内后续 usage
     * 可能已更新），并应用最低计费；结算日志缺失时按零费结算（对齐旧系统语义）。
     */
    private BigDecimal resolveFinalAmount(String requestId)
    {
        CpaAiLog settlementLog = aiLogMapper.selectByRequestId(requestId);
        if (settlementLog == null)
        {
            log.warn("CLIProxyAPI结算日志缺失，按零费结算: requestId={}", requestId);
            return ZERO;
        }
        return minimumChargeResolver.applyMinimumAmount(settlementLog.getCost());
    }

    private void markTaskDone(CpaBillingSettlementTask task, String claimToken)
    {
        if (settlementTaskMapper.markTaskDone(task.getTaskId(), claimToken) != 1)
        {
            throw new ServiceException("异步结算任务领取凭证已失效");
        }
    }

    /**
     * 确保账单存在结算任务，已存在或已终态时幂等返回 false。
     * 任务的 actual_cost 仅为提交时快照，最终金额以账单 amount 为权威。
     * 补建任务时同步回填账单与日志的关联，避免崩溃恢复后的账单在日志详情页不可见。
     */
    private Boolean ensureSettlementTaskInTransaction(Long billingId, Date availableTime)
    {
        if (billingId == null)
        {
            return false;
        }
        CpaBillingRecord billingRecord = billingRecordMapper.selectByIdForUpdate(billingId);
        if (billingRecord == null)
        {
            return false;
        }
        if (isTerminal(billingRecord.getStatus()))
        {
            return false;
        }
        if (!CpaBillingConstants.STATUS_RESERVED.equals(billingRecord.getStatus())
                && !CpaBillingConstants.STATUS_PENDING_SETTLEMENT.equals(billingRecord.getStatus()))
        {
            return false;
        }
        CpaBillingSettlementTask existingTask = settlementTaskMapper
                .selectByBillingIdForUpdate(billingRecord.getBillingId());
        if (existingTask == null)
        {
            existingTask = settlementTaskMapper.selectByRequestIdForUpdate(billingRecord.getRequestId());
        }
        if (existingTask != null)
        {
            return false;
        }
        linkSettlementLog(billingRecord.getRequestId());
        return insertTask(billingRecord, nvl(billingRecord.getAmount()), availableTime);
    }

    private boolean insertTask(CpaBillingRecord billingRecord, BigDecimal actualCost, Date availableTime)
    {
        if (billingRecord.getBillingId() == null)
        {
            throw new ServiceException("账单ID不能为空");
        }
        CpaBillingSettlementTask task = new CpaBillingSettlementTask();
        task.setBillingId(billingRecord.getBillingId());
        task.setRequestId(billingRecord.getRequestId());
        task.setUserId(billingRecord.getUserId());
        task.setKeyId(billingRecord.getKeyId());
        task.setActualCost(actualCost);
        task.setAvailableTime(availableTime);
        try
        {
            if (settlementTaskMapper.insertTask(task) != 1)
            {
                throw new ServiceException("写入异步结算任务失败");
            }
            return true;
        }
        catch (DuplicateKeyException e)
        {
            CpaBillingSettlementTask existingTask = settlementTaskMapper
                    .selectByBillingIdForUpdate(billingRecord.getBillingId());
            if (existingTask == null)
            {
                existingTask = settlementTaskMapper.selectByRequestIdForUpdate(billingRecord.getRequestId());
            }
            if (existingTask == null)
            {
                throw e;
            }
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markTaskRetry(CpaBillingSettlementTask task, String claimToken, String errorMessage)
    {
        validateClaimedTask(task, claimToken);
        int nextRetryCount = nvl(task.getRetryCount()) + 1;
        Date availableTime = new Date(System.currentTimeMillis() + calculateRetryDelayMs(nextRetryCount));
        String safeError = StrUtil.subPre(StrUtil.blankToDefault(errorMessage, "异步结算失败"), 500);
        return settlementTaskMapper.markTaskRetry(task.getTaskId(), claimToken,
            billingProperties.getSettlementMaxRetries(), availableTime, safeError);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markTaskFailed(CpaBillingSettlementTask task, String claimToken, String errorMessage)
    {
        validateClaimedTask(task, claimToken);
        String safeError = StrUtil.subPre(StrUtil.blankToDefault(errorMessage, "异步结算数据异常"), 500);
        return settlementTaskMapper.markTaskFailed(task.getTaskId(), claimToken, safeError);
    }

    @Override
    public List<CpaBillingSettlementFailedVO> selectFailedTaskList(CpaBillingSettlementFailedQuery query)
    {
        CpaBillingSettlementFailedQuery actualQuery = query == null
            ? new CpaBillingSettlementFailedQuery() : query;
        return settlementTaskMapper.selectFailedTaskList(actualQuery);
    }

    @Override
    public int countFailedTasks()
    {
        return settlementTaskMapper.countFailedTasks();
    }

    @Override
    public int deleteDoneTasksBefore(Date beforeTime, int limit)
    {
        if (beforeTime == null)
        {
            return 0;
        }
        return settlementTaskMapper.deleteDoneTasksBefore(beforeTime, Math.max(1, limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int retryFailedTask(String requestId)
    {
        if (StrUtil.isBlank(requestId))
        {
            return 0;
        }
        CpaBillingSettlementTask task = settlementTaskMapper.selectByRequestIdForUpdate(requestId);
        if (task == null || !CpaBillingConstants.TASK_FAILED.equals(task.getStatus()))
        {
            return 0;
        }
        CpaBillingRecord billingRecord = billingRecordMapper.selectByIdForUpdate(task.getBillingId());
        validateFailedTaskBilling(task, billingRecord);
        return settlementTaskMapper.retryFailedTask(task.getTaskId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int writeOffFailedTask(String requestId, String reason, String operator)
    {
        if (StrUtil.isBlank(requestId))
        {
            return 0;
        }
        String normalizedReason = StrUtil.trim(reason);
        if (StrUtil.isBlank(normalizedReason))
        {
            throw new ServiceException("核销原因不能为空");
        }
        if (normalizedReason.length() > 500)
        {
            throw new ServiceException("核销原因不能超过500个字符");
        }
        if (StrUtil.isBlank(operator))
        {
            throw new ServiceException("核销操作人不能为空");
        }

        CpaBillingSettlementTask task = settlementTaskMapper.selectByRequestIdForUpdate(requestId);
        if (task == null || !CpaBillingConstants.TASK_FAILED.equals(task.getStatus()))
        {
            return 0;
        }
        CpaBillingRecord billingRecord = billingRecordMapper.selectByIdForUpdate(task.getBillingId());
        validateFailedTaskBilling(task, billingRecord);
        releaseFailedTaskReservations(billingRecord);
        if (billingRecordMapper.updateWrittenOff(
            billingRecord.getBillingId(), normalizedReason, operator) != 1)
        {
            throw new ServiceException("更新人工核销账单失败");
        }
        // 核销出口：仅 reserved 来源账单占用并发额度需释放（pending_settlement 来源在结算领取时已释放）
        if (CpaBillingConstants.STATUS_RESERVED.equals(billingRecord.getStatus()))
        {
            sysUserMapper.decrementUserActiveRequestCount(billingRecord.getUserId());
        }
        if (settlementTaskMapper.markTaskResolved(task.getTaskId()) != 1)
        {
            throw new ServiceException("更新结算任务处置状态失败");
        }
        return 1;
    }

    private void releaseFailedTaskReservations(CpaBillingRecord billingRecord)
    {
        BigDecimal walletReserved = nvl(billingRecord.getWalletReservedAmount());
        if (walletReserved.compareTo(ZERO) > 0
                && sysUserMapper.releaseUserFrozenBalance(billingRecord.getUserId(), walletReserved) != 1)
        {
            throw new ServiceException("释放钱包预占额度失败");
        }

        BigDecimal subscriptionReserved = nvl(billingRecord.getSubscriptionReservedAmount());
        if (subscriptionReserved.compareTo(ZERO) > 0)
        {
            if (billingRecord.getSubscriptionId() == null
                    || userSubscriptionMapper.releaseAiUserSubscriptionFrozenBalance(
                    billingRecord.getSubscriptionId(), subscriptionReserved) != 1)
            {
                throw new ServiceException("释放订阅预占额度失败");
            }
        }
    }

    private void validateTaskConsistency(CpaBillingSettlementTask task, CpaBillingRecord billingRecord)
    {
        if (!Objects.equals(task.getBillingId(), billingRecord.getBillingId())
                || !Objects.equals(task.getRequestId(), billingRecord.getRequestId())
                || !Objects.equals(task.getUserId(), billingRecord.getUserId())
                || !Objects.equals(task.getKeyId(), billingRecord.getKeyId()))
        {
            throw new CpaBillingSettlementNonRetryableException("异步结算任务与账单数据不一致");
        }
    }

    /** 失败任务重试/核销前校验：账单仍处于可处置状态且未产生任何实扣，防止重复扣款。 */
    private void validateFailedTaskBilling(CpaBillingSettlementTask task, CpaBillingRecord billingRecord)
    {
        if (billingRecord == null)
        {
            throw new ServiceException("结算失败任务对应账单不存在");
        }
        validateTaskConsistency(task, billingRecord);
        if (!CpaBillingConstants.STATUS_RESERVED.equals(billingRecord.getStatus())
                && !CpaBillingConstants.STATUS_PENDING_SETTLEMENT.equals(billingRecord.getStatus()))
        {
            throw new ServiceException("结算失败任务对应账单状态不可处置: " + billingRecord.getStatus());
        }
        if (nvl(billingRecord.getWalletChargedAmount()).compareTo(ZERO) != 0
                || nvl(billingRecord.getSubscriptionChargedAmount()).compareTo(ZERO) != 0
                || nvl(billingRecord.getKeyChargedAmount()).compareTo(ZERO) != 0)
        {
            throw new ServiceException("结算失败账单已存在实扣金额，禁止重试或核销");
        }
    }

    private void validateClaimedTask(CpaBillingSettlementTask task, String claimToken)
    {
        if (task == null || task.getTaskId() == null || StrUtil.isBlank(claimToken))
        {
            throw new ServiceException("异步结算任务或领取凭证不能为空");
        }
    }

    /** 结算阶段容量 = 已冻结额 + 当前可用额，允许实际费用超过预占金额。 */
    private ChargeAllocation allocateSettlement(SysUser user, AiUserSubscription subscription,
                                                CpaBillingRecord record, BigDecimal actualCost)
    {
        BigDecimal walletCapacity = nvl(record.getWalletReservedAmount()).add(availableWallet(user));
        BigDecimal subscriptionCapacity = nvl(record.getSubscriptionReservedAmount())
                .add(availableSubscription(subscription));
        ReserveAllocation allocation = allocate(record.getBillingPreference(), actualCost,
                walletCapacity, subscriptionCapacity);
        ChargeAllocation result = new ChargeAllocation();
        result.setWalletAmount(allocation.getWalletAmount());
        result.setSubscriptionAmount(allocation.getSubscriptionAmount());
        return result;
    }

    private ReserveAllocation allocate(String preference, BigDecimal target,
                                       BigDecimal walletCapacity, BigDecimal subscriptionCapacity)
    {
        ReserveAllocation result = new ReserveAllocation();
        BigDecimal remaining = target;
        if (AiSubscriptionConstants.PREFERENCE_WALLET_ONLY.equals(preference))
        {
            result.setWalletAmount(min(remaining, walletCapacity));
            return result;
        }
        if (AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_ONLY.equals(preference))
        {
            result.setSubscriptionAmount(min(remaining, subscriptionCapacity));
            return result;
        }
        if (AiSubscriptionConstants.PREFERENCE_WALLET_FIRST.equals(preference))
        {
            BigDecimal wallet = min(remaining, walletCapacity);
            result.setWalletAmount(wallet);
            remaining = remaining.subtract(wallet);
            result.setSubscriptionAmount(min(remaining, subscriptionCapacity));
            return result;
        }
        BigDecimal subscription = min(remaining, subscriptionCapacity);
        result.setSubscriptionAmount(subscription);
        remaining = remaining.subtract(subscription);
        result.setWalletAmount(min(remaining, walletCapacity));
        return result;
    }

    private boolean isTerminal(String status)
    {
        return CpaBillingConstants.STATUS_SUCCESS.equals(status)
                || CpaBillingConstants.STATUS_PARTIAL.equals(status)
                || CpaBillingConstants.STATUS_RELEASED.equals(status)
                || CpaBillingConstants.STATUS_EXPIRED.equals(status)
                || CpaBillingConstants.STATUS_FAILED.equals(status)
                || CpaBillingConstants.STATUS_WRITTEN_OFF.equals(status);
    }

    private long calculateRetryDelayMs(int retryCount)
    {
        long delayMs = billingProperties.getSettlementRetryBaseDelayMs();
        long maxDelayMs = billingProperties.getSettlementRetryMaxDelayMs();
        for (int i = 1; i < retryCount && delayMs < maxDelayMs; i++)
        {
            delayMs = Math.min(maxDelayMs, delayMs * 2L);
        }
        return delayMs;
    }

    private BigDecimal availableWallet(SysUser user)
    {
        return positive(nvl(user.getBalance()).subtract(nvl(user.getFrozenBalance())));
    }

    private BigDecimal availableSubscription(AiUserSubscription subscription)
    {
        if (subscription == null)
        {
            return ZERO;
        }
        Date now = new Date();
        if (!AiSubscriptionConstants.SUB_STATUS_ACTIVE.equals(subscription.getStatus())
                || subscription.getEndTime() == null || !subscription.getEndTime().after(now)
                || (subscription.getNextResetTime() != null && !subscription.getNextResetTime().after(now)))
        {
            return ZERO;
        }
        BigDecimal total = nvl(subscription.getAmountTotal());
        if (total.compareTo(ZERO) == 0)
        {
            return new BigDecimal("999999999999999999");
        }
        return positive(total.subtract(nvl(subscription.getAmountUsed()))
                .subtract(nvl(subscription.getFrozenBalance())));
    }

    private String resolveBillingSource(ChargeAllocation allocation)
    {
        boolean wallet = allocation.getWalletAmount().compareTo(ZERO) > 0;
        boolean subscription = allocation.getSubscriptionAmount().compareTo(ZERO) > 0;
        if (wallet && subscription)
        {
            return CpaBillingConstants.SOURCE_MIXED;
        }
        if (wallet)
        {
            return CpaBillingConstants.SOURCE_WALLET;
        }
        if (subscription)
        {
            return CpaBillingConstants.SOURCE_SUBSCRIPTION;
        }
        return CpaBillingConstants.SOURCE_NO_CHARGE;
    }

    private String buildPartialMessage(BigDecimal uncoveredAmount)
    {
        return uncoveredAmount.compareTo(ZERO) > 0
                ? "用户额度未覆盖金额: " + uncoveredAmount.toPlainString()
                : null;
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? ZERO : value;
    }

    private int nvl(Integer value)
    {
        return value == null ? 0 : value;
    }

    private BigDecimal positive(BigDecimal value)
    {
        return value.compareTo(ZERO) > 0 ? value : ZERO;
    }

    private BigDecimal min(BigDecimal left, BigDecimal right)
    {
        BigDecimal safeLeft = positive(nvl(left));
        BigDecimal safeRight = positive(nvl(right));
        return safeLeft.compareTo(safeRight) <= 0 ? safeLeft : safeRight;
    }

    private <T> T executeNewTransaction(Supplier<T> action)
    {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> action.get());
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

    private static class ChargeAllocation extends ReserveAllocation
    {
    }
}
