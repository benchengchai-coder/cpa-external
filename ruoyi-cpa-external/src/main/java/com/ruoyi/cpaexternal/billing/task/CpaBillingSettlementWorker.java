package com.ruoyi.cpaexternal.billing.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementTask;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingSettlementService;
import com.ruoyi.cpaexternal.billing.service.impl.CpaBillingSettlementNonRetryableException;

/**
 * CLIProxyAPI 持久化异步结算队列消费者。
 *
 * <p>同一用户的任务固定分配到同一分区串行执行，避免同用户并发结算互相死锁；
 * 领取使用租约，进程崩溃后任务自动回队。</p>
 */
@Component
public class CpaBillingSettlementWorker
{
    private static final Logger log = LoggerFactory.getLogger(CpaBillingSettlementWorker.class);

    @Autowired
    private ICpaBillingSettlementService billingSettlementService;

    @Autowired
    private CpaBillingProperties billingProperties;

    private final AtomicInteger activePartitions = new AtomicInteger(0);

    private ExecutorService settlementExecutor;

    private volatile boolean running;

    @PostConstruct
    public void start()
    {
        if (!billingProperties.isSettlementWorkerEnabled())
        {
            return;
        }
        AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable ->
        {
            Thread thread = new Thread(runnable,
                    "cpa-billing-settlement-" + threadNumber.getAndIncrement());
            // 守护线程不阻止 JVM 退出；应用关闭时 @PreDestroy 仍会等待正在执行的结算任务完成
            thread.setDaemon(true);
            return thread;
        };
        settlementExecutor = Executors.newFixedThreadPool(
                billingProperties.getSettlementWorkerThreads(), threadFactory);
        running = true;
        log.info("CLIProxyAPI异步结算Worker已启动: threads={}, claimBatchSize={}",
                billingProperties.getSettlementWorkerThreads(),
                billingProperties.getSettlementClaimBatchSize());
    }

    @Scheduled(fixedDelayString = "${cpa.billing.settlement-poll-interval-ms:5000}",
            initialDelayString = "1000")
    public void poll()
    {
        if (!running || !billingProperties.isSettlementWorkerEnabled() || settlementExecutor == null
                || activePartitions.get() > 0)
        {
            return;
        }
        String claimToken = IdUtil.fastSimpleUUID();
        try
        {
            List<CpaBillingSettlementTask> tasks = billingSettlementService.claimPendingTasks(
                    claimToken, billingProperties.getSettlementClaimBatchSize(),
                    billingProperties.getSettlementClaimTimeoutSeconds());
            if (tasks.isEmpty())
            {
                return;
            }
            dispatchByUser(claimToken, tasks);
        }
        catch (Exception e)
        {
            log.error("领取CLIProxyAPI异步结算任务失败", e);
        }
    }

    private void dispatchByUser(String claimToken, List<CpaBillingSettlementTask> tasks)
    {
        Map<Long, List<CpaBillingSettlementTask>> tasksByUser = new LinkedHashMap<>();
        for (CpaBillingSettlementTask task : tasks)
        {
            tasksByUser.computeIfAbsent(task.getUserId(), key -> new ArrayList<>()).add(task);
        }
        for (List<CpaBillingSettlementTask> userTasks : tasksByUser.values())
        {
            userTasks.sort(Comparator.comparing(CpaBillingSettlementTask::getTaskId));
        }
        int partitionCount = Math.min(billingProperties.getSettlementWorkerThreads(), tasksByUser.size());
        List<List<Map.Entry<Long, List<CpaBillingSettlementTask>>>> partitions = new ArrayList<>(partitionCount);
        for (int i = 0; i < partitionCount; i++)
        {
            partitions.add(new ArrayList<>());
        }
        int partitionIndex = 0;
        for (Map.Entry<Long, List<CpaBillingSettlementTask>> entry : tasksByUser.entrySet())
        {
            partitions.get(partitionIndex % partitionCount).add(entry);
            partitionIndex++;
        }

        activePartitions.set(partitionCount);
        for (List<Map.Entry<Long, List<CpaBillingSettlementTask>>> partition : partitions)
        {
            try
            {
                settlementExecutor.execute(() -> processPartition(claimToken, partition));
            }
            catch (Exception e)
            {
                activePartitions.decrementAndGet();
                log.error("提交CLIProxyAPI异步结算分区失败，任务将在领取超时后恢复", e);
            }
        }
    }

    private void processPartition(String claimToken,
                                  List<Map.Entry<Long, List<CpaBillingSettlementTask>>> partition)
    {
        try
        {
            for (Map.Entry<Long, List<CpaBillingSettlementTask>> entry : partition)
            {
                processUserTasks(claimToken, entry.getKey(), entry.getValue());
            }
        }
        finally
        {
            activePartitions.decrementAndGet();
        }
    }

    private void processUserTasks(String claimToken, Long userId, List<CpaBillingSettlementTask> tasks)
    {
        for (CpaBillingSettlementTask task : tasks)
        {
            try
            {
                billingSettlementService.processClaimedTask(task, claimToken);
            }
            catch (CpaBillingSettlementNonRetryableException e)
            {
                handleNonRetryableFailure(task, claimToken, userId, e);
            }
            catch (Exception e)
            {
                handleRetryableFailure(task, claimToken, userId, e);
            }
        }
    }

    private void handleNonRetryableFailure(CpaBillingSettlementTask task, String claimToken,
                                           Long userId, Exception cause)
    {
        try
        {
            int rows = billingSettlementService.markTaskFailed(task, claimToken, cause.getMessage());
            log.error("CLIProxyAPI异步结算数据异常，任务已停止自动重试: userId={}, taskId={}, updated={}, error={}",
                    userId, task.getTaskId(), rows, cause.getMessage());
        }
        catch (Exception markException)
        {
            log.error("标记CLIProxyAPI异步结算任务失败状态失败，任务将在租约到期后恢复: userId={}, taskId={}",
                    userId, task.getTaskId(), markException);
        }
    }

    private void handleRetryableFailure(CpaBillingSettlementTask task, String claimToken,
                                        Long userId, Exception cause)
    {
        try
        {
            int rows = billingSettlementService.markTaskRetry(task, claimToken, cause.getMessage());
            log.warn("CLIProxyAPI异步结算失败，已更新任务重试状态: userId={}, taskId={}, updated={}, error={}",
                    userId, task.getTaskId(), rows, cause.getMessage());
        }
        catch (Exception markException)
        {
            log.error("标记CLIProxyAPI异步结算重试失败，任务将在租约到期后恢复: userId={}, taskId={}",
                    userId, task.getTaskId(), markException);
        }
    }

    @PreDestroy
    public void stop()
    {
        running = false;
        if (settlementExecutor == null)
        {
            return;
        }
        settlementExecutor.shutdown();
        try
        {
            if (!settlementExecutor.awaitTermination(
                    billingProperties.getSettlementShutdownWaitMs(), TimeUnit.MILLISECONDS))
            {
                settlementExecutor.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            settlementExecutor.shutdownNow();
        }
    }
}
