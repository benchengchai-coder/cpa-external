package com.ruoyi.cpaexternal.log.task;

import java.util.Date;
import cn.hutool.core.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.cpaexternal.log.service.ICpaUpstreamFailureService;
import com.ruoyi.cpaexternal.log.subscription.CpaErrorsSubscriptionProperties;

/**
 * CLIProxyAPI 上游失败事件保留期清理。
 *
 * <p>失败事件只用于短期排查与告警，默认保留 3 天，按配置分批删除，
 * 单次执行达到批次上限时停止，剩余数据留给下一次任务。</p>
 */
@Component
public class CpaUpstreamFailureCleanupTask
{
    private static final Logger log = LoggerFactory.getLogger(CpaUpstreamFailureCleanupTask.class);

    @Autowired
    private ICpaUpstreamFailureService upstreamFailureService;

    @Autowired
    private CpaErrorsSubscriptionProperties errorsProperties;

    @Scheduled(
        cron = "${cpa.cli-proxy.errors-subscription.cleanup-cron:0 30 1 * * *}",
        zone = "${cpa.cli-proxy.errors-subscription.cleanup-zone:Asia/Shanghai}")
    public void cleanupExpiredFailures()
    {
        int retentionDays = errorsProperties.getRetentionDays();
        int batchSize = errorsProperties.getCleanupBatchSize();
        int maxBatches = errorsProperties.getCleanupMaxBatches();
        Date beforeTime = DateUtil.offsetDay(new Date(), -retentionDays);
        int totalDeleted = 0;
        int deleted = 0;
        int batches = 0;
        try
        {
            do
            {
                deleted = upstreamFailureService.deleteBefore(beforeTime, batchSize);
                totalDeleted += deleted;
                batches++;
            }
            while (deleted >= batchSize && batches < maxBatches);

            if (totalDeleted > 0)
            {
                log.info("已清理CLIProxyAPI上游失败事件: deleted={}, retentionDays={}, batches={}",
                    totalDeleted, retentionDays, batches);
            }
            if (deleted >= batchSize && batches >= maxBatches)
            {
                log.warn("CLIProxyAPI上游失败事件清理达到单次批次上限，剩余数据将在下次继续清理: deleted={}, maxBatches={}",
                    totalDeleted, maxBatches);
            }
        }
        catch (Exception e)
        {
            log.error("清理CLIProxyAPI上游失败事件失败: retentionDays={}, deleted={}",
                retentionDays, totalDeleted, e);
        }
    }
}
