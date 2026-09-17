package com.ruoyi.cpaexternal.log.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 失败事件中的配额状态（是否超限、原因与预计恢复时间）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaErrorEventQuotaStatus
{
    private Boolean exceeded;
    private String reason;
    @JsonProperty("next_recover_at")
    private String nextRecoverAt;
    @JsonProperty("backoff_level")
    private Integer backoffLevel;

    public Boolean getExceeded() { return exceeded; }
    public void setExceeded(Boolean exceeded) { this.exceeded = exceeded; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNextRecoverAt() { return nextRecoverAt; }
    public void setNextRecoverAt(String nextRecoverAt) { this.nextRecoverAt = nextRecoverAt; }
    public Integer getBackoffLevel() { return backoffLevel; }
    public void setBackoffLevel(Integer backoffLevel) { this.backoffLevel = backoffLevel; }
}
