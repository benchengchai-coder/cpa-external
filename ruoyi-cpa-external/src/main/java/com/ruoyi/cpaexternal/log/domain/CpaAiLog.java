package com.ruoyi.cpaexternal.log.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CLIProxyAPI 调用日志。
 *
 * <p>该对象只承载外部日志和管理查询，不参与 AI 请求转发。</p>
 */
public class CpaAiLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long logId;
    /** CLIProxyAPI 上报的日志时间，入库前已从 ISO-8601 字符串解析为 datetime。 */
    private Date timestamp;
    private Integer latencyMs;
    private Integer ttftMs;
    private String source;
    private String authIndex;
    private String accessTokenSha256;
    private String clientIp;
    private String xForwardedFor;
    private String userAgent;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer reasoningTokens;
    private Integer cachedTokens;
    private Integer cacheReadTokens;
    private Boolean cacheReadTokensPresent;
    private Integer cacheCreationTokens;
    private Integer totalTokens;
    private Boolean failed;
    private Boolean generate;
    private Boolean stream;
    private String fail;
    private Integer accountingVersion;
    private String tokenBreakdown;
    private String provider;
    private String executorType;
    private String model;
    private String alias;
    private String endpoint;
    private String authType;
    private String apiKey;
    private String requestId;
    private String sessionId;
    private String parentSessionId;
    private String reasoningEffort;
    private String serviceTier;
    private String responseServiceTier;
    private String responseHeaders;
    private Long userId;
    private Long keyId;
    private String username;
    private String keyName;
    private String modelName;
    private String status;
    /** 入库时用户计费倍率快照，未归属用户时为 null。 */
    private BigDecimal billingMultiplier;
    /** 计算费用（官方定价×用户倍率，美元），模型未配置定价时为 null。 */
    private BigDecimal cost;
    private Date createTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getAuthIndex() { return authIndex; }
    public void setAuthIndex(String authIndex) { this.authIndex = authIndex; }
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    public Integer getReasoningTokens() { return reasoningTokens; }
    public void setReasoningTokens(Integer reasoningTokens) { this.reasoningTokens = reasoningTokens; }
    public Integer getCachedTokens() { return cachedTokens; }
    public void setCachedTokens(Integer cachedTokens) { this.cachedTokens = cachedTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public Boolean getFailed() { return failed; }
    public void setFailed(Boolean failed) { this.failed = failed; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
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
    public Integer getTtftMs() { return ttftMs; }
    public void setTtftMs(Integer ttftMs) { this.ttftMs = ttftMs; }
    public String getAccessTokenSha256() { return accessTokenSha256; }
    public void setAccessTokenSha256(String accessTokenSha256) { this.accessTokenSha256 = accessTokenSha256; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getXForwardedFor() { return xForwardedFor; }
    public void setXForwardedFor(String xForwardedFor) { this.xForwardedFor = xForwardedFor; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setCacheReadTokens(Integer cacheReadTokens) { this.cacheReadTokens = cacheReadTokens; }
    public Boolean getCacheReadTokensPresent() { return cacheReadTokensPresent; }
    public void setCacheReadTokensPresent(Boolean cacheReadTokensPresent) { this.cacheReadTokensPresent = cacheReadTokensPresent; }
    public Integer getCacheCreationTokens() { return cacheCreationTokens; }
    public void setCacheCreationTokens(Integer cacheCreationTokens) { this.cacheCreationTokens = cacheCreationTokens; }
    public Boolean getGenerate() { return generate; }
    public void setGenerate(Boolean generate) { this.generate = generate; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public void setFail(String fail) { this.fail = fail; }
    public Integer getAccountingVersion() { return accountingVersion; }
    public void setAccountingVersion(Integer accountingVersion) { this.accountingVersion = accountingVersion; }
    public void setTokenBreakdown(String tokenBreakdown) { this.tokenBreakdown = tokenBreakdown; }
    public String getExecutorType() { return executorType; }
    public void setExecutorType(String executorType) { this.executorType = executorType; }
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

    /** 优先返回上游显式上报的缓存读取字段，旧数据回退到 cached_tokens。 */
    public Integer getCacheReadTokens()
    {
        return cacheReadTokens == null ? cachedTokens : cacheReadTokens;
    }

    /** 返回 JSON 对象而不是 JSON 字符串，保持 CLIProxyAPI 的数据形状。 */
    @JsonRawValue
    public String getResponseHeaders()
    {
        return responseHeaders == null ? "{}" : responseHeaders;
    }

    /** 返回 JSON 对象而不是 JSON 字符串，保持 CLIProxyAPI 的数据形状。 */
    @JsonRawValue
    public String getFail()
    {
        return fail == null ? "{}" : fail;
    }

    /** 返回 JSON 对象而不是 JSON 字符串，保持 CLIProxyAPI 的数据形状。 */
    @JsonRawValue
    public String getTokenBreakdown()
    {
        return tokenBreakdown == null ? "{}" : tokenBreakdown;
    }

    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKeyId() { return keyId; }
    public void setKeyId(Long keyId) { this.keyId = keyId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }
    public BigDecimal getBillingMultiplier() { return billingMultiplier; }
    public void setBillingMultiplier(BigDecimal billingMultiplier) { this.billingMultiplier = billingMultiplier; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    /* 兼容现有日志页面的只读别名，实际数据仍以 CLIProxyAPI 字段为准。 */
    public Date getRequestTime() { return timestamp; }
    public String getModelName() { return modelName == null || modelName.isEmpty() ? model : modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getChannelName() { return provider; }
    public String getRelayMode() { return endpoint; }
    public Integer getDuration() { return latencyMs; }
    public Integer getPromptTokens() { return inputTokens; }
    public Integer getCompletionTokens() { return outputTokens; }
    public Integer getReasoningOutputTokens() { return reasoningTokens; }
    public Integer getCacheWriteTokens() { return cacheCreationTokens; }
    public Integer getTtft() { return ttftMs; }
    public String getIp() { return clientIp; }
    public Integer getIsStream() { return Boolean.TRUE.equals(stream) ? 1 : 0; }
    public String getStatus() { return status == null ? (Boolean.TRUE.equals(failed) ? "1" : "0") : status; }
    public void setStatus(String status) { this.status = status; }
}
