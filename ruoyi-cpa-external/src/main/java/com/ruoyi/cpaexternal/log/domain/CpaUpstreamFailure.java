package com.ruoyi.cpaexternal.log.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CLIProxyAPI 上游失败事件（ai_upstream_failure 表）。
 *
 * <p>errors 通道事件只做追加写入：没有 request_id 或事件 ID，无法与具体用户请求关联，
 * 也无法去重，每条消息对应一行记录。</p>
 */
public class CpaUpstreamFailure extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long failureId;
    /** 事件发生时间（CLIProxyAPI 上报时间，入库前已从 ISO-8601 字符串解析）。 */
    private Date eventTime;
    private String provider;
    private String model;
    private String authId;
    private String authIndex;
    private Integer statusCode;
    /** 上游错误消息或响应体文本。 */
    private String body;
    /** CLIProxyAPI 错误分类码。 */
    private String code;
    /** 是否可重试（0否 1是）。 */
    private Boolean retryable;
    /** 凭证状态（auth_status.status），常用筛选条件的扁平列。 */
    private String authStatus;
    /** 是否已被禁用（0否 1是）。 */
    private Boolean authDisabled;
    /** 是否处于不可用冷却（0否 1是）。 */
    private Boolean authUnavailable;
    /** 凭证预计恢复时间。 */
    private Date authNextRetryAt;
    /** 配额是否超限（0否 1是）。 */
    private Boolean quotaExceeded;
    /** 配额超限原因。 */
    private String quotaReason;
    /** 完整凭证状态快照 JSON（含模型级状态与配额恢复时间）。 */
    private String authStatusSnapshot;
    private Date createTime;

    public Long getFailureId() { return failureId; }
    public void setFailureId(Long failureId) { this.failureId = failureId; }
    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }
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
    public String getAuthStatus() { return authStatus; }
    public void setAuthStatus(String authStatus) { this.authStatus = authStatus; }
    public Boolean getAuthDisabled() { return authDisabled; }
    public void setAuthDisabled(Boolean authDisabled) { this.authDisabled = authDisabled; }
    public Boolean getAuthUnavailable() { return authUnavailable; }
    public void setAuthUnavailable(Boolean authUnavailable) { this.authUnavailable = authUnavailable; }
    public Date getAuthNextRetryAt() { return authNextRetryAt; }
    public void setAuthNextRetryAt(Date authNextRetryAt) { this.authNextRetryAt = authNextRetryAt; }
    public Boolean getQuotaExceeded() { return quotaExceeded; }
    public void setQuotaExceeded(Boolean quotaExceeded) { this.quotaExceeded = quotaExceeded; }
    public String getQuotaReason() { return quotaReason; }
    public void setQuotaReason(String quotaReason) { this.quotaReason = quotaReason; }
    public void setAuthStatusSnapshot(String authStatusSnapshot) { this.authStatusSnapshot = authStatusSnapshot; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    /** 返回 JSON 对象而不是 JSON 字符串，保持 CLIProxyAPI 的数据形状。 */
    @JsonRawValue
    public String getAuthStatusSnapshot()
    {
        return authStatusSnapshot == null ? "{}" : authStatusSnapshot;
    }
}
