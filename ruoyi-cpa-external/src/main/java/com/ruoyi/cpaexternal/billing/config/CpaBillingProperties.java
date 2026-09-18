package com.ruoyi.cpaexternal.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** CLIProxyAPI 计费配置。 */
@Component
@ConfigurationProperties(prefix = "cpa.billing")
public class CpaBillingProperties
{
    /** usage 落库后到结算任务可领取的延迟窗口（秒），用于聚合凭据重试产生的多条 usage。 */
    private int settleDelaySeconds = 5;

    /** 公开计费 API 的 Bearer Token，为空时公开接口拒绝所有请求。 */
    private String apiToken = "";

    /** 是否启动异步结算 Worker。 */
    private boolean settlementWorkerEnabled = true;

    /** Worker 轮询间隔（毫秒）。 */
    private long settlementPollIntervalMs = 5000L;

    /** Worker 每轮最多领取记录数。 */
    private int settlementClaimBatchSize = 100;

    /** Worker 并行处理用户数。 */
    private int settlementWorkerThreads = 4;

    /** 领取租约超时时间（秒）。 */
    private long settlementClaimTimeoutSeconds = 60L;

    /** 最大自动重试次数。 */
    private int settlementMaxRetries = 10;

    /** 重试指数退避基数（毫秒）。 */
    private long settlementRetryBaseDelayMs = 1000L;

    /** 重试最大退避时间（毫秒）。 */
    private long settlementRetryMaxDelayMs = 60000L;

    /** Worker 关闭时等待时间（毫秒）。 */
    private long settlementShutdownWaitMs = 30000L;

    /** 已完成结算任务保留天数。 */
    private int settlementTaskRetentionDays = 30;

    /** 已完成结算任务单批清理数量。 */
    private int settlementTaskCleanupBatchSize = 2000;

    /** 已完成结算任务单次最多清理批数。 */
    private int settlementTaskCleanupMaxBatches = 50;

    /** 数据库死锁即时重试次数。 */
    private int transactionDeadlockMaxAttempts = 3;

    /** 数据库死锁即时重试基础延迟（毫秒）。 */
    private long transactionDeadlockBaseDelayMs = 25L;

    /** 数据库死锁即时重试最大延迟（毫秒）。 */
    private long transactionDeadlockMaxDelayMs = 200L;

    public int getSettleDelaySeconds()
    {
        return settleDelaySeconds;
    }

    public void setSettleDelaySeconds(int settleDelaySeconds)
    {
        this.settleDelaySeconds = settleDelaySeconds;
    }

    public String getApiToken()
    {
        return apiToken;
    }

    public void setApiToken(String apiToken)
    {
        this.apiToken = apiToken;
    }

    public boolean isSettlementWorkerEnabled()
    {
        return settlementWorkerEnabled;
    }

    public void setSettlementWorkerEnabled(boolean settlementWorkerEnabled)
    {
        this.settlementWorkerEnabled = settlementWorkerEnabled;
    }

    public long getSettlementPollIntervalMs()
    {
        return settlementPollIntervalMs;
    }

    public void setSettlementPollIntervalMs(long settlementPollIntervalMs)
    {
        this.settlementPollIntervalMs = settlementPollIntervalMs;
    }

    public int getSettlementClaimBatchSize()
    {
        return settlementClaimBatchSize;
    }

    public void setSettlementClaimBatchSize(int settlementClaimBatchSize)
    {
        this.settlementClaimBatchSize = settlementClaimBatchSize;
    }

    public int getSettlementWorkerThreads()
    {
        return settlementWorkerThreads;
    }

    public void setSettlementWorkerThreads(int settlementWorkerThreads)
    {
        this.settlementWorkerThreads = settlementWorkerThreads;
    }

    public long getSettlementClaimTimeoutSeconds()
    {
        return settlementClaimTimeoutSeconds;
    }

    public void setSettlementClaimTimeoutSeconds(long settlementClaimTimeoutSeconds)
    {
        this.settlementClaimTimeoutSeconds = settlementClaimTimeoutSeconds;
    }

    public int getSettlementMaxRetries()
    {
        return settlementMaxRetries;
    }

    public void setSettlementMaxRetries(int settlementMaxRetries)
    {
        this.settlementMaxRetries = settlementMaxRetries;
    }

    public long getSettlementRetryBaseDelayMs()
    {
        return settlementRetryBaseDelayMs;
    }

    public void setSettlementRetryBaseDelayMs(long settlementRetryBaseDelayMs)
    {
        this.settlementRetryBaseDelayMs = settlementRetryBaseDelayMs;
    }

    public long getSettlementRetryMaxDelayMs()
    {
        return settlementRetryMaxDelayMs;
    }

    public void setSettlementRetryMaxDelayMs(long settlementRetryMaxDelayMs)
    {
        this.settlementRetryMaxDelayMs = settlementRetryMaxDelayMs;
    }

    public long getSettlementShutdownWaitMs()
    {
        return settlementShutdownWaitMs;
    }

    public void setSettlementShutdownWaitMs(long settlementShutdownWaitMs)
    {
        this.settlementShutdownWaitMs = settlementShutdownWaitMs;
    }

    public int getSettlementTaskRetentionDays()
    {
        return settlementTaskRetentionDays;
    }

    public void setSettlementTaskRetentionDays(int settlementTaskRetentionDays)
    {
        this.settlementTaskRetentionDays = settlementTaskRetentionDays;
    }

    public int getSettlementTaskCleanupBatchSize()
    {
        return settlementTaskCleanupBatchSize;
    }

    public void setSettlementTaskCleanupBatchSize(int settlementTaskCleanupBatchSize)
    {
        this.settlementTaskCleanupBatchSize = settlementTaskCleanupBatchSize;
    }

    public int getSettlementTaskCleanupMaxBatches()
    {
        return settlementTaskCleanupMaxBatches;
    }

    public void setSettlementTaskCleanupMaxBatches(int settlementTaskCleanupMaxBatches)
    {
        this.settlementTaskCleanupMaxBatches = settlementTaskCleanupMaxBatches;
    }

    public int getTransactionDeadlockMaxAttempts()
    {
        return transactionDeadlockMaxAttempts;
    }

    public void setTransactionDeadlockMaxAttempts(int transactionDeadlockMaxAttempts)
    {
        this.transactionDeadlockMaxAttempts = transactionDeadlockMaxAttempts;
    }

    public long getTransactionDeadlockBaseDelayMs()
    {
        return transactionDeadlockBaseDelayMs;
    }

    public void setTransactionDeadlockBaseDelayMs(long transactionDeadlockBaseDelayMs)
    {
        this.transactionDeadlockBaseDelayMs = transactionDeadlockBaseDelayMs;
    }

    public long getTransactionDeadlockMaxDelayMs()
    {
        return transactionDeadlockMaxDelayMs;
    }

    public void setTransactionDeadlockMaxDelayMs(long transactionDeadlockMaxDelayMs)
    {
        this.transactionDeadlockMaxDelayMs = transactionDeadlockMaxDelayMs;
    }
}
