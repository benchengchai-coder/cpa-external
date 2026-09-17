import request from "@/utils/request";
import { getToken } from "@/utils/auth";
import type { AiModel, AjaxResult } from "@/types";

/**
 * 获取可用模型列表
 */
export function listChatModels(): Promise<AjaxResult<AiModel[]>> {
  return request({
    url: "/aigate/chat/models",
    method: "get"
  });
}

/** 聊天消息 */
export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: number;
  streaming?: boolean;
  modelName?: string;
}

/** 聊天请求参数 */
export interface ChatCompletionParams {
  model: string;
  messages: Array<{ role: string; content: string }>;
  stream: boolean;
}

/**
 * SSE 流式聊天请求
 * 使用原生 fetch 而非 axios，因为：
 * 1. axios 不原生支持 ReadableStream
 * 2. axios 10s 超时会中断长连接
 */
export async function sendChatStream(
  params: ChatCompletionParams,
  signal?: AbortSignal
): Promise<Response> {
  const baseURL = import.meta.env.VITE_APP_BASE_API;
  const url = baseURL + "/aigate/chat/completions";
  const token = getToken();

  return fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`
    },
    body: JSON.stringify(params),
    signal
  });
}
