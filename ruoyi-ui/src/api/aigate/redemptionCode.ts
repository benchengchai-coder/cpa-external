import request from "@/utils/request";
import type { AiRedemptionCode, AiRedemptionCodeQueryParams, AjaxResult, TableDataInfo } from "@/types";

export function listAiRedemptionCode(query: AiRedemptionCodeQueryParams): Promise<TableDataInfo<AiRedemptionCode[]>> {
  return request({
    url: "/aigate/redemptionCode/list",
    method: "get",
    params: query
  });
}

export function getAiRedemptionCode(codeId: number): Promise<AjaxResult<AiRedemptionCode>> {
  return request({
    url: "/aigate/redemptionCode/" + codeId,
    method: "get"
  });
}

export function addAiRedemptionCode(data: AiRedemptionCode): Promise<AjaxResult> {
  return request({
    url: "/aigate/redemptionCode",
    method: "post",
    data
  });
}

export function batchAddAiRedemptionCode(data: AiRedemptionCode): Promise<AjaxResult> {
  return request({
    url: "/aigate/redemptionCode/batch",
    method: "post",
    data
  });
}

export function updateAiRedemptionCode(data: AiRedemptionCode): Promise<AjaxResult> {
  return request({
    url: "/aigate/redemptionCode",
    method: "put",
    data
  });
}

export function delAiRedemptionCode(codeId: number | number[]): Promise<AjaxResult> {
  return request({
    url: "/aigate/redemptionCode/" + codeId,
    method: "delete"
  });
}
