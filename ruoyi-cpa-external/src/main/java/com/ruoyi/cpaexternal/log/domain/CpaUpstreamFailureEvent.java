package com.ruoyi.cpaexternal.log.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CLIProxyAPI errors 通道的上游失败事件载荷。
 *
 * <p>对应 CLIProxyAPI 每次上游凭证尝试失败时发布的事件，
 * 只消费外部系统已产生的失败统计，不参与 AI 请求转发。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaUpstreamFailureEvent
{
    /** 事件发生时间，ISO-8601 字符串（如 2026-09-17T10:59:53.738095654+08:00）。 */
    private String timestamp;
    private String provider;
    private String model;
    @JsonProperty("auth_id")
    private String authId;
    @JsonProperty("auth_index")
    private String authIndex;
    @JsonProperty("status_code")
    private Integer statusCode;
    private String body;
    private String code;
    private Boolean retryable;
    @JsonProperty("auth_status")
    private CpaErrorEventAuthStatus authStatus;

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getAuthId() { return authId; }
    public void setAuthId(String authId) { this.authId = authId; }
    public String getAuthIndex() { return authIndex; }
    public void setAuthIndex(String authIndex) { this.authIndex = authIndex; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Boolean getRetryable() { return retryable; }
    public void setRetryable(Boolean retryable) { this.retryable = retryable; }
    public CpaErrorEventAuthStatus getAuthStatus() { return authStatus; }
    public void setAuthStatus(CpaErrorEventAuthStatus authStatus) { this.authStatus = authStatus; }
}
