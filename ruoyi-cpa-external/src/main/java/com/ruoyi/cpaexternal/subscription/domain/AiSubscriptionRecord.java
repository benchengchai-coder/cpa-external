package com.ruoyi.cpaexternal.subscription.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI订阅流水对象 ai_subscription_record
 */
public class AiSubscriptionRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;

    private Long userId;

    private String username;

    private Long planId;

    private String planTitle;

    private Long userSubscriptionId;

    private String type;

    private BigDecimal amount;

    private String sourceName;

    private String operatorName;

    private String status;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public Long getPlanId()
    {
        return planId;
    }

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public String getPlanTitle()
    {
        return planTitle;
    }

    public void setPlanTitle(String planTitle)
    {
        this.planTitle = planTitle;
    }

    public Long getUserSubscriptionId()
    {
        return userSubscriptionId;
    }

    public void setUserSubscriptionId(Long userSubscriptionId)
    {
        this.userSubscriptionId = userSubscriptionId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public void setSourceName(String sourceName)
    {
        this.sourceName = sourceName;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
