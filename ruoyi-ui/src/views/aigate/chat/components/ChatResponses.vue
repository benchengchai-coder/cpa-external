<template>
  <div class="chat-responses">
    <div class="chat-responses__form">
      <el-input
        v-model="inputText"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 5 }"
        placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
        resize="none"
        @keydown="handleKeydown"
      />
      <div class="chat-responses__actions">
        <el-select
          v-model="selectedModel"
          placeholder="选择模型"
          size="small"
          filterable
          class="chat-responses__select"
        >
          <el-option
            v-for="model in models"
            :key="model.modelId"
            :label="model.modelName"
            :value="model.modelName"
          />
        </el-select>
        <el-button
          type="primary"
          :disabled="!canSend"
          :loading="loading"
          @click="handleSend"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import type { AiModel } from "@/types";
import { resolveDefaultChatModel } from "../model";

const props = defineProps<{
  models: AiModel[];
  loading: boolean;
}>();

const emit = defineEmits<{
  send: [input: string, modelName: string];
}>();

const inputText = ref("");
const selectedModel = ref<string>("");

// 优先选中默认模型，不可用时回退到第一个模型
watch(() => props.models, (models) => {
  const modelAvailable = models.some((model) => model.modelName === selectedModel.value);
  if (models.length > 0 && (!selectedModel.value || !modelAvailable)) {
    selectedModel.value = resolveDefaultChatModel(models);
  }
}, { immediate: true });

const canSend = computed(() => {
  return (
    inputText.value.trim() !== "" &&
    selectedModel.value !== "" &&
    !props.loading
  );
});

function handleKeydown(e: KeyboardEvent) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
}

function handleSend() {
  if (!canSend.value) return;
  emit("send", inputText.value.trim(), selectedModel.value);
  inputText.value = "";
}
</script>

<style lang="scss" scoped>
.chat-responses__form {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  box-shadow: var(--el-box-shadow-light);
  padding: 12px 14px 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;

  :deep(.el-textarea__inner) {
    border: none;
    box-shadow: none !important;
    padding: 4px 0;
    font-size: 14px;
    line-height: 1.6;
    background: transparent;
    color: var(--el-text-color-primary);
    caret-color: var(--el-color-primary);

    &::placeholder {
      color: var(--el-text-color-placeholder);
    }

    &:focus {
      border: none;
      box-shadow: none !important;
    }
  }
}

.chat-responses__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.chat-responses__select {
  width: 160px;

  :deep(.el-input__wrapper) {
    background-color: var(--el-fill-color-light);
    border-radius: 8px;
    box-shadow: 0 0 0 1px var(--el-border-color-light) inset;

    &.is-focus {
      box-shadow: 0 0 0 1px var(--el-color-primary) inset;
    }
  }
}
</style>
