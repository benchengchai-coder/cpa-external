package com.ruoyi.cpaexternal.dashboard.domain.vo;

import java.math.BigDecimal;

/**
 * 管理员仪表盘概览数据。
 */
public class DashboardOverviewVO
{
    /** 平台总数（原渠道概念，ai_channel 已随中继代码移除） */
    private int channelCount;

    /** 模型总数 */
    private int modelCount;

    /** 用户总数 */
    private int userCount;

    /** 今日新增用户数 */
    private int todayNewUsers;

    /** API Key 总数 */
    private int apiKeyCount;

    /** 今日请求数 */
    private int todayRequests;

    /** 最近一分钟请求数（RPM） */
    private int requestsPerMinute;

    /** 今日 Token 消耗总量 */
    private long todayTokens;

    /** 历史 Token 消耗总量 */
    private long totalTokens;

    /** 今日计算费用 */
    private BigDecimal todayCost;

    /** 今日钱包与订阅实扣金额 */
    private BigDecimal todayChargedAmount;

    /** 尚未人工处置的结算失败任务数量 */
    private int failedSettlementCount;

    /** 最近7天成功调用平均响应时间（毫秒） */
    private BigDecimal averageResponseTime;

    /** 最近7天成功流式调用平均首 Token 响应时间（毫秒） */
    private BigDecimal averageFirstTokenTime;

    public int getChannelCount()
    {
        return channelCount;
    }

    public void setChannelCount(int channelCount)
    {
        this.channelCount = channelCount;
    }

    public int getModelCount()
    {
        return modelCount;
    }

    public void setModelCount(int modelCount)
    {
        this.modelCount = modelCount;
    }

    public int getUserCount()
    {
        return userCount;
    }

    public void setUserCount(int userCount)
    {
        this.userCount = userCount;
    }

    public int getTodayNewUsers()
    {
        return todayNewUsers;
    }

    public void setTodayNewUsers(int todayNewUsers)
    {
        this.todayNewUsers = todayNewUsers;
    }

    public int getApiKeyCount()
    {
        return apiKeyCount;
    }

    public void setApiKeyCount(int apiKeyCount)
    {
        this.apiKeyCount = apiKeyCount;
    }

    public int getTodayRequests()
    {
        return todayRequests;
    }

    public void setTodayRequests(int todayRequests)
    {
        this.todayRequests = todayRequests;
    }

    public int getRequestsPerMinute()
    {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute)
    {
        this.requestsPerMinute = requestsPerMinute;
    }

    public long getTodayTokens()
    {
        return todayTokens;
    }

    public void setTodayTokens(long todayTokens)
    {
        this.todayTokens = todayTokens;
    }

    public long getTotalTokens()
    {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens)
    {
        this.totalTokens = totalTokens;
    }

    public BigDecimal getTodayCost()
    {
        return todayCost;
    }

    public void setTodayCost(BigDecimal todayCost)
    {
        this.todayCost = todayCost;
    }

    public BigDecimal getTodayChargedAmount()
    {
        return todayChargedAmount;
    }

    public void setTodayChargedAmount(BigDecimal todayChargedAmount)
    {
        this.todayChargedAmount = todayChargedAmount;
    }

    public int getFailedSettlementCount()
    {
        return failedSettlementCount;
    }

    public void setFailedSettlementCount(int failedSettlementCount)
    {
        this.failedSettlementCount = failedSettlementCount;
    }

    public BigDecimal getAverageResponseTime()
    {
        return averageResponseTime;
    }

    public void setAverageResponseTime(BigDecimal averageResponseTime)
    {
        this.averageResponseTime = averageResponseTime;
    }

    public BigDecimal getAverageFirstTokenTime()
    {
        return averageFirstTokenTime;
    }

    public void setAverageFirstTokenTime(BigDecimal averageFirstTokenTime)
    {
        this.averageFirstTokenTime = averageFirstTokenTime;
    }
}
