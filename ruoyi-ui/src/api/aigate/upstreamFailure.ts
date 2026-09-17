import request from "@/utils/request";
import type { AjaxResult, TableDataInfo, UpstreamFailureQueryParams, UpstreamFailureRecord } from "@/types";

export function listUpstreamFailure(query: UpstreamFailureQueryParams): Promise<TableDataInfo<UpstreamFailureRecord[]>> {
  return request({
    url: "/aigate/upstreamFailure/list",
    method: "get",
    params: query
  });
}

export function getUpstreamFailure(failureId: number): Promise<AjaxResult<UpstreamFailureRecord>> {
  return request({
    url: "/aigate/upstreamFailure/" + failureId,
    method: "get"
  });
}

export function delUpstreamFailure(failureIds: number | number[]): Promise<AjaxResult<void>> {
  return request({
    url: "/aigate/upstreamFailure/" + failureIds,
    method: "delete"
  });
}
