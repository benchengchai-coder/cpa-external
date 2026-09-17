package com.ruoyi.cpaexternal.log.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 失败事件携带的凭证状态快照（禁用、冷却、配额等），用于渠道健康分析。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaErrorEventAuthStatus
{
    private String status;
    @JsonProperty("status_message")
    private String statusMessage;
    private Boolean disabled;
    private Boolean unavailable;
    @JsonProperty("next_retry_after")
    private String nextRetryAfter;
    private CpaErrorEventQuotaStatus quota;
    private CpaErrorEventModelStatus model;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public Boolean getDisabled() { return disabled; }
    public void setDisabled(Boolean disabled) { this.disabled = disabled; }
    public Boolean getUnavailable() { return unavailable; }
    public void setUnavailable(Boolean unavailable) { this.unavailable = unavailable; }
    public String getNextRetryAfter() { return nextRetryAfter; }
    public void setNextRetryAfter(String nextRetryAfter) { this.nextRetryAfter = nextRetryAfter; }
    public CpaErrorEventQuotaStatus getQuota() { return quota; }
    public void setQuota(CpaErrorEventQuotaStatus quota) { this.quota = quota; }
    public CpaErrorEventModelStatus getModel() { return model; }
    public void setModel(CpaErrorEventModelStatus model) { this.model = model; }
}
