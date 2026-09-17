<template>
  <div class="chat-message" :class="[`chat-message--${message.role}`]">
    <!-- AI 消息 -->
    <template v-if="message.role === 'assistant'">
      <div class="chat-message__avatar chat-message__avatar--ai">
        <el-icon :size="18"><Monitor /></el-icon>
      </div>
      <div class="chat-message__bubble chat-message__bubble--ai">
        <div class="chat-message__content" v-text="message.content"></div>
        <span v-if="message.streaming" class="chat-message__cursor">|</span>
      </div>
    </template>

    <!-- 用户消息 -->
    <template v-else>
      <div class="chat-message__bubble chat-message__bubble--user">
        <div class="chat-message__content" v-text="message.content"></div>
      </div>
      <div class="chat-message__avatar chat-message__avatar--user">
        <el-icon :size="18"><User /></el-icon>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { Monitor, User } from "@element-plus/icons-vue";
import type { ChatMessage } from "@/api/aigate/chat";

defineProps<{
  message: ChatMessage;
}>();
</script>

<style lang="scss" scoped>
.chat-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;

  &--assistant {
    justify-content: flex-start;
  }

  &--user {
    justify-content: flex-end;
  }
}

.chat-message__avatar {
  width: 34px;
  height: 34px;
  min-width: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;

  &--ai {
    background: var(--el-color-primary);
  }

  &--user {
    background: var(--el-color-success);
  }
}

.chat-message__bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  word-break: break-word;

  &--ai {
    background: var(--el-bg-color-overlay);
    color: var(--el-text-color-primary);
    border: 1px solid var(--el-border-color-light);
    border-left: 3px solid var(--el-color-primary);
    border-radius: 4px 12px 12px 12px;
    box-shadow: var(--el-box-shadow-light);
  }

  &--user {
    background: var(--el-color-primary);
    color: #fff;
    border-radius: 12px 4px 12px 12px;
  }
}

.chat-message__content {
  white-space: pre-wrap;
}

.chat-message__cursor {
  display: inline-block;
  animation: blink 0.8s infinite;
  color: var(--el-color-primary);
  font-weight: bold;
  margin-left: 2px;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
