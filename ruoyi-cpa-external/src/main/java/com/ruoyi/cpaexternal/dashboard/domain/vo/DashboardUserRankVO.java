package com.ruoyi.cpaexternal.dashboard.domain.vo;

import java.math.BigDecimal;

/**
 * 用户实扣排行数据。
 */
public class DashboardUserRankVO
{
    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 调用次数 */
    private long requestCount;

    /** Token 消耗总量 */
    private long totalTokens;

    /** 钱包与订阅累计实扣金额 */
    private BigDecimal totalChargedAmount;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public long getRequestCount()
    {
        return requestCount;
    }

    public void setRequestCount(long requestCount)
    {
        this.requestCount = requestCount;
    }

    public long getTotalTokens()
    {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens)
    {
        this.totalTokens = totalTokens;
    }

    public BigDecimal getTotalChargedAmount()
    {
        return totalChargedAmount;
    }

    public void setTotalChargedAmount(BigDecimal totalChargedAmount)
    {
        this.totalChargedAmount = totalChargedAmount;
    }
}
