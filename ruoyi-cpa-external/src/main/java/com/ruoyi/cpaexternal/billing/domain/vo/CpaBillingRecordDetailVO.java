package com.ruoyi.cpaexternal.billing.domain.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;

/** 用户可见的 CLIProxyAPI 账单详情。 */
public class CpaBillingRecordDetailVO
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private Long billingId;

    private String requestId;

    private Long logId;

    private Long subscriptionId;

    private String subscriptionPlanTitle;

    private BigDecimal amount;

    private BigDecimal userChargedAmount;

    private BigDecimal walletChargedAmount;

    private BigDecimal subscriptionChargedAmount;

    private BigDecimal keyChargedAmount;

    private BigDecimal uncoveredAmount;

    private String billingSource;

    private String status;

    private Date createTime;

    private Date updateTime;

    public static CpaBillingRecordDetailVO from(CpaBillingRecord record)
    {
        CpaBillingRecordDetailVO detail = new CpaBillingRecordDetailVO();
        detail.setBillingId(record.getBillingId());
        detail.setRequestId(record.getRequestId());
        detail.setLogId(record.getLogId());
        detail.setSubscriptionId(record.getSubscriptionId());
        detail.setSubscriptionPlanTitle(record.getSubscriptionPlanTitle());
        detail.setAmount(nvl(record.getAmount()));
        detail.setWalletChargedAmount(nvl(record.getWalletChargedAmount()));
        detail.setSubscriptionChargedAmount(nvl(record.getSubscriptionChargedAmount()));
        detail.setUserChargedAmount(detail.getWalletChargedAmount().add(detail.getSubscriptionChargedAmount()));
        detail.setKeyChargedAmount(nvl(record.getKeyChargedAmount()));
        detail.setUncoveredAmount(nvl(record.getUncoveredAmount()));
        detail.setBillingSource(record.getBillingSource());
        detail.setStatus(record.getStatus());
        detail.setCreateTime(record.getCreateTime());
        detail.setUpdateTime(record.getUpdateTime());
        return detail;
    }

    private static BigDecimal nvl(BigDecimal value)
    {
        return value == null ? ZERO : value;
    }

    public Long getBillingId()
    {
        return billingId;
    }

    public void setBillingId(Long billingId)
    {
        this.billingId = billingId;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getSubscriptionId()
    {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId)
    {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionPlanTitle()
    {
        return subscriptionPlanTitle;
    }

    public void setSubscriptionPlanTitle(String subscriptionPlanTitle)
    {
        this.subscriptionPlanTitle = subscriptionPlanTitle;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public BigDecimal getUserChargedAmount()
    {
        return userChargedAmount;
    }

    public void setUserChargedAmount(BigDecimal userChargedAmount)
    {
        this.userChargedAmount = userChargedAmount;
    }

    public BigDecimal getWalletChargedAmount()
    {
        return walletChargedAmount;
    }

    public void setWalletChargedAmount(BigDecimal walletChargedAmount)
    {
        this.walletChargedAmount = walletChargedAmount;
    }

    public BigDecimal getSubscriptionChargedAmount()
    {
        return subscriptionChargedAmount;
    }

    public void setSubscriptionChargedAmount(BigDecimal subscriptionChargedAmount)
    {
        this.subscriptionChargedAmount = subscriptionChargedAmount;
    }

    public BigDecimal getKeyChargedAmount()
    {
        return keyChargedAmount;
    }

    public void setKeyChargedAmount(BigDecimal keyChargedAmount)
    {
        this.keyChargedAmount = keyChargedAmount;
    }

    public BigDecimal getUncoveredAmount()
    {
        return uncoveredAmount;
    }

    public void setUncoveredAmount(BigDecimal uncoveredAmount)
    {
        this.uncoveredAmount = uncoveredAmount;
    }

    public String getBillingSource()
    {
        return billingSource;
    }

    public void setBillingSource(String billingSource)
    {
        this.billingSource = billingSource;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
