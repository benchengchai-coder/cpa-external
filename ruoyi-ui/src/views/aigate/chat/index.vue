<template>
  <div class="ai-chat">
    <!-- 格式切换 Tab -->
    <div class="ai-chat__tabs">
      <el-tabs v-model="activeTab" class="ai-chat__tab-nav">
        <el-tab-pane label="Chat Completions" name="chat" />
        <el-tab-pane label="Responses" name="responses" />
      </el-tabs>
    </div>

    <!-- 聊天内容区 -->
    <div ref="scrollContainer" class="ai-chat__messages">
      <!-- 空状态 -->
      <div v-if="messages.length === 0" class="ai-chat__empty">
        <div class="ai-chat__empty-icon">
          <el-icon :size="48"><ChatDotRound /></el-icon>
        </div>
        <p class="ai-chat__empty-text">开始一段新的对话</p>
        <p class="ai-chat__empty-hint">选择模型，输入消息即可开始</p>
      </div>

      <!-- 消息列表 -->
      <div v-else class="ai-chat__list">
        <ChatMessage
          v-for="msg in messages"
          :key="msg.id"
          :message="msg"
        />
      </div>
    </div>

    <!-- 输入操作区：按 Tab 切换 -->
    <div class="ai-chat__input-area">
      <ChatInput
        v-if="activeTab === 'chat'"
        ref="chatInputRef"
        :models="modelList"
        :loading="isLoading"
        @send="handleSend"
      />
      <ChatResponses
        v-else
        :models="modelList"
        :loading="isLoading"
        @send="handleResponsesSend"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { ChatDotRound } from "@element-plus/icons-vue";
import ChatMessage from "./components/ChatMessage.vue";
import ChatInput from "./components/ChatInput.vue";
import ChatResponses from "./components/ChatResponses.vue";
import { sendResponsesStream } from "@/api/aigate/responses";
import {
  listChatModels,
  sendChatStream,
  type ChatMessage as ChatMessageType
} from "@/api/aigate/chat";
import type { AiModel } from "@/types";

/** 消息列表 */
const messages = ref<ChatMessageType[]>([]);
/** 模型列表 */
const modelList = ref<AiModel[]>([]);
/** 是否正在生成 */
const isLoading = ref(false);
/** 滚动容器 */
const scrollContainer = ref<HTMLDivElement>();
/** 输入组件引用 */
const chatInputRef = ref<InstanceType<typeof ChatInput>>();
/** 当前格式 Tab */
const activeTab = ref<"chat" | "responses">("chat");
/** 当前请求的 AbortController，用于组件卸载时取消 */
let currentAbortController: AbortController | null = null;

/** 生成唯一 ID */
let idCounter = 0;
function genId(): string {
  return `msg_${Date.now()}_${++idCounter}`;
}

/** 自动滚动到底部 */
async function scrollToBottom() {
  await nextTick();
  if (scrollContainer.value) {
    scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight;
  }
}

/** 加载模型列表 */
async function loadOptions() {
  try {
    const modelRes = await listChatModels();
    if (modelRes.code === 200) {
      modelList.value = (modelRes as any).data || [];
    }
  } catch (e) {
    console.error("加载选项失败:", e);
  }
}

/** 取消当前请求 */
function abortCurrentRequest() {
  if (currentAbortController) {
    currentAbortController.abort();
    currentAbortController = null;
  }
}

/** 发送消息 */
async function handleSend(content: string, modelName: string) {
  if (isLoading.value) return;

  // 取消之前未完成的请求
  abortCurrentRequest();

  // 添加用户消息
  const userMsg: ChatMessageType = {
    id: genId(),
    role: "user",
    content,
    timestamp: Date.now()
  };
  messages.value.push(userMsg);

  // 添加 AI 消息占位（push 后通过响应式引用修改，避免丢失响应性）
  const aiMsgPlaceholder: ChatMessageType = {
    id: genId(),
    role: "assistant",
    content: "",
    timestamp: Date.now(),
    streaming: true,
    modelName
  };
  messages.value.push(aiMsgPlaceholder);
  const aiMsgIndex = messages.value.length - 1;
  await scrollToBottom();

  isLoading.value = true;

  // 创建 AbortController
  const abortController = new AbortController();
  currentAbortController = abortController;

  try {
    // 构建请求消息（排除最后一条 AI 占位消息，取最近 20 条上下文）
    const allMessages = messages.value.filter(m => m.role === "user" || (m.role === "assistant" && !m.streaming));
    const contextMessages = allMessages.slice(-20).map(m => ({ role: m.role, content: m.content }));

    const response = await sendChatStream(
      {
        model: modelName,
        messages: contextMessages,
        stream: true
      },
      abortController.signal
    );

    if (!response.ok) {
      // 尝试读取错误详情
      let errorDetail = `请求失败: ${response.status}`;
      try {
        const errorText = await response.text();
        if (errorText) {
          const errorJson = JSON.parse(errorText);
          errorDetail = errorJson.msg || errorJson.error?.message || errorDetail;
        }
      } catch {
        // 忽略解析错误
      }
      throw new Error(errorDetail);
    }

    if (!response.body) {
      throw new Error("响应流为空");
    }

    // 消费 SSE 流
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let done = false;
    let terminalReceived = false;

    while (!done) {
      const { done: readerDone, value } = await reader.read();
      if (readerDone) break;

      buffer += decoder.decode(value, { stream: true });

      // 按换行分割，处理 SSE 行
      const lines = buffer.split("\n");
      // 最后一行可能不完整，保留到下次处理
      buffer = lines.pop() || "";

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith(":")) continue;

        if (trimmed.startsWith("data:")) {
          const data = trimmed.slice(5).trim();

          if (data === "[DONE]") {
            terminalReceived = true;
            done = true;
            break;
          }

          let parsed: any;
          try {
            parsed = JSON.parse(data);
          } catch {
            // 非 JSON 数据，忽略
            continue;
          }

          const errorMessage = parsed.error?.message
            || (parsed.type === "error" ? parsed.message : undefined);
          if (errorMessage) {
            throw new Error(errorMessage);
          }

          const choice = parsed.choices?.[0];
          const delta = choice?.delta?.content || "";
          if (delta) {
            messages.value[aiMsgIndex].content += delta;
            await scrollToBottom();
          }
          if (choice?.finish_reason != null) {
            terminalReceived = true;
            done = true;
            break;
          }
        }
      }
    }

    if (!terminalReceived) {
      throw new Error("响应流在终止事件前结束");
    }
    messages.value[aiMsgIndex].streaming = false;
  } catch (e: any) {
    if (e.name === "AbortError") {
      // 请求被主动取消（如切换页面），不显示错误
      messages.value[aiMsgIndex].streaming = false;
      return;
    }
    console.error("聊天请求异常:", e);
    messages.value[aiMsgIndex].content = messages.value[aiMsgIndex].content || `请求失败: ${e.message}`;
    messages.value[aiMsgIndex].streaming = false;
    ElMessage.error("聊天请求失败: " + e.message);
  } finally {
    isLoading.value = false;
    currentAbortController = null;
    await scrollToBottom();
    chatInputRef.value?.focus();
  }
}

