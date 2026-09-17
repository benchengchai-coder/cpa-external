package com.ruoyi.cpaexternal.billing.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** CLIProxyAPI 请求前额度预占请求。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaBillingReserveRequest
{
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("api_key")
    private String apiKey;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
