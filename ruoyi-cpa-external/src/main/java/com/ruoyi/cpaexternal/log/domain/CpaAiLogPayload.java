package com.ruoyi.cpaexternal.log.domain;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** CLIProxyAPI 日志上报载荷。字段名与 CLIProxyAPI 输出保持一致。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CpaAiLogPayload
{
    private String timestamp;
    @JsonProperty("latency_ms")
    private Integer latencyMs;
    @JsonProperty("ttft_ms")
    private Integer ttftMs;
    private String source;
    @JsonProperty("auth_index")
    private String authIndex;
    @JsonProperty("access_token_sha256")
    private String accessTokenSha256;
    @JsonProperty("client_ip")
    private String clientIp;
    @JsonProperty("x_forwarded_for")
    private String xForwardedFor;
    @JsonProperty("user_agent")
    private String userAgent;
    private CpaAiLogTokens tokens;
    private Boolean failed;
    private Boolean generate;
    private Boolean stream;
    private CpaAiLogFail fail;
    @JsonProperty("response_headers")
    private Map<String, List<String>> responseHeaders;
    @JsonProperty("accounting_version")
    private Integer accountingVersion;
    @JsonProperty("token_breakdown")
    private CpaAiLogTokenBreakdown tokenBreakdown;
    private String provider;
    @JsonProperty("executor_type")
    private String executorType;
    private String model;
    private String alias;
    private String endpoint;
    @JsonProperty("auth_type")
    private String authType;
    @JsonProperty("api_key")
    private String apiKey;
    @JsonProperty("request_id")
    private String requestId;
    @JsonProperty("session_id")
    private String sessionId;
    @JsonProperty("parent_session_id")
    private String parentSessionId;
    @JsonProperty("reasoning_effort")
    private String reasoningEffort;
    @JsonProperty("service_tier")
    private String serviceTier;
    @JsonProperty("response_service_tier")
    private String responseServiceTier;

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public Integer getTtftMs() { return ttftMs; }
    public void setTtftMs(Integer ttftMs) { this.ttftMs = ttftMs; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getAuthIndex() { return authIndex; }
    public void setAuthIndex(String authIndex) { this.authIndex = authIndex; }
    public String getAccessTokenSha256() { return accessTokenSha256; }
    public void setAccessTokenSha256(String accessTokenSha256) { this.accessTokenSha256 = accessTokenSha256; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getXForwardedFor() { return xForwardedFor; }
    public void setXForwardedFor(String xForwardedFor) { this.xForwardedFor = xForwardedFor; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public CpaAiLogTokens getTokens() { return tokens; }
    public void setTokens(CpaAiLogTokens tokens) { this.tokens = tokens; }
    public Boolean getFailed() { return failed; }
    public void setFailed(Boolean failed) { this.failed = failed; }
    public Boolean getGenerate() { return generate; }
    public void setGenerate(Boolean generate) { this.generate = generate; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public CpaAiLogFail getFail() { return fail; }
    public void setFail(CpaAiLogFail fail) { this.fail = fail; }
    public Map<String, List<String>> getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(Map<String, List<String>> responseHeaders) { this.responseHeaders = responseHeaders; }
    public Integer getAccountingVersion() { return accountingVersion; }
    public void setAccountingVersion(Integer accountingVersion) { this.accountingVersion = accountingVersion; }
    public CpaAiLogTokenBreakdown getTokenBreakdown() { return tokenBreakdown; }
    public void setTokenBreakdown(CpaAiLogTokenBreakdown tokenBreakdown) { this.tokenBreakdown = tokenBreakdown; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getExecutorType() { return executorType; }
    public void setExecutorType(String executorType) { this.executorType = executorType; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getParentSessionId() { return parentSessionId; }
    public void setParentSessionId(String parentSessionId) { this.parentSessionId = parentSessionId; }
    public String getReasoningEffort() { return reasoningEffort; }
    public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
    public String getServiceTier() { return serviceTier; }
    public void setServiceTier(String serviceTier) { this.serviceTier = serviceTier; }
    public String getResponseServiceTier() { return responseServiceTier; }
    public void setResponseServiceTier(String responseServiceTier) { this.responseServiceTier = responseServiceTier; }
}
