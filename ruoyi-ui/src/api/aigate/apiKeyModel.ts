import request from "@/utils/request";
import type { AiApiKeyModel, AiApiKeyModelQueryParams, AjaxResult, TableDataInfo } from "@/types";

export function listAiApiKeyModel(query: AiApiKeyModelQueryParams): Promise<TableDataInfo<AiApiKeyModel[]>> {
  return request({
    url: "/aigate/api-key-model/list",
    method: "get",
    params: query
  });
}

export function getAiApiKeyModel(id: number): Promise<AjaxResult<AiApiKeyModel>> {
  return request({
    url: "/aigate/api-key-model/" + id,
    method: "get"
  });
}

export function addAiApiKeyModel(data: AiApiKeyModel): Promise<AjaxResult> {
  return request({
    url: "/aigate/api-key-model",
    method: "post",
    data
  });
}

export function delAiApiKeyModel(id: number | number[]): Promise<AjaxResult> {
  return request({
    url: "/aigate/api-key-model/" + id,
    method: "delete"
  });
}
