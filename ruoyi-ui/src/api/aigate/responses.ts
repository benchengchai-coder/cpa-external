import { getToken } from "@/utils/auth";

/** Responses 请求参数 */
export interface ResponsesParams {
  model: string;
  input: string;
  instructions?: string;
  stream: boolean;
  temperature?: number;
  top_p?: number;
  max_output_tokens?: number;
  tools?: unknown[];
  previous_response_id?: string;
}

/**
 * SSE 流式 Responses 请求。
 * 使用原生 fetch，与 Chat Completions 保持一致。
 */
export async function sendResponsesStream(
  params: ResponsesParams,
  signal?: AbortSignal
): Promise<Response> {
  const baseURL = import.meta.env.VITE_APP_BASE_API;
  const url = baseURL + "/aigate/chat/responses";
  const token = getToken();

  return fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(params),
    signal,
  });
}
