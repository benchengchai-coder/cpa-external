package com.ruoyi.cpaexternal.log.domain.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;

/** 个人使用记录列表展示对象，仅返回列表字段。 */
public class CpaAiLogUsageListVO
{
    private Long logId;
    private Date requestTime;
    private String relayMode;
    private String xForwardedFor;
    private String modelName;
    private String reasoningEffort;
    private BigDecimal billingMultiplier;
    private Integer isStream;
    private String status;
    private Integer ttft;
    private Integer duration;
    private Integer promptTokens;
    private Integer cacheReadTokens;
    private Integer completionTokens;
    private Integer reasoningOutputTokens;
    private Integer totalTokens;
    private BigDecimal cost;

    public static CpaAiLogUsageListVO from(CpaAiLog log)
    {
        CpaAiLogUsageListVO vo = new CpaAiLogUsageListVO();
        vo.logId = log.getLogId();
        vo.requestTime = log.getRequestTime();
        vo.relayMode = log.getRelayMode();
        vo.xForwardedFor = log.getXForwardedFor() == null ? log.getClientIp() : log.getXForwardedFor();
        vo.modelName = log.getModelName();
        vo.reasoningEffort = log.getReasoningEffort();
        vo.billingMultiplier = log.getBillingMultiplier();
        vo.isStream = log.getIsStream();
        vo.status = log.getStatus();
        vo.ttft = log.getTtft();
        vo.duration = log.getDuration();
        vo.promptTokens = log.getPromptTokens();
        vo.cacheReadTokens = log.getCacheReadTokens();
        vo.completionTokens = log.getCompletionTokens();
        vo.reasoningOutputTokens = log.getReasoningOutputTokens();
        vo.totalTokens = log.getTotalTokens();
        vo.cost = log.getCost();
        return vo;
    }

    public Long getLogId() { return logId; }
    public Date getRequestTime() { return requestTime; }
    public String getRelayMode() { return relayMode; }
    public String getXForwardedFor() { return xForwardedFor; }
    public String getModelName() { return modelName; }
    public String getReasoningEffort() { return reasoningEffort; }
    public BigDecimal getBillingMultiplier() { return billingMultiplier; }
    public Integer getIsStream() { return isStream; }
    public String getStatus() { return status; }
    public Integer getTtft() { return ttft; }
    public Integer getDuration() { return duration; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getCacheReadTokens() { return cacheReadTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public Integer getReasoningOutputTokens() { return reasoningOutputTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public BigDecimal getCost() { return cost; }
}
