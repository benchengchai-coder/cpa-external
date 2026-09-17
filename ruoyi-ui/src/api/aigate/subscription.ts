import request from "@/utils/request";
import type {
  AiBillingPreference,
  AiSubscriptionPlan,
  AiSubscriptionPlanQueryParams,
  AiSubscriptionRecord,
  AiSubscriptionSelf,
  AiUserSubscription,
  AjaxResult,
  TableDataInfo
} from "@/types";

export function listSubscriptionPlan(query: AiSubscriptionPlanQueryParams): Promise<TableDataInfo<AiSubscriptionPlan[]>> {
  return request({ url: "/aigate/subscription/plan/list", method: "get", params: query });
}

export function getSubscriptionPlan(planId: number): Promise<AjaxResult<AiSubscriptionPlan>> {
  return request({ url: `/aigate/subscription/plan/${planId}`, method: "get" });
}

export function addSubscriptionPlan(data: AiSubscriptionPlan): Promise<AjaxResult> {
  return request({ url: "/aigate/subscription/plan", method: "post", data });
}

export function updateSubscriptionPlan(data: AiSubscriptionPlan): Promise<AjaxResult> {
  return request({ url: "/aigate/subscription/plan", method: "put", data });
}

export function delSubscriptionPlan(planIds: number | number[]): Promise<AjaxResult> {
  return request({ url: `/aigate/subscription/plan/${planIds}`, method: "delete" });
}

export function changeSubscriptionPlanStatus(planId: number, status: string): Promise<AjaxResult> {
  return request({ url: "/aigate/subscription/plan/changeStatus", method: "put", data: { planId, status } });
}

export function listAdminUserSubscriptions(userId: number, query: Record<string, any>): Promise<TableDataInfo<AiUserSubscription[]>> {
  return request({ url: `/aigate/subscription/admin/users/${userId}/subscriptions`, method: "get", params: query });
}

export function grantSubscription(userId: number, data: Record<string, any>): Promise<AjaxResult<AiUserSubscription>> {
  return request({ url: `/aigate/subscription/admin/users/${userId}/grant`, method: "post", data });
}

export function cancelSubscription(subscriptionId: number): Promise<AjaxResult> {
  return request({ url: `/aigate/subscription/admin/user-subscriptions/${subscriptionId}/cancel`, method: "put" });
}

export function delUserSubscriptions(subscriptionIds: number | number[]): Promise<AjaxResult> {
  return request({ url: `/aigate/subscription/admin/user-subscriptions/${subscriptionIds}`, method: "delete" });
}

export function listUserPlans(): Promise<AjaxResult<AiSubscriptionPlan[]>> {
  return request({ url: "/aigate/subscription/plans", method: "get" });
}

export function getSelfSubscription(): Promise<AjaxResult<AiSubscriptionSelf>> {
  return request({ url: "/aigate/subscription/self", method: "get" });
}

export function updateBillingPreference(billingPreference: AiBillingPreference): Promise<AjaxResult> {
  return request({ url: "/aigate/subscription/self/preference", method: "put", data: { billingPreference } });
}

export function balancePurchaseSubscription(planId: number): Promise<AjaxResult<AiUserSubscription>> {
  return request({ url: "/aigate/subscription/balance/purchase", method: "post", data: { planId } });
}

export function listSubscriptionRecord(query: Record<string, any>): Promise<TableDataInfo<AiSubscriptionRecord[]>> {
  return request({ url: "/aigate/subscription/record/list", method: "get", params: query });
}

export function getSubscriptionConfig(): Promise<AjaxResult<{ balancePurchaseEnabled: string }>> {
  return request({ url: "/aigate/subscription/config", method: "get" });
}

export function updateSubscriptionConfig(balancePurchaseEnabled: string): Promise<AjaxResult> {
  return request({ url: "/aigate/subscription/config", method: "put", data: { balancePurchaseEnabled } });
}
