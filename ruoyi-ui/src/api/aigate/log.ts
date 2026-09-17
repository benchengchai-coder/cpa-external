import request from "@/utils/request";
import type { AiLog, AiLogQueryParams, AiLogUserSummary, AjaxResult, TableDataInfo } from "@/types";

export function listAiLog(query: AiLogQueryParams): Promise<TableDataInfo<AiLog[]>> {
  return request({
    url: "/aigate/log/list",
    method: "get",
    params: query
  });
}

export function getAiLog(logId: number): Promise<AjaxResult<AiLog>> {
  return request({
    url: "/aigate/log/" + logId,
    method: "get"
  });
}

/** 查询调用日志关联用户的关键信息 */
export function getAiLogUserSummary(userId: number): Promise<AjaxResult<AiLogUserSummary>> {
  return request({
    url: `/aigate/log/users/${userId}/summary`,
    method: "get"
  });
}

export function delAiLog(logId: number | number[]): Promise<AjaxResult> {
  return request({
    url: "/aigate/log/" + logId,
    method: "delete"
  });
}