/** 发送 Responses 格式消息 */
async function handleResponsesSend(input: string, modelName: string) {
  if (isLoading.value) return;
  abortCurrentRequest();

  const userMsg: ChatMessageType = {
    id: genId(),
    role: "user",
    content: input,
    timestamp: Date.now(),
  };
  messages.value.push(userMsg);

  const aiMsgPlaceholder: ChatMessageType = {
    id: genId(),
    role: "assistant",
    content: "",
    timestamp: Date.now(),
    streaming: true,
    modelName,
  };
  messages.value.push(aiMsgPlaceholder);
  const aiMsgIndex = messages.value.length - 1;
  await scrollToBottom();

  isLoading.value = true;
  const abortController = new AbortController();
  currentAbortController = abortController;

  try {
    const response = await sendResponsesStream(
      { model: modelName, input: input, stream: true },
      abortController.signal
    );

    if (!response.ok) {
      let errorDetail = `请求失败: ${response.status}`;
      try {
        const errorText = await response.text();
        if (errorText) {
          const errorJson = JSON.parse(errorText);
          errorDetail = errorJson.msg || errorJson.error?.message || errorDetail;
        }
      } catch { /* ignore */ }
      throw new Error(errorDetail);
    }

    if (!response.body) { throw new Error("响应流为空"); }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let done = false;
    let terminalReceived = false;

    while (!done) {
      const { done: readerDone, value } = await reader.read();
      if (readerDone) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() || "";

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith(":")) continue;
        if (trimmed.startsWith("data:")) {
          const data = trimmed.slice(5).trim();
          if (data === "[DONE]") {
            terminalReceived = true;
            done = true;
            break;
          }

          let parsed: any;
          try {
            parsed = JSON.parse(data);
          } catch {
            continue;
          }

          if (parsed.type === "response.output_text.delta" && parsed.delta) {
            messages.value[aiMsgIndex].content += parsed.delta;
            await scrollToBottom();
          }
          if (parsed.type === "response.completed") {
            terminalReceived = true;
            done = true;
            break;
          }
          if (parsed.type === "response.failed") {
            const errorMessage = parsed.response?.error?.message
              || parsed.error?.message
              || "Responses 请求失败";
            throw new Error(errorMessage);
          }
          if (parsed.type === "response.incomplete") {
            const reason = parsed.response?.incomplete_details?.reason;
            throw new Error(reason ? `响应未完成: ${reason}` : "响应未完成");
          }
        }
      }
    }

    if (!terminalReceived) {
      throw new Error("响应流在终止事件前结束");
    }
    messages.value[aiMsgIndex].streaming = false;
  } catch (e: any) {
    if (e.name === "AbortError") {
      messages.value[aiMsgIndex].streaming = false;
      return;
    }
    console.error("Responses 请求异常:", e);
    messages.value[aiMsgIndex].content =
      messages.value[aiMsgIndex].content || `请求失败: ${e.message}`;
    messages.value[aiMsgIndex].streaming = false;
    ElMessage.error("Responses 请求失败: " + e.message);
  } finally {
    isLoading.value = false;
    currentAbortController = null;
    await scrollToBottom();
  }
}

onMounted(() => {
  loadOptions();
});

onUnmounted(() => {
  // 组件卸载时取消正在进行的请求，防止内存泄漏
  abortCurrentRequest();
});
</script>

<style lang="scss" scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  // background: var(--el-bg-color-page);
  overflow: hidden;
  color: var(--el-text-color-primary);
  transition: background-color 0.2s ease, color 0.2s ease;
}

.ai-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--el-border-color);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.ai-chat__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  user-select: none;
}

.ai-chat__empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--el-color-primary-light-9);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;

  :deep(.el-icon) {
    color: var(--el-color-primary);
  }
}

.ai-chat__empty-text {
  font-size: 18px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin: 0 0 6px;
}

.ai-chat__empty-hint {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.ai-chat__list {
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.ai-chat__input-area {
  padding: 0 24px 20px;
  max-width: 848px;
  margin: 0 auto;
  width: 100%;
}

.ai-chat__tabs {
  padding: 0 24px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  transition: background-color 0.2s ease, border-color 0.2s ease;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    color: var(--el-text-color-secondary);

    &.is-active {
      color: var(--el-color-primary);
    }
  }
}
</style>
