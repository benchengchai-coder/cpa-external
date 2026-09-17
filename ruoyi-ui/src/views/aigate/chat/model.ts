import type { AiModel } from "@/types";

export const DEFAULT_CHAT_MODEL = "gpt-5.6-terra";

export function resolveDefaultChatModel(models: AiModel[]): string {
  const availableNames = models
    .map((model) => model.modelName)
    .filter((modelName): modelName is string => !!modelName);
  return availableNames.includes(DEFAULT_CHAT_MODEL) ? DEFAULT_CHAT_MODEL : availableNames[0] || "";
}
