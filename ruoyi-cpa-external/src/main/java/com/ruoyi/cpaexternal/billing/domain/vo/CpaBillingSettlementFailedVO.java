package com.ruoyi.cpaexternal.billing.domain.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/** 结算失败任务列表数据。 */
public class CpaBillingSettlementFailedVO
{
    private Long taskId;

    private Long billingId;

    private String requestId;

    private Long userId;

    private String username;

    private Long keyId;

    private String keyName;

    private BigDecimal amount;

    private BigDecimal walletReservedAmount;

    private BigDecimal subscriptionReservedAmount;

    private BigDecimal keyReservedAmount;

    private Integer retryCount;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date failedTime;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
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

    public Long getKeyId()
    {
        return keyId;
    }

    public void setKeyId(Long keyId)
    {
        this.keyId = keyId;
    }

    public String getKeyName()
    {
        return keyName;
    }

    public void setKeyName(String keyName)
    {
        this.keyName = keyName;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public BigDecimal getWalletReservedAmount()
    {
        return walletReservedAmount;
    }

    public void setWalletReservedAmount(BigDecimal walletReservedAmount)
    {
        this.walletReservedAmount = walletReservedAmount;
    }

    public BigDecimal getSubscriptionReservedAmount()
    {
        return subscriptionReservedAmount;
    }

    public void setSubscriptionReservedAmount(BigDecimal subscriptionReservedAmount)
    {
        this.subscriptionReservedAmount = subscriptionReservedAmount;
    }

    public BigDecimal getKeyReservedAmount()
    {
        return keyReservedAmount;
    }

    public void setKeyReservedAmount(BigDecimal keyReservedAmount)
    {
        this.keyReservedAmount = keyReservedAmount;
    }

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public Date getFailedTime()
    {
        return failedTime;
    }

    public void setFailedTime(Date failedTime)
    {
        this.failedTime = failedTime;
    }
}
