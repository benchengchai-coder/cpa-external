package com.ruoyi.cpaexternal.dashboard.domain.vo;

import java.util.List;

/**
 * 用户实扣排行分页数据。
 */
public class DashboardUserRankPageVO
{
    /** 总条数 */
    private Long total;

    /** 当前页数据 */
    private List<DashboardUserRankVO> rows;

    public Long getTotal()
    {
        return total;
    }

    public void setTotal(Long total)
    {
        this.total = total;
    }

    public List<DashboardUserRankVO> getRows()
    {
        return rows;
    }

    public void setRows(List<DashboardUserRankVO> rows)
    {
        this.rows = rows;
    }
}
