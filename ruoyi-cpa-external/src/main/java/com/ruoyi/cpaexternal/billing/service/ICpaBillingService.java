package com.ruoyi.cpaexternal.billing.service;

import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingReserveRequest;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingReserveResult;

/** CLIProxyAPI 额度预占与释放服务。 */
public interface ICpaBillingService
{
    /**
     * 请求前额度预占。
     *
     * <p>按 request_id 幂等：同一 request_id 重复预占时，若账单处于可结算状态
     * 则回放既有预占结果，否则抛出异常。预占失败抛 ServiceException，整体回滚。</p>
     */
    CpaBillingReserveResult reserve(CpaBillingReserveRequest request);

    /** 主动释放预占（仅 reserved 状态生效），CLIProxyAPI 在请求未产生用量时调用。 */
    void release(String requestId, String reason);

    CpaBillingRecord selectByRequestId(String requestId);

    /** 批量释放超时未结算的预占，返回释放条数。 */
    int releaseExpiredReservations(int limit);

    /**
     * 对账 AI 并发计数列与 reserved 在途账单数，漂移时仅记录告警日志，不自动修复。
     *
     * @return 存在漂移的用户数
     */
    int logConcurrencyDriftIfAny();
}
