package com.ruoyi.pay.domain;

import java.util.Date;

/**
 * 支付纯域操作的结果（返回给聚合层编排副作用）。
 * <p>
 * 通用载体：标识本次操作是否完成首次入账（settled），并携带订单与交易号，
 * 供 PayFacade 完成「加余额 + 写账户流水 + 触发返利」。
 */
public class PayResult
{
    /** 是否完成首次入账（CAS 0→1 成功），仅此时才需触发副作用 */
    private final boolean settled;

    /** 入账订单 */
    private final AiPayOrder order;

    /** 支付宝交易号 */
    private final String tradeNo;

    /** 支付时间 */
    private final Date payTime;

    public PayResult(boolean settled, AiPayOrder order, String tradeNo, Date payTime)
    {
        this.settled = settled;
        this.order = order;
        this.tradeNo = tradeNo;
        this.payTime = payTime;
    }

    public boolean isSettled()
    {
        return settled;
    }

    public AiPayOrder getOrder()
    {
        return order;
    }

    public String getTradeNo()
    {
        return tradeNo;
    }

    public Date getPayTime()
    {
        return payTime;
    }
}
