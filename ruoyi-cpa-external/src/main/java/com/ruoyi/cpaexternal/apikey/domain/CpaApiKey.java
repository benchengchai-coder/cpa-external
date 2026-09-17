package com.ruoyi.cpaexternal.apikey.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * API Key 管理对象。
 */
public class CpaApiKey extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long keyId;
    private Long userId;
    private String keyName;
    private String apiKey;
    private String status;
    private Date accessedTime;

    public Long getKeyId() { return keyId; }
    public void setKeyId(Long keyId) { this.keyId = keyId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getAccessedTime() { return accessedTime; }
    public void setAccessedTime(Date accessedTime) { this.accessedTime = accessedTime; }
}
