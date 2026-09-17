import request from "@/utils/request";
import type { AiPlatformRecord, AiPlatformRecordQueryParams, AjaxResult, TableDataInfo } from "@/types";

export function listAiPlatform(query: AiPlatformRecordQueryParams): Promise<TableDataInfo<AiPlatformRecord[]>> {
  return request({
    url: "/aigate/platform/list",
    method: "get",
    params: query,
  });
}

export function getAiPlatform(platformId: number): Promise<AjaxResult<AiPlatformRecord>> {
  return request({
    url: "/aigate/platform/" + platformId,
    method: "get",
  });
}

export function addAiPlatform(data: AiPlatformRecord): Promise<AjaxResult> {
  return request({
    url: "/aigate/platform",
    method: "post",
    data,
  });
}

export function updateAiPlatform(data: AiPlatformRecord): Promise<AjaxResult> {
  return request({
    url: "/aigate/platform",
    method: "put",
    data,
  });
}

export function delAiPlatform(platformId: number | number[]): Promise<AjaxResult> {
  return request({
    url: "/aigate/platform/" + platformId,
    method: "delete",
  });
}
