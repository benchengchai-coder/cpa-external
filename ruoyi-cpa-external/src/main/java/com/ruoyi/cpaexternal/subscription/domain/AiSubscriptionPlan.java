package com.ruoyi.cpaexternal.subscription.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI订阅套餐对象 ai_subscription_plan
 */
public class AiSubscriptionPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long planId;

    private String title;

    private String subTitle;

    private BigDecimal priceAmount;

    private String durationUnit;

    private Integer durationValue;

    private Long customSeconds;

    private BigDecimal amountTotal;

    private String quotaResetPeriod;

    private Long quotaResetCustomSeconds;

    private String status;

    private Integer sortOrder;

    private Integer maxPurchasePerUser;

    private Integer allowBalancePurchase;

    public Long getPlanId()
    {
        return planId;
    }

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSubTitle()
    {
        return subTitle;
    }

    public void setSubTitle(String subTitle)
    {
        this.subTitle = subTitle;
    }

    public BigDecimal getPriceAmount()
    {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount)
    {
        this.priceAmount = priceAmount;
    }

    public String getDurationUnit()
    {
        return durationUnit;
    }

    public void setDurationUnit(String durationUnit)
    {
        this.durationUnit = durationUnit;
    }

    public Integer getDurationValue()
    {
        return durationValue;
    }

    public void setDurationValue(Integer durationValue)
    {
        this.durationValue = durationValue;
    }

    public Long getCustomSeconds()
    {
        return customSeconds;
    }

    public void setCustomSeconds(Long customSeconds)
    {
        this.customSeconds = customSeconds;
    }

    public BigDecimal getAmountTotal()
    {
        return amountTotal;
    }

    public void setAmountTotal(BigDecimal amountTotal)
    {
        this.amountTotal = amountTotal;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getMaxPurchasePerUser()
    {
        return maxPurchasePerUser;
    }

    public void setMaxPurchasePerUser(Integer maxPurchasePerUser)
    {
        this.maxPurchasePerUser = maxPurchasePerUser;
    }

    public Integer getAllowBalancePurchase()
    {
        return allowBalancePurchase;
    }

    public void setAllowBalancePurchase(Integer allowBalancePurchase)
    {
        this.allowBalancePurchase = allowBalancePurchase;
    }
}
