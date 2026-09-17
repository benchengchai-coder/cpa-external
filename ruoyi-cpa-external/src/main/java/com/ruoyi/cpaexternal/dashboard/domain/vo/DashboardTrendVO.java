package com.ruoyi.cpaexternal.dashboard.domain.vo;

import java.math.BigDecimal;

/**
 * 调用趋势数据。
 */
public class DashboardTrendVO
{
    /** 日期或小时标签 */
    private String date;

    /** 请求数 */
    private long requestCount;

    /** Token 消耗总量 */
    private long totalTokens;

    /** 计算费用 */
    private BigDecimal cost;

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
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

    public BigDecimal getCost()
    {
        return cost;
    }

    public void setCost(BigDecimal cost)
    {
        this.cost = cost;
    }
}
