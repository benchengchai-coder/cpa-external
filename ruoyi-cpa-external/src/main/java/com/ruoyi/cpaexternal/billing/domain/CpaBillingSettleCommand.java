package com.ruoyi.cpaexternal.billing.domain;

import java.math.BigDecimal;

/**
 * CLIProxyAPI 单次请求结算命令。
 *
 * <p>amount 仅是提交结算时的快照，供结算任务展示与对账参考；
 * Worker 领取任务时会重新读取 ai_log.cost 作为最终结算金额。</p>
 */
public class CpaBillingSettleCommand
{
    private String requestId;

    private Long userId;

    private Long keyId;

    private BigDecimal amount;

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getKeyId()
    {
        return keyId;
    }

    public void setKeyId(Long keyId)
    {
        this.keyId = keyId;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }
}
