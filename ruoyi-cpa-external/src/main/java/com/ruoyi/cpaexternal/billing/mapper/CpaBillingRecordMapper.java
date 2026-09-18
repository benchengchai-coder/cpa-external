package com.ruoyi.cpaexternal.billing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;

/** CLIProxyAPI 幂等计费账单 Mapper。 */
public interface CpaBillingRecordMapper
{
    CpaBillingRecord selectByRequestId(String requestId);

    CpaBillingRecord selectByRequestIdForUpdate(String requestId);

    CpaBillingRecord selectByIdForUpdate(Long billingId);

    /** 查询已持久化结算意图但没有结算任务的账单（含旧系统遗留数据），供补偿任务补建任务。 */
    List<Long> selectPendingWithoutTask(int limit);

    /** 幂等插入占位账单，request_id 冲突时返回 0。 */
    int insertIgnore(CpaBillingRecord record);

    /** 结算意图 CAS：reserved → pending_settlement 并写入应计金额。 */
    int updatePendingSettlement(CpaBillingRecord record);

    int updatePendingAmount(CpaBillingRecord record);

    /** 最终结算 CAS：pending_settlement → success / partial。 */
    int updateSettlementFromPending(CpaBillingRecord record);

    /** 人工核销 CAS：未产生实扣的账单 → written_off。 */
    int updateWrittenOff(@Param("billingId") Long billingId, @Param("resolutionReason") String resolutionReason,
            @Param("resolvedBy") String resolvedBy);

    /** 回填账单关联的结算日志。 */
    int updateLogId(@Param("requestId") String requestId, @Param("logId") Long logId);

    /** 汇总用户累计实扣与未覆盖金额（仅 success / partial）。 */
    CpaBillingAmountSummaryVO selectUserAmountSummary(Long userId);

}
