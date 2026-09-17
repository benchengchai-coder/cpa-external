import request from "@/utils/request";
import type {
  AiInviteAffiliate,
  AiInviteAffiliateQuery,
  AiInviteRebateRecord,
  AiInviteRebateRecordQuery,
  InviteClaimResult,
  InviteAdminConfig,
  InviteInfoVO,
  InviteOverviewVO,
  AjaxResult,
  TableDataInfo
} from "@/types";

// ==================== 用户接口 ====================

/** 获取当前用户邀请返利信息 */
export function getInviteInfo(): Promise<AjaxResult<InviteInfoVO>> {
  return request({ url: "/aigate/invite/info", method: "get" });
}

/** 领取全部待领返利 */
export function claimInviteRebate(): Promise<AjaxResult<InviteClaimResult>> {
  return request({ url: "/aigate/invite/claim", method: "post" });
}

// ==================== 管理员接口 ====================

/** 管理员 - 邀请关系列表 */
export function listInviteAffiliate(query: AiInviteAffiliateQuery): Promise<TableDataInfo<AiInviteAffiliate[]>> {
  return request({ url: "/aigate/invite/admin/affiliate/list", method: "get", params: query });
}

/** 管理员 - 返利流水列表 */
export function listInviteRebate(query: AiInviteRebateRecordQuery): Promise<TableDataInfo<AiInviteRebateRecord[]>> {
  return request({ url: "/aigate/invite/admin/rebate/list", method: "get", params: query });
}

/** 管理员 - 单用户概览 */
export function getInviteOverview(userId: number): Promise<AjaxResult<InviteOverviewVO>> {
  return request({ url: `/aigate/invite/admin/overview/${userId}`, method: "get" });
}

/** 管理员 - 设置专属返利比例（rebateRate 为空表示清除，沿用全局） */
export function setInviteRebateRate(userId: number, rebateRate: number | null): Promise<AjaxResult> {
  return request({ url: "/aigate/invite/admin/rate", method: "put", data: { userId, rebateRate } });
}

/** 管理员 - 重置用户邀请码 */
export function resetInviteCode(userId: number): Promise<AjaxResult<{ inviteCode: string }>> {
  return request({ url: `/aigate/invite/admin/resetCode/${userId}`, method: "put" });
}

/** 管理员 - 查询邀请返利参数配置 */
export function getInviteConfig(): Promise<AjaxResult<InviteAdminConfig>> {
  return request({ url: "/aigate/invite/admin/config", method: "get" });
}

/** 管理员 - 更新邀请返利参数配置 */
export function updateInviteConfig(data: InviteAdminConfig): Promise<AjaxResult<InviteAdminConfig>> {
  return request({ url: "/aigate/invite/admin/config", method: "put", data });
}
