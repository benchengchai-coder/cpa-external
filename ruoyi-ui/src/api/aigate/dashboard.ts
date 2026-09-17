import request from "@/utils/request";
import type {
  AjaxResult,
  DashboardOverview,
  DashboardTrend,
  DashboardUserRank,
  DashboardTrendRange,
  DashboardRankRange,
  PageDomain,
  TableDataInfo,
  UserDashboard
} from "@/types";

/** 管理员 - 概览统计 */
export function getAdminOverview(): Promise<AjaxResult<DashboardOverview>> {
  return request({ url: "/aigate/dashboard/admin/overview", method: "get" });
}

/** 管理员 - 调用趋势 */
export function getAdminTrend(range: DashboardTrendRange = "7"): Promise<AjaxResult<DashboardTrend[]>> {
  return request({ url: "/aigate/dashboard/admin/trend", method: "get", params: { range } });
}

/** 管理员 - 用户实扣排行 */
export function getAdminUserRank(
  query: Pick<PageDomain, "pageNum" | "pageSize"> & { range: DashboardRankRange }
): Promise<TableDataInfo<DashboardUserRank>> {
  return request({ url: "/aigate/dashboard/admin/user-rank", method: "get", params: query });
}

/** 用户 - 个人首页数据 */
export function getUserDashboard(): Promise<AjaxResult<UserDashboard>> {
  return request({ url: "/aigate/dashboard/user", method: "get" });
}

/** 用户 - 个人调用趋势 */
export function getUserTrend(range: DashboardTrendRange = "7"): Promise<AjaxResult<DashboardTrend[]>> {
  return request({ url: "/aigate/dashboard/user/trend", method: "get", params: { range } });
}
