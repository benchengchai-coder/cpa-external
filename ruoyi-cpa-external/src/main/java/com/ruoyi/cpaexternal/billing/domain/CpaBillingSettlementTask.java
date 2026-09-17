package com.ruoyi.cpaexternal.billing.domain;

import java.math.BigDecimal;
import java.util.Date;

/** 持久化计费结算任务。 */
public class CpaBillingSettlementTask
{
    private Long taskId;
    private Long billingId;
    private String requestId;
    private Long userId;
    private Long keyId;
    private BigDecimal actualCost;
    private String status;
    private Integer retryCount;
    private Date availableTime;
    private String claimToken;
    private Date claimExpireTime;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getBillingId() { return billingId; }
    public void setBillingId(Long billingId) { this.billingId = billingId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKeyId() { return keyId; }
    public void setKeyId(Long keyId) { this.keyId = keyId; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Date getAvailableTime() { return availableTime; }
    public void setAvailableTime(Date availableTime) { this.availableTime = availableTime; }
    public String getClaimToken() { return claimToken; }
    public void setClaimToken(String claimToken) { this.claimToken = claimToken; }
    public Date getClaimExpireTime() { return claimExpireTime; }
    public void setClaimExpireTime(Date claimExpireTime) { this.claimExpireTime = claimExpireTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
