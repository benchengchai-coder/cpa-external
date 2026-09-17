import request from "@/utils/request";
import type { AiLog, AiLogQueryParams, AjaxResult, TableDataInfo } from "@/types";

export function listUsageLog(query: AiLogQueryParams): Promise<TableDataInfo<AiLog[]>> {
  return request({
    url: "/aigate/log/usage-list",
    method: "get",
    params: query
  });
}

export function getUsageLog(logId: number): Promise<AjaxResult<AiLog>> {
  return request({
    url: "/aigate/log/usage/" + logId,
    method: "get"
  });
}
