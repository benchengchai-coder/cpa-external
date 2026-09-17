package com.ruoyi.cpaexternal.log.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 失败事件中触发状态变化的模型级凭证状态（CLIProxyAPI 按模型独立冷却时存在）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaErrorEventModelStatus
{
    private String name;
    private String status;
    @JsonProperty("status_message")
    private String statusMessage;
    private Boolean unavailable;
    @JsonProperty("next_retry_after")
    private String nextRetryAfter;
    private CpaErrorEventQuotaStatus quota;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    public Boolean getUnavailable() { return unavailable; }
    public void setUnavailable(Boolean unavailable) { this.unavailable = unavailable; }
    public String getNextRetryAfter() { return nextRetryAfter; }
    public void setNextRetryAfter(String nextRetryAfter) { this.nextRetryAfter = nextRetryAfter; }
    public CpaErrorEventQuotaStatus getQuota() { return quota; }
    public void setQuota(CpaErrorEventQuotaStatus quota) { this.quota = quota; }
}
