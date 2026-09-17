package com.ruoyi.cpaexternal.billing.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettleCommand;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementFailedQuery;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementTask;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingSettlementFailedVO;

/** CLIProxyAPI 计费异步结算任务服务。 */
public interface ICpaBillingSettlementService
{
    /**
     * usage 落库后提交结算意图。
     *
     * <p>为 reserved 账单确保一个延迟结算任务（延迟窗口内聚合凭据重试产生的
     * 多条 usage）。无账单（预占未启用或插件未覆盖）时静默跳过，仅保留日志。</p>
     */
    void submitSettlement(CpaBillingSettleCommand command);

    /**
     * 补偿缺失的结算任务：
     * pending_settlement 无任务（含旧系统遗留）与延迟窗口后仍无任务的 reserved 账单。
     */
    int repairMissingSettlementTasks(int limit);

    List<CpaBillingSettlementTask> claimPendingTasks(String claimToken, int limit, long claimTimeoutSeconds);

    /** 执行已领取任务：领取时重读 ai_log.cost 作为最终结算金额，单事务完成意图与扣费。 */
    void processClaimedTask(CpaBillingSettlementTask task, String claimToken);

    int markTaskRetry(CpaBillingSettlementTask task, String claimToken, String errorMessage);

    int markTaskFailed(CpaBillingSettlementTask task, String claimToken, String errorMessage);

    List<CpaBillingSettlementFailedVO> selectFailedTaskList(CpaBillingSettlementFailedQuery query);

    int countFailedTasks();

    int deleteDoneTasksBefore(Date beforeTime, int limit);

    int retryFailedTask(String requestId);

    int writeOffFailedTask(String requestId, String reason, String operator);
}
