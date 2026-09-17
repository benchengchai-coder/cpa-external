package com.ruoyi.cpaexternal.dashboard.service;

import java.util.List;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardOverviewVO;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardTrendVO;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardUserRankPageVO;
import com.ruoyi.cpaexternal.dashboard.domain.vo.UserDashboardVO;

/** 首页仪表盘统计服务。 */
public interface ICpaDashboardService
{
    /** 管理员概览数据。 */
    DashboardOverviewVO getAdminOverview();

    /** 全站调用趋势（range：today / 7 / 30，非法值回落 7）。 */
    List<DashboardTrendVO> getRecentTrend(String range);

    /** 用户实扣排行分页（range：today / 7 / 30 / 90 / all，非法值回落 all）。 */
    DashboardUserRankPageVO getUserRank(String range, int pageNum, int pageSize);

    /** 用户个人首页数据。 */
    UserDashboardVO getUserDashboard(Long userId);

    /** 用户个人调用趋势（range：today / 7 / 30，非法值回落 7）。 */
    List<DashboardTrendVO> getUserRecentTrend(Long userId, String range);
}
