import request from "@/utils/request";
import type { AiModel, AiPlatformRecord, AjaxResult } from "@/types";

/** 公开：获取已启用的模型列表 */
export function publicModels(): Promise<AjaxResult<AiModel[]>> {
  return request({
    url: "/aigate/public/models",
    method: "get",
  });
}

/** 公开：获取已启用的平台列表 */
export function publicPlatforms(): Promise<AjaxResult<AiPlatformRecord[]>> {
  return request({
    url: "/aigate/public/platforms",
    method: "get",
  });
}

/** 公开：获取已启用的订阅套餐列表（套餐定价页用） */
export function publicPlans() {
  return request({
    url: "/aigate/public/plans",
    method: "get",
  });
}
