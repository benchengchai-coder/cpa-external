package com.ruoyi.cpaexternal.billing.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 人工核销结算失败任务请求。 */
public class CpaBillingSettlementWriteOffRequest
{
    @NotBlank(message = "核销原因不能为空")
    @Size(max = 500, message = "核销原因不能超过500个字符")
    private String reason;

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }
}
