package com.ruoyi.cpaexternal.billing.domain;

import java.math.BigDecimal;
import java.util.Date;

/** 单次 CLIProxyAPI 请求的幂等计费记录。 */
public class CpaBillingRecord
{
    private Long billingId;
    private String requestId;
    private Long userId;
    private Long keyId;
    private Long subscriptionId;
    private String subscriptionPlanTitle;
    private String billingSource;
    private String billingPreference;
    private BigDecimal amount;
    private BigDecimal reservedAmount;
    private BigDecimal walletReservedAmount;
    private BigDecimal subscriptionReservedAmount;
    private BigDecimal keyReservedAmount;
    private BigDecimal walletChargedAmount;
    private BigDecimal subscriptionChargedAmount;
    private BigDecimal keyChargedAmount;
    private BigDecimal uncoveredAmount;
    private Date reserveExpireTime;
    private String status;
    private Long logId;
    private String errorMessage;
    private String resolutionReason;
    private String resolvedBy;
    private Date resolvedTime;
    private Date createTime;
    private Date updateTime;

    public Long getBillingId() { return billingId; }
    public void setBillingId(Long billingId) { this.billingId = billingId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKeyId() { return keyId; }
    public void setKeyId(Long keyId) { this.keyId = keyId; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getSubscriptionPlanTitle() { return subscriptionPlanTitle; }
    public void setSubscriptionPlanTitle(String subscriptionPlanTitle) { this.subscriptionPlanTitle = subscriptionPlanTitle; }
    public String getBillingSource() { return billingSource; }
    public void setBillingSource(String billingSource) { this.billingSource = billingSource; }
    public String getBillingPreference() { return billingPreference; }
    public void setBillingPreference(String billingPreference) { this.billingPreference = billingPreference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(BigDecimal reservedAmount) { this.reservedAmount = reservedAmount; }
    public BigDecimal getWalletReservedAmount() { return walletReservedAmount; }
    public void setWalletReservedAmount(BigDecimal walletReservedAmount) { this.walletReservedAmount = walletReservedAmount; }
    public BigDecimal getSubscriptionReservedAmount() { return subscriptionReservedAmount; }
    public void setSubscriptionReservedAmount(BigDecimal subscriptionReservedAmount) { this.subscriptionReservedAmount = subscriptionReservedAmount; }
    public BigDecimal getKeyReservedAmount() { return keyReservedAmount; }
    public void setKeyReservedAmount(BigDecimal keyReservedAmount) { this.keyReservedAmount = keyReservedAmount; }
    public BigDecimal getWalletChargedAmount() { return walletChargedAmount; }
    public void setWalletChargedAmount(BigDecimal walletChargedAmount) { this.walletChargedAmount = walletChargedAmount; }
    public BigDecimal getSubscriptionChargedAmount() { return subscriptionChargedAmount; }
    public void setSubscriptionChargedAmount(BigDecimal subscriptionChargedAmount) { this.subscriptionChargedAmount = subscriptionChargedAmount; }
    public BigDecimal getKeyChargedAmount() { return keyChargedAmount; }
    public void setKeyChargedAmount(BigDecimal keyChargedAmount) { this.keyChargedAmount = keyChargedAmount; }
    public BigDecimal getUncoveredAmount() { return uncoveredAmount; }
    public void setUncoveredAmount(BigDecimal uncoveredAmount) { this.uncoveredAmount = uncoveredAmount; }
    public Date getReserveExpireTime() { return reserveExpireTime; }
    public void setReserveExpireTime(Date reserveExpireTime) { this.reserveExpireTime = reserveExpireTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getResolutionReason() { return resolutionReason; }
    public void setResolutionReason(String resolutionReason) { this.resolutionReason = resolutionReason; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public Date getResolvedTime() { return resolvedTime; }
    public void setResolvedTime(Date resolvedTime) { this.resolvedTime = resolvedTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
