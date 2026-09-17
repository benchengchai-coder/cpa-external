package com.ruoyi.cpaexternal.billing.service;

import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingRecordDetailVO;

/** CLIProxyAPI 账单只读查询服务。 */
public interface ICpaBillingRecordQueryService
{
    CpaBillingRecordDetailVO selectUserBillingByLogId(Long logId, Long userId);

    CpaBillingRecordDetailVO selectAdminBillingByLogId(Long logId);

    CpaBillingAmountSummaryVO selectUserAmountSummary(Long userId);
}
