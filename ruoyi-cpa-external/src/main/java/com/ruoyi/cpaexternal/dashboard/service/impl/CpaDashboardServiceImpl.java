package com.ruoyi.cpaexternal.dashboard.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingRecordQueryService;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingSettlementService;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardOverviewVO;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardTrendVO;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardUserRankPageVO;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardUserRankVO;
import com.ruoyi.cpaexternal.dashboard.domain.vo.UserDashboardVO;
import com.ruoyi.cpaexternal.dashboard.mapper.CpaDashboardMapper;
import com.ruoyi.cpaexternal.dashboard.service.ICpaDashboardService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * 首页仪表盘统计 Service 实现。
 *
 * <p>数据口径：ai_log 直接聚合（CLIProxyAPI 用量日志落库表），
 * 实扣金额来自 ai_billing_record（success/partial），与账单金额汇总口径一致。</p>
 */
@Service
public class CpaDashboardServiceImpl implements ICpaDashboardService
{
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private CpaDashboardMapper dashboardMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ICpaBillingRecordQueryService billingRecordQueryService;

    @Autowired
    private ICpaBillingSettlementService billingSettlementService;

    @Override
    public DashboardOverviewVO getAdminOverview()
    {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setChannelCount(dashboardMapper.countPlatforms());
        vo.setModelCount(dashboardMapper.countModels());
        vo.setUserCount(dashboardMapper.countUsers());
        vo.setTodayNewUsers(dashboardMapper.countTodayNewUsers());
        vo.setApiKeyCount(dashboardMapper.countApiKeys());
        vo.setTodayRequests(dashboardMapper.countTodayRequests());
        vo.setRequestsPerMinute(dashboardMapper.countRequestsPerMinute());
        vo.setTodayTokens(dashboardMapper.sumTodayTokens());
        vo.setTotalTokens(dashboardMapper.sumTotalTokens());
        vo.setTodayCost(dashboardMapper.sumTodayCost());
        vo.setTodayChargedAmount(dashboardMapper.sumTodayChargedAmount());
        vo.setFailedSettlementCount(billingSettlementService.countFailedTasks());
        vo.setAverageResponseTime(dashboardMapper.avgRecentSuccessDuration());
        vo.setAverageFirstTokenTime(dashboardMapper.avgRecentSuccessFirstTokenTime());
        return vo;
    }

    @Override
    public List<DashboardTrendVO> getRecentTrend(String range)
    {
        return dashboardMapper.selectTrend(resolveTrendMode(range), resolveTrendStartDate(range), null);
    }

    @Override
    public List<DashboardTrendVO> getUserRecentTrend(Long userId, String range)
    {
        return dashboardMapper.selectTrend(resolveTrendMode(range), resolveTrendStartDate(range), userId);
    }

    @Override
    public DashboardUserRankPageVO getUserRank(String range, int pageNum, int pageSize)
    {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        long total = dashboardMapper.countUserRank(resolveRankStartDate(range));
        if (total <= 0L)
        {
            DashboardUserRankPageVO emptyPage = new DashboardUserRankPageVO();
            emptyPage.setTotal(0L);
            emptyPage.setRows(Collections.emptyList());
            return emptyPage;
        }

        long totalPages = (total + safePageSize - 1L) / safePageSize;
        safePageNum = (int) Math.min(safePageNum, totalPages);
        int offset = (int) Math.min((long) (safePageNum - 1) * safePageSize, Integer.MAX_VALUE);
        List<DashboardUserRankVO> rows = dashboardMapper.selectUserRank(
            resolveRankStartDate(range), offset, safePageSize);

        DashboardUserRankPageVO page = new DashboardUserRankPageVO();
        page.setTotal(total);
        page.setRows(rows);
        return page;
    }

    @Override
    public UserDashboardVO getUserDashboard(Long userId)
    {
        SysUser user = sysUserService.selectUserById(userId);
        UserDashboardVO vo = new UserDashboardVO();
        vo.setBalance(user.getBalance());
        BigDecimal frozenBalance = user.getFrozenBalance() == null ? BigDecimal.ZERO : user.getFrozenBalance();
        BigDecimal availableBalance = user.getBalance() == null
            ? BigDecimal.ZERO : user.getBalance().subtract(frozenBalance);
        vo.setFrozenBalance(frozenBalance);
        vo.setAvailableBalance(availableBalance.compareTo(BigDecimal.ZERO) > 0
            ? availableBalance : BigDecimal.ZERO);
        vo.setUsedBalance(user.getUsedBalance());
        CpaBillingAmountSummaryVO amountSummary = billingRecordQueryService.selectUserAmountSummary(userId);
        vo.setTotalChargedAmount(amountSummary.getTotalChargedAmount());
        vo.setTotalUncoveredAmount(amountSummary.getTotalUncoveredAmount());
        vo.setRequestCount(user.getRequestCount() == null ? 0 : user.getRequestCount());
        vo.setTodayRequests(dashboardMapper.countUserTodayRequests(userId));
        vo.setTotalTokens(dashboardMapper.sumUserTokens(userId));
        vo.setApiKeyCount(dashboardMapper.countUserApiKeys(userId));
        vo.setAverageResponseTime(dashboardMapper.avgRecentUserSuccessDuration(userId));
        vo.setAverageFirstTokenTime(dashboardMapper.avgRecentUserSuccessFirstTokenTime(userId));
        // 日志含密钥等敏感快照，不再下发最近记录；字段保留空列表兼容前端类型
        vo.setRecentLogs(Collections.emptyList());
        return vo;
    }

    /** 趋势聚合模式：today 按小时，其余按天。 */
    private String resolveTrendMode(String range)
    {
        return "today".equals(normalizeTrendRange(range)) ? "today" : "daily";
    }

    /** 趋势起始日期（含），today 模式返回当日（SQL 侧使用 curdate，此值不参与过滤）。 */
    private String resolveTrendStartDate(String range)
    {
        String normalized = normalizeTrendRange(range);
        LocalDate today = LocalDate.now();
        if ("today".equals(normalized))
        {
            return today.toString();
        }
        long days = Long.parseLong(normalized);
        return today.minusDays(days - 1L).toString();
    }

    /** 排行起始日期（含），all 范围返回 null 表示不设下界。 */
    private String resolveRankStartDate(String range)
    {
        String normalized = normalizeRankRange(range);
        LocalDate today = LocalDate.now();
        if ("all".equals(normalized))
        {
            return null;
        }
        if ("today".equals(normalized))
        {
            return today.toString();
        }
        long days = Long.parseLong(normalized);
        return today.minusDays(days - 1L).toString();
    }

    private String normalizeTrendRange(String range)
    {
        String normalized = StringUtils.trim(range);
        if ("today".equals(normalized) || "7".equals(normalized) || "30".equals(normalized))
        {
            return normalized;
        }
        return "7";
    }

    private String normalizeRankRange(String range)
    {
        String normalized = StringUtils.trim(range);
        if ("today".equals(normalized) || "7".equals(normalized) || "30".equals(normalized)
            || "90".equals(normalized) || "all".equals(normalized))
        {
            return normalized;
        }
        return "all";
    }
}
