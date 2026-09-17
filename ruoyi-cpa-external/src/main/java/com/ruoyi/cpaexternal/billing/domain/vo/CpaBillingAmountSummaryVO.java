package com.ruoyi.cpaexternal.billing.domain.vo;

import java.math.BigDecimal;

/** 用户账单金额汇总。 */
public class CpaBillingAmountSummaryVO
{
    /** 累计实扣金额（钱包 + 订阅）。 */
    private BigDecimal totalChargedAmount;

    /** 累计未覆盖金额。 */
    private BigDecimal totalUncoveredAmount;

    public BigDecimal getTotalChargedAmount()
    {
        return totalChargedAmount;
    }

    public void setTotalChargedAmount(BigDecimal totalChargedAmount)
    {
        this.totalChargedAmount = totalChargedAmount;
    }

    public BigDecimal getTotalUncoveredAmount()
    {
        return totalUncoveredAmount;
    }

    public void setTotalUncoveredAmount(BigDecimal totalUncoveredAmount)
    {
        this.totalUncoveredAmount = totalUncoveredAmount;
    }
}
