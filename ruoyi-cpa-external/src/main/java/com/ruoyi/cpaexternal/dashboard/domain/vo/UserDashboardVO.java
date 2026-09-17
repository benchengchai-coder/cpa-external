package com.ruoyi.cpaexternal.dashboard.domain.vo;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;

/**
 * 用户首页仪表盘数据。
 */
public class UserDashboardVO
{
    /** 账户余额 */
    private BigDecimal balance;

    /** 冻结余额 */
    private BigDecimal frozenBalance;

    /** 可用余额 */
    private BigDecimal availableBalance;

    /** 累计应计金额 */
    private BigDecimal usedBalance;

    /** 钱包与订阅累计实扣金额 */
    private BigDecimal totalChargedAmount;

    /** 累计未覆盖金额 */
    private BigDecimal totalUncoveredAmount;

    /** 请求次数 */
    private int requestCount;

    /** 今日请求次数 */
    private int todayRequests;

    /** Token 总数 */
    private long totalTokens;

    /** API Key 数量 */
    private int apiKeyCount;

    /** 最近7天成功调用平均响应时间（毫秒） */
    private BigDecimal averageResponseTime;

    /** 最近7天成功流式调用平均首 Token 响应时间（毫秒） */
    private BigDecimal averageFirstTokenTime;

    /** 最近调用记录（保留字段兼容前端类型；日志含密钥等敏感快照，不再下发，恒为空列表） */
    private List<CpaAiLog> recentLogs;

    public BigDecimal getBalance()
    {
        return balance;
    }

    public void setBalance(BigDecimal balance)
    {
        this.balance = balance;
    }

    public BigDecimal getFrozenBalance()
    {
        return frozenBalance;
    }

    public void setFrozenBalance(BigDecimal frozenBalance)
    {
        this.frozenBalance = frozenBalance;
    }

    public BigDecimal getAvailableBalance()
    {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance)
    {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getUsedBalance()
    {
        return usedBalance;
    }

    public void setUsedBalance(BigDecimal usedBalance)
    {
        this.usedBalance = usedBalance;
    }

    public BigDecimal getTotalChargedAmount()
    {
        return totalChargedAmount;
    }

    public void setTotalChargedAmount(BigDecimal totalChargedAmount)
    {
        this.totalChargedAmount = totalChargedAmount;
    }

    public BigDecimal getTotalUncoveredAmount()
    {
        return totalUncoveredAmount;
    }

    public void setTotalUncoveredAmount(BigDecimal totalUncoveredAmount)
    {
        this.totalUncoveredAmount = totalUncoveredAmount;
    }

    public int getRequestCount()
    {
        return requestCount;
    }

    public void setRequestCount(int requestCount)
    {
        this.requestCount = requestCount;
    }

    public int getTodayRequests()
    {
        return todayRequests;
    }

    public void setTodayRequests(int todayRequests)
    {
        this.todayRequests = todayRequests;
    }

    public long getTotalTokens()
    {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens)
    {
        this.totalTokens = totalTokens;
    }

    public int getApiKeyCount()
    {
        return apiKeyCount;
    }

    public void setApiKeyCount(int apiKeyCount)
    {
        this.apiKeyCount = apiKeyCount;
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

    public List<CpaAiLog> getRecentLogs()
    {
        return recentLogs;
    }

    public void setRecentLogs(List<CpaAiLog> recentLogs)
    {
        this.recentLogs = recentLogs;
    }
}
