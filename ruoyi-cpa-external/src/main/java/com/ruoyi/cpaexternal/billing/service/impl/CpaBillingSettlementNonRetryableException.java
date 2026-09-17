package com.ruoyi.cpaexternal.billing.service.impl;

/** CLIProxyAPI 计费任务数据不满足自动重试条件。 */
public class CpaBillingSettlementNonRetryableException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public CpaBillingSettlementNonRetryableException(String message)
    {
        super(message);
    }
}
