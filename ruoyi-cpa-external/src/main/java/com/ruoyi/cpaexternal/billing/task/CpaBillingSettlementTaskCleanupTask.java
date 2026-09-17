package com.ruoyi.cpaexternal.billing.task;

import java.util.Date;
import cn.hutool.core.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingSettlementService;

/**
 * CLIProxyAPI 计费已完成结算任务清理。
 */
@Component
public class CpaBillingSettlementTaskCleanupTask
{
    private static final Logger log = LoggerFactory.getLogger(CpaBillingSettlementTaskCleanupTask.class);

    @Autowired
    private ICpaBillingSettlementService billingSettlementService;

    @Autowired
    private CpaBillingProperties billingProperties;

    @Scheduled(
        cron = "${cpa.billing.settlement-task-cleanup-cron:0 0 0 * * *}",
        zone = "${cpa.billing.settlement-task-cleanup-zone:Asia/Shanghai}")
    public void cleanupDoneTasks()
    {
        int retentionDays = billingProperties.getSettlementTaskRetentionDays();
        int batchSize = billingProperties.getSettlementTaskCleanupBatchSize();
        int maxBatches = billingProperties.getSettlementTaskCleanupMaxBatches();
        Date beforeTime = DateUtil.offsetDay(new Date(), -retentionDays);
        int totalDeleted = 0;
        int deleted = 0;
        int batches = 0;
        try
        {
            do
            {
                deleted = billingSettlementService.deleteDoneTasksBefore(beforeTime, batchSize);
                totalDeleted += deleted;
                batches++;
            }
            while (deleted >= batchSize && batches < maxBatches);

            if (totalDeleted > 0)
            {
                log.info("已清理CLIProxyAPI计费完成任务: deleted={}, retentionDays={}, batches={}",
                    totalDeleted, retentionDays, batches);
            }
            if (deleted >= batchSize && batches >= maxBatches)
            {
                log.warn("CLIProxyAPI计费完成任务清理达到单次批次上限，剩余数据将在下次继续清理: deleted={}, maxBatches={}",
                    totalDeleted, maxBatches);
            }
        }
        catch (Exception e)
        {
            log.error("清理CLIProxyAPI计费完成任务失败: retentionDays={}, deleted={}",
                retentionDays, totalDeleted, e);
        }
    }
}
