package com.ruoyi.web.controller.aigate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardUserRankPageVO;
import com.ruoyi.cpaexternal.dashboard.service.ICpaDashboardService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 首页仪表盘统计 Controller。
 */
@RestController
@RequestMapping("/aigate/dashboard")
public class CpaDashboardController extends BaseController
{
    @Autowired
    private ICpaDashboardService dashboardService;

    // ==================== 管理员接口 ====================

    /**
     * 管理员 - 概览数据
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/admin/overview")
    public AjaxResult adminOverview()
    {
        return success(dashboardService.getAdminOverview());
    }

    /**
     * 管理员 - 调用趋势
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/admin/trend")
    public AjaxResult adminTrend(@RequestParam(defaultValue = "7") String range)
    {
        return success(dashboardService.getRecentTrend(range));
    }

    /**
     * 管理员 - 用户实扣排行
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/admin/user-rank")
    public TableDataInfo adminUserRank(@RequestParam(defaultValue = "all") String range,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        DashboardUserRankPageVO page = dashboardService.getUserRank(range, pageNum, pageSize);
        TableDataInfo response = new TableDataInfo();
        response.setCode(HttpStatus.SUCCESS);
        response.setMsg("查询成功");
        response.setRows(page.getRows());
        response.setTotal(page.getTotal());
        return response;
    }

    // ==================== 用户接口 ====================

    /**
     * 用户 - 个人首页数据
     */
    @GetMapping("/user")
    public AjaxResult userDashboard()
    {
        return success(dashboardService.getUserDashboard(getUserId()));
    }

    /**
     * 用户 - 个人调用趋势
     */
    @GetMapping("/user/trend")
    public AjaxResult userTrend(@RequestParam(defaultValue = "7") String range)
    {
        return success(dashboardService.getUserRecentTrend(getUserId(), range));
    }
}
