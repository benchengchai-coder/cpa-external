import request from "@/utils/request";
import type { AiModel, AiModelQueryParams, AjaxResult, TableDataInfo } from "@/types";

export function listAiModel(query: AiModelQueryParams): Promise<TableDataInfo<AiModel[]>> {
  return request({
    url: "/aigate/model/list",
    method: "get",
    params: query
  });
}

export function getAiModel(modelId: number): Promise<AjaxResult<AiModel>> {
  return request({
    url: "/aigate/model/" + modelId,
    method: "get"
  });
}

export function addAiModel(data: AiModel): Promise<AjaxResult> {
  return request({
    url: "/aigate/model",
    method: "post",
    data
  });
}

export function updateAiModel(data: AiModel): Promise<AjaxResult> {
  return request({
    url: "/aigate/model",
    method: "put",
    data
  });
}

export function delAiModel(modelId: number | number[]): Promise<AjaxResult> {
  return request({
    url: "/aigate/model/" + modelId,
    method: "delete"
  });
}

export function optionselectAiModel(): Promise<AjaxResult<AiModel[]>> {
  return request({
    url: "/aigate/model/optionselect",
    method: "get"
  });
}
