package com.ruoyi.cpaexternal.billing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingConcurrencyDriftVO;

/** CLIProxyAPI 幂等计费账单 Mapper。 */
public interface CpaBillingRecordMapper
{
    CpaBillingRecord selectByRequestId(String requestId);

    CpaBillingRecord selectByRequestIdForUpdate(String requestId);

    CpaBillingRecord selectByIdForUpdate(Long billingId);

    /** 查询已超时且没有结算任务的预占账单，供超时释放任务批量处理。 */
    List<Long> selectExpiredReservedIds(int limit);

    /** 查询已持久化结算意图但没有结算任务的账单（含旧系统遗留数据），供补偿任务补建任务。 */
    List<Long> selectPendingWithoutTask(int limit);

    /** 查询已收到 usage 日志、处于 reserved 且没有结算任务的账单，供延迟窗口后的结算意图补偿。 */
    List<Long> selectReservedWithLogWithoutTask(@Param("limit") int limit, @Param("beforeTime") java.util.Date beforeTime);

    /** 幂等插入占位账单，request_id 冲突时返回 0。 */
    int insertIgnore(CpaBillingRecord record);

    /** 预占成功 CAS：processing → reserved。 */
    int updateReserved(CpaBillingRecord record);

    /** 结算意图 CAS：reserved → pending_settlement 并写入应计金额。 */
    int updatePendingSettlement(CpaBillingRecord record);

    /** 最终结算 CAS：pending_settlement → success / partial。 */
    int updateSettlementFromPending(CpaBillingRecord record);

    /** 终态 CAS：reserved → released / expired / failed。 */
    int updateTerminal(@Param("billingId") Long billingId, @Param("status") String status,
            @Param("errorMessage") String errorMessage);

    /** 人工核销 CAS：未产生实扣的账单 → written_off。 */
    int updateWrittenOff(@Param("billingId") Long billingId, @Param("resolutionReason") String resolutionReason,
            @Param("resolvedBy") String resolvedBy);

    /** 回填账单关联的结算日志。 */
    int updateLogId(@Param("requestId") String requestId, @Param("logId") Long logId);

    /** 汇总用户累计实扣与未覆盖金额（仅 success / partial）。 */
    CpaBillingAmountSummaryVO selectUserAmountSummary(Long userId);

    /** 对账查询：AI并发计数列与 reserved 在途账单数不一致的用户（仅诊断告警用）。 */
    List<CpaBillingConcurrencyDriftVO> selectConcurrencyDrift();
}
