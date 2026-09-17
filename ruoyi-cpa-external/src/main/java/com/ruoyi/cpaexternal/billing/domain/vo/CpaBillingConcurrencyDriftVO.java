package com.ruoyi.cpaexternal.billing.domain.vo;

/** AI并发计数与reserved账单数的对账漂移结果（仅告警诊断用）。 */
public class CpaBillingConcurrencyDriftVO
{
    /** 用户ID */
    private Long userId;

    /** sys_user.active_request_count 计数列当前值 */
    private Integer counterCount;

    /** status='reserved' 的在途账单实际数量 */
    private Integer recordCount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getCounterCount() { return counterCount; }
    public void setCounterCount(Integer counterCount) { this.counterCount = counterCount; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
}
