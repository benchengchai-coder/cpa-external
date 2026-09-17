package com.ruoyi.cpaexternal.billing.domain;

/** 结算失败任务查询条件。 */
public class CpaBillingSettlementFailedQuery
{
    private String requestId;

    private String username;

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }
}
