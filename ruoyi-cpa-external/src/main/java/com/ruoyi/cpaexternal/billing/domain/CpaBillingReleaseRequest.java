package com.ruoyi.cpaexternal.billing.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** CLIProxyAPI 主动释放请求预占的请求。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaBillingReleaseRequest
{
    @JsonProperty("request_id")
    private String requestId;
    private String reason;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
