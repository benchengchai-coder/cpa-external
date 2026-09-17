package com.ruoyi.cpaexternal.billing.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingService;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingSettlementService;

/**
 * CLIProxyAPI 计费补偿与过期预占释放任务。
 */
@Component
public class CpaBillingReservationCleanupTask
{
    private static final Logger log = LoggerFactory.getLogger(CpaBillingReservationCleanupTask.class);

    @Autowired
    private ICpaBillingService billingService;

    @Autowired
    private ICpaBillingSettlementService billingSettlementService;

    @Autowired
    private CpaBillingProperties billingProperties;

    @Scheduled(fixedDelayString = "${cpa.billing.cleanup-interval-ms:60000}", initialDelayString = "30000")
    public void releaseExpiredReservations()
    {
        try
        {
            int repaired = billingSettlementService.repairMissingSettlementTasks(
                billingProperties.getSettlementClaimBatchSize());
            if (repaired > 0)
            {
                log.warn("已补建缺失的CLIProxyAPI异步结算任务: count={}", repaired);
            }
        }
        catch (Exception e)
        {
            log.error("补建缺失的CLIProxyAPI异步结算任务失败", e);
        }

        try
        {
            int released = billingService.releaseExpiredReservations(billingProperties.getCleanupBatchSize());
            if (released > 0)
            {
                log.warn("已释放过期CLIProxyAPI计费预占: count={}", released);
            }
        }
        catch (Exception e)
        {
            log.error("释放过期CLIProxyAPI计费预占失败", e);
        }

        try
        {
            // 并发计数对账：漂移只告警不修复，用于发现状态出口遗漏递减的缺陷
            billingService.logConcurrencyDriftIfAny();
        }
        catch (Exception e)
        {
            log.error("AI并发计数对账检查失败", e);
        }
    }
}
