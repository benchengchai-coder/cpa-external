import request from "@/utils/request";
import type { AiWalletInfo, AiRechargeRecord, AjaxResult, TableDataInfo } from "@/types";

export interface PendingAlipayOrder {
  outTradeNo: string;
  amount: number | string;
  status: string;
  createTime?: string;
  expireTime?: string;
  expireTimestamp?: number;
  remainingSeconds: number;
  payForm?: string;
  created?: boolean;
}

export interface OnlineRechargeStatus {
  enabled: boolean;
  available: boolean;
}

export interface AiUserConcurrencyUpdateResult {
  userId: number;
  aiConcurrencyLimit: number;
}

/** 获取当前用户钱包信息 */
export function getWalletInfo(): Promise<AjaxResult<AiWalletInfo>> {
  return request({ url: "/aigate/wallet/info", method: "get" });
}

/** 兑换充值码 */
export function redeemCode(redemptionKey: string): Promise<AjaxResult> {
  return request({ url: "/aigate/wallet/redeem", method: "post", data: { redemptionKey } });
}

/** 查询充值兑换记录 */
export function listRechargeRecord(query: Record<string, any>): Promise<TableDataInfo<AiRechargeRecord[]>> {
  return request({ url: "/aigate/wallet/billing/list", method: "get", params: query });
}

/** 查询在线充值业务状态 */
export function getOnlineRechargeStatus(): Promise<AjaxResult<OnlineRechargeStatus>> {
  return request({ url: "/aigate/pay/online-recharge/status", method: "get" });
}

// ==================== 管理员接口 ====================

/** 管理员 - 获取指定用户钱包信息 */
export function adminGetWalletInfo(userId: number): Promise<AjaxResult<AiWalletInfo>> {
  return request({ url: `/aigate/wallet/admin/info/${userId}`, method: "get" });
}

/** 管理员 - 调整用户余额 */
export function adminAdjustBalance(data: { userId: number; type: string; amount: number }): Promise<AjaxResult> {
  return request({ url: "/aigate/wallet/admin/adjust", method: "post", data });
}

/** 管理员 - 调整用户AI并发上限 */
export function adminUpdateConcurrency(data: {
  userId: number;
  aiConcurrencyLimit: number;
}): Promise<AjaxResult<AiUserConcurrencyUpdateResult>> {
  return request({ url: "/aigate/wallet/admin/concurrency", method: "put", data });
}

/** 管理员 - 查询指定用户充值记录 */
export function adminListRechargeRecord(userId: number, query: Record<string, any>): Promise<TableDataInfo<AiRechargeRecord[]>> {
  return request({ url: `/aigate/wallet/admin/recharge/list/${userId}`, method: "get", params: query });
}

/** 创建支付宝充值订单，返回商户订单号 + 支付表单 HTML（前端写入新窗口跳转收银台） */
export function createAlipayOrder(amount: number): Promise<AjaxResult<PendingAlipayOrder>> {
  return request({ url: "/aigate/pay/alipay/create", method: "post", data: { amount } });
}

/** 查询当前用户待支付的支付宝订单 */
export function getPendingAlipayOrder(): Promise<AjaxResult<PendingAlipayOrder | null>> {
  return request({ url: "/aigate/pay/alipay/pending", method: "get" });
}

/** 重新打开待支付订单的支付宝付款页面 */
export function reopenAlipayOrder(outTradeNo: string): Promise<AjaxResult<PendingAlipayOrder>> {
  return request({ url: `/aigate/pay/alipay/reopen/${outTradeNo}`, method: "post" });
}

/** 取消待支付的支付宝订单 */
export function cancelAlipayOrder(outTradeNo: string): Promise<AjaxResult> {
  return request({ url: `/aigate/pay/alipay/cancel/${outTradeNo}`, method: "post" });
}

/** 查询订单支付状态（支付结果页轮询用） */
export function queryOrderStatus(outTradeNo: string): Promise<AjaxResult<{ status: string; balance?: number; amount?: number }>> {
  return request({ url: `/aigate/pay/order/status/${outTradeNo}`, method: "get" });
}
