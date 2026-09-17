import request from "@/utils/request";
import type {
  AiApiKey,
  AiApiKeyQueryParams,
  AiApiKeySecret,
  AiApiKeySyncStatus,
  AjaxResult,
  TableDataInfo
} from "@/types";

export function listAiApiKey(query: AiApiKeyQueryParams): Promise<TableDataInfo<AiApiKey[]>> {
  return request({
    url: "/aigate/api-key/list",
    method: "get",
    params: query
  });
}

export function getAiApiKey(keyId: number): Promise<AjaxResult<AiApiKey>> {
  return request({
    url: "/aigate/api-key/" + keyId,
    method: "get"
  });
}

export function getAiApiKeySecret(keyId: number): Promise<AjaxResult<AiApiKeySecret>> {
  return request({
    url: "/aigate/api-key/" + keyId + "/secret",
    method: "get"
  });
}

export function getAdminUserApiKeySecrets(userId: number): Promise<AjaxResult<AiApiKeySecret[]>> {
  return request({
    url: "/aigate/api-key/admin/users/" + userId + "/secrets",
    method: "get"
  });
}

/** 查询当前登录用户的唯一 API Key（含明文） */
export function getSelfAiApiKey(): Promise<AjaxResult<AiApiKey>> {
  return request({
    url: "/aigate/api-key/self",
    method: "get"
  });
}

/** 更换当前登录用户的唯一 API Key，旧密钥立即失效 */
export function rotateSelfAiApiKey(): Promise<AjaxResult<AiApiKey>> {
  return request({
    url: "/aigate/api-key/self/rotate",
    method: "put"
  });
}

export function addAiApiKey(data: AiApiKey): Promise<AjaxResult<AiApiKey>> {
  return request({
    url: "/aigate/api-key",
    method: "post",
    data
  });
}

export function updateAiApiKey(data: AiApiKey): Promise<AjaxResult> {
  return request({
    url: "/aigate/api-key",
    method: "put",
    data
  });
}

/** 管理员自定义指定密钥的明文值，旧密钥立即失效 */
export function customizeAiApiKeySecret(keyId: number, apiKey: string): Promise<AjaxResult<AiApiKey>> {
  return request({
    url: "/aigate/api-key/" + keyId + "/secret",
    method: "put",
    data: { apiKey }
  });
}

export function delAiApiKey(keyId: number | number[]): Promise<AjaxResult> {
  return request({
    url: "/aigate/api-key/" + keyId,
    method: "delete"
  });
}

/** 管理员手动同步指定 Key 到 CLIProxyAPI 使其生效（启用推送、停用移除） */
export function syncAiApiKeyToCpa(keyId: number): Promise<AjaxResult> {
  return request({
    url: "/aigate/api-key/" + keyId + "/sync",
    method: "post"
  });
}

/** 实时对比平台与 CLIProxyAPI 的 api-keys，返回分类差异明细（仅管理员） */
export function getCpaApiKeySyncStatus(): Promise<AjaxResult<AiApiKeySyncStatus>> {
  return request({
    url: "/aigate/api-key/cpa-sync-status",
    method: "get"
  });
}
