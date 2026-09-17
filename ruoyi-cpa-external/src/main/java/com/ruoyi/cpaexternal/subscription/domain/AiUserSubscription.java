package com.ruoyi.cpaexternal.subscription.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI用户订阅对象 ai_user_subscription
 */
public class AiUserSubscription extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long subscriptionId;

    private Long userId;

    private String username;

    private Long planId;

    private String planTitle;

    private String planSubTitle;

    private BigDecimal priceAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private String status;

    private BigDecimal amountTotal;

    private BigDecimal amountUsed;

    private BigDecimal frozenBalance;

    private String quotaResetPeriod;

    private Long quotaResetCustomSeconds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastResetTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextResetTime;

    private String sourceType;

    public Long getSubscriptionId()
    {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId)
    {
        this.subscriptionId = subscriptionId;
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

    public String getPlanSubTitle()
    {
        return planSubTitle;
    }

    public void setPlanSubTitle(String planSubTitle)
    {
        this.planSubTitle = planSubTitle;
    }

    public BigDecimal getPriceAmount()
    {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount)
    {
        this.priceAmount = priceAmount;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public BigDecimal getAmountTotal()
    {
        return amountTotal;
    }

    public void setAmountTotal(BigDecimal amountTotal)
    {
        this.amountTotal = amountTotal;
    }

    public BigDecimal getAmountUsed()
    {
        return amountUsed;
    }

    public void setAmountUsed(BigDecimal amountUsed)
    {
        this.amountUsed = amountUsed;
    }

    public BigDecimal getFrozenBalance()
    {
        return frozenBalance;
    }

    public void setFrozenBalance(BigDecimal frozenBalance)
    {
        this.frozenBalance = frozenBalance;
    }

    public BigDecimal getAvailableAmount()
    {
        BigDecimal total = amountTotal == null ? BigDecimal.ZERO : amountTotal;
        if (total.compareTo(BigDecimal.ZERO) == 0)
        {
            return null;
        }
        BigDecimal used = amountUsed == null ? BigDecimal.ZERO : amountUsed;
        BigDecimal frozen = frozenBalance == null ? BigDecimal.ZERO : frozenBalance;
        BigDecimal available = total.subtract(used).subtract(frozen);
        return available.compareTo(BigDecimal.ZERO) > 0 ? available : BigDecimal.ZERO;
    }

    public String getQuotaResetPeriod()
    {
        return quotaResetPeriod;
    }

    public void setQuotaResetPeriod(String quotaResetPeriod)
    {
        this.quotaResetPeriod = quotaResetPeriod;
    }

    public Long getQuotaResetCustomSeconds()
    {
        return quotaResetCustomSeconds;
    }

    public void setQuotaResetCustomSeconds(Long quotaResetCustomSeconds)
    {
        this.quotaResetCustomSeconds = quotaResetCustomSeconds;
    }

    public Date getLastResetTime()
    {
        return lastResetTime;
    }

    public void setLastResetTime(Date lastResetTime)
    {
        this.lastResetTime = lastResetTime;
    }

    public Date getNextResetTime()
    {
        return nextResetTime;
    }

    public void setNextResetTime(Date nextResetTime)
    {
        this.nextResetTime = nextResetTime;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }
}
