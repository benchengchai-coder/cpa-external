package com.ruoyi.cpaexternal.billing.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementFailedQuery;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementTask;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingSettlementFailedVO;

/** CLIProxyAPI 计费异步结算任务 Mapper。 */
public interface CpaBillingSettlementTaskMapper
{
    CpaBillingSettlementTask selectByRequestId(String requestId);

    CpaBillingSettlementTask selectByRequestIdForUpdate(String requestId);

    CpaBillingSettlementTask selectByBillingIdForUpdate(Long billingId);

    /** 创建结算任务，available_time 由调用方按延迟窗口传入。 */
    int insertTask(CpaBillingSettlementTask task);

    /** 把租约过期的已领取任务放回 pending。 */
    int requeueExpiredClaims(int limit);

    /** 领取到期可执行任务（FOR UPDATE SKIP LOCKED）。 */
    List<CpaBillingSettlementTask> selectPendingTasksForUpdate(int limit);

    /** 批量标记领取并写入租约。 */
    int claimTasks(@Param("taskIds") List<Long> taskIds, @Param("claimToken") String claimToken,
            @Param("claimExpireTime") Date claimExpireTime);

    int markTaskDone(@Param("taskId") Long taskId, @Param("claimToken") String claimToken);

    int markTaskRetry(@Param("taskId") Long taskId, @Param("claimToken") String claimToken,
            @Param("maxRetries") int maxRetries, @Param("availableTime") Date availableTime,
            @Param("errorMessage") String errorMessage);

    int markTaskFailed(@Param("taskId") Long taskId, @Param("claimToken") String claimToken,
            @Param("errorMessage") String errorMessage);

    /** 人工重试：failed → pending 并清零重试计数。 */
    int retryFailedTask(Long taskId);

    /** 人工核销后任务终态。 */
    int markTaskResolved(Long taskId);

    int deleteDoneTasksBefore(@Param("beforeTime") Date beforeTime, @Param("limit") int limit);

    List<CpaBillingSettlementFailedVO> selectFailedTaskList(CpaBillingSettlementFailedQuery query);

    int countFailedTasks();
}
