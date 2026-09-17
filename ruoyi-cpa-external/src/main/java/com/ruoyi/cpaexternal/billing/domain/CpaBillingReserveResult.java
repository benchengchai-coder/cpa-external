package com.ruoyi.cpaexternal.billing.domain;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

/** CLIProxyAPI 请求前额度预占结果。 */
public class CpaBillingReserveResult
{
    private boolean allowed;
    @JsonProperty("request_id")
    private String requestId;
    @JsonProperty("billing_id")
    private Long billingId;
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("key_id")
    private Long keyId;
    @JsonProperty("subscription_id")
    private Long subscriptionId;
    @JsonProperty("billing_preference")
    private String billingPreference;
    @JsonProperty("reserved_amount")
    private BigDecimal reservedAmount;
    @JsonProperty("wallet_reserved_amount")
    private BigDecimal walletReservedAmount;
    @JsonProperty("subscription_reserved_amount")
    private BigDecimal subscriptionReservedAmount;
    @JsonProperty("key_reserved_amount")
    private BigDecimal keyReservedAmount;
    /** AI并发上限（0表示禁用AI访问） */
    @JsonProperty("concurrency_limit")
    private Integer concurrencyLimit;
    /** AI在途并发参考值（取自事务开始时的用户快照，仅用于诊断，权威判定以本结果allowed为准） */
    @JsonProperty("active_request_count")
    private Integer activeRequestCount;
    private String reason;

    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getBillingId() { return billingId; }
    public void setBillingId(Long billingId) { this.billingId = billingId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKeyId() { return keyId; }
    public void setKeyId(Long keyId) { this.keyId = keyId; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getBillingPreference() { return billingPreference; }
    public void setBillingPreference(String billingPreference) { this.billingPreference = billingPreference; }
    public BigDecimal getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(BigDecimal reservedAmount) { this.reservedAmount = reservedAmount; }
    public BigDecimal getWalletReservedAmount() { return walletReservedAmount; }
    public void setWalletReservedAmount(BigDecimal walletReservedAmount) { this.walletReservedAmount = walletReservedAmount; }
    public BigDecimal getSubscriptionReservedAmount() { return subscriptionReservedAmount; }
    public void setSubscriptionReservedAmount(BigDecimal subscriptionReservedAmount) { this.subscriptionReservedAmount = subscriptionReservedAmount; }
    public BigDecimal getKeyReservedAmount() { return keyReservedAmount; }
    public void setKeyReservedAmount(BigDecimal keyReservedAmount) { this.keyReservedAmount = keyReservedAmount; }
    public Integer getConcurrencyLimit() { return concurrencyLimit; }
    public void setConcurrencyLimit(Integer concurrencyLimit) { this.concurrencyLimit = concurrencyLimit; }
    public Integer getActiveRequestCount() { return activeRequestCount; }
    public void setActiveRequestCount(Integer activeRequestCount) { this.activeRequestCount = activeRequestCount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
