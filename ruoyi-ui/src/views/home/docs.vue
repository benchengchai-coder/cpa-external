<template>
  <div class="docs-page">
    <div class="page-inner">
      <!-- 左侧目录 -->
      <aside class="docs-sidebar">
        <div class="sidebar-inner">
          <div class="sidebar-header">
            <h3>文档目录</h3>
            <p class="sidebar-sub">快速上手与 API 参考</p>
          </div>
          <nav class="sidebar-nav">
            <div
              v-for="group in toc"
              :key="group.title"
              class="sidebar-group"
            >
              <p class="sidebar-group-title">{{ group.title }}</p>
              <ul class="sidebar-list">
                <li
                  v-for="item in group.items"
                  :key="item.id"
                  class="sidebar-item"
                  :class="{ 'is-active': activeSection === item.id }"
                  @click="scrollToSection(item.id)"
                >
                  <span class="sidebar-item-dot" v-if="activeSection === item.id"></span>
                  <span class="sidebar-item-text">{{ item.label }}</span>
                </li>
              </ul>
            </div>
          </nav>
        </div>
      </aside>

      <!-- 右侧内容 -->
      <main class="docs-content">
        <article class="docs-article">
          <!-- 顶部元信息 -->
          <header class="article-header">
            <div class="article-meta">
              <span class="article-badge">指南</span>
              <span class="article-updated">更新于 2026/06/16</span>
            </div>
            <h1 class="article-title">接入配置</h1>
            <p class="article-intro">
              调用 AI 网关接口前，您需要完成三项基础配置：确认 Base URL、获取 API Key、了解鉴权方式。
            </p>
            <el-alert
              class="article-tip"
              type="info"
              :closable="false"
              show-icon
            >
              <template #title>
                如果你还不熟悉控制台页面和基本操作，请先查看 <router-link to="/general-service/dashboard">控制台</router-link> 指南。
              </template>
            </el-alert>
          </header>

          <!-- 快速开始 -->
          <section id="quickstart" class="docs-section">
            <h2 class="section-title">
              <span class="section-hash">#</span>快速开始
            </h2>
            <p class="section-text">
              从注册到第一次调用，最快 5 分钟即可完成接入。整体流程分为四步：
            </p>
            <div class="steps-grid">
              <div v-for="(step, i) in steps" :key="step.title" class="step-card">
                <div class="step-head">
                  <span class="step-num">{{ i + 1 }}</span>
                  <span class="step-line" v-if="i < steps.length - 1"></span>
                </div>
                <h3 class="step-title">{{ step.title }}</h3>
                <p class="step-desc">{{ step.desc }}</p>
              </div>
            </div>
          </section>

          <!-- Base URL -->
          <section id="base-url" class="docs-section">
            <h2 class="section-title">
              <span class="section-hash">#</span>Base URL
            </h2>
            <p class="section-text">
              AI 网关为不同业务线分配了独立端点。请根据你使用的服务选择对应地址：
            </p>
            <div class="kv-card">
              <div class="kv-row">
                <span class="kv-label">AI 大模型 API</span>
                <code class="kv-value">{{ baseUrl }}/v1</code>
              </div>
            </div>
          </section>

          <!-- 认证鉴权 -->
          <section id="auth" class="docs-section">
            <h2 class="section-title">
              <span class="section-hash">#</span>认证鉴权
            </h2>
            <p class="section-text">
              所有 API 请求需要在请求头（Header）中携带 <code class="inline-code">Authorization</code> 字段，
              使用 Bearer Token 方式进行身份验证。
            </p>
            <CodeBlock
              language="http"
              code="Authorization: Bearer sk-your-api-key"
            />
            <el-alert
              class="section-alert"
              type="warning"
              :closable="false"
              show-icon
            >
              <template #title>
                请妥善保管你的 API Key，不要提交到代码仓库或暴露在客户端代码中。
              </template>
            </el-alert>
          </section>

          <!-- 对话接口 -->
          <section id="chat" class="docs-section">
            <h2 class="section-title">
              <span class="section-hash">#</span>对话接口
            </h2>
            <p class="section-text">
              对话接口完全兼容 OpenAI 格式，请求体使用标准的 messages 数组：
            </p>
            <CodeBlock
              language="http"
              :code="chatExample"
            />
            <p class="section-text">
              成功响应将返回与 OpenAI 一致的 JSON 结构，包含 <code class="inline-code">choices</code>、<code class="inline-code">usage</code> 等字段。
            </p>
          </section>

          <!-- 模型列表 -->
          <section id="models" class="docs-section">
            <h2 class="section-title">
              <span class="section-hash">#</span>模型列表
            </h2>
            <p class="section-text">
              通过模型列表接口可获取当前账号可用的全部模型及其上下文长度：
            </p>
            <CodeBlock
              language="http"
              code="GET /v1/models"
            />
            <p class="section-text">
              完整的模型说明、计费单价可在
              <router-link to="/home/models" class="inline-link">模型广场</router-link>
              中查看。
            </p>
          </section>

          <!-- 错误码 -->
          <section id="errors" class="docs-section">
            <h2 class="section-title">
              <span class="section-hash">#</span>错误码说明
            </h2>
            <p class="section-text">
              当请求出现异常时，接口会返回对应的 HTTP 状态码与 JSON 错误体，可据此排查：
            </p>
            <div class="error-table">
              <div class="error-head">
                <span>状态码</span>
                <span>说明</span>
                <span>解决方案</span>
              </div>
              <div v-for="err in errorCodes" :key="err.code" class="error-row">
                <span class="error-code">{{ err.code }}</span>
                <span class="error-message">{{ err.message }}</span>
                <span class="error-solution">{{ err.solution }}</span>
              </div>
            </div>
          </section>

          <!-- 文档反馈 -->
          <footer class="article-footer">
            <div class="footer-help">
              <span class="footer-help-icon">?</span>
              <span class="footer-help-text">
                还有疑问？<router-link to="/home/about" class="inline-link">联系我们</router-link> 获取技术支持。
              </span>
            </div>
            <div class="footer-nav">
              <router-link to="/home/pricing" class="footer-nav-link">← 价格方案</router-link>
              <router-link to="/home/models" class="footer-nav-link">模型广场 →</router-link>
            </div>
          </footer>
        </article>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import CodeBlock from '@/components/CodeBlock/index.vue'

const baseUrl = window.location.origin

const activeSection = ref('quickstart')

const toc = [
  {
    title: '入门',
    items: [
      { id: 'quickstart', label: '快速开始' },
      { id: 'base-url', label: 'Base URL' }
    ]
  },
  {
    title: 'API 参考',
    items: [
      { id: 'auth', label: '认证鉴权' },
      { id: 'chat', label: '对话接口' },
      { id: 'models', label: '模型列表' },
      { id: 'errors', label: '错误码说明' }
    ]
  }
]

const steps = [
  { title: '注册账号', desc: '访问注册页面创建账号，完成邮箱验证' },
  { title: '获取 API Key', desc: '系统会自动生成唯一密钥，在控制台的「API Key 管理」中复制即可' },
  { title: '安装并配置cc-switch', desc: '在模型广场中浏览并选择适合的模型' },
  { title: '发起调用', desc: 'Codex测试使用' }
]

const chatExample = `POST /v1/chat/completions
Content-Type: application/json
Authorization: Bearer sk-your-api-key

{
  "model": "gpt-5.5",
  "messages": [
    { "role": "user", "content": "你好" }
  ]
}`

const errorCodes = [
  { code: '401', message: 'API Key 无效或已过期', solution: '检查 API Key 是否正确，必要时在控制台更换密钥' },
  { code: '429', message: '请求频率超限', solution: '降低请求频率或升级套餐' },
  { code: '500', message: '服务内部错误', solution: '稍后重试，持续报错请联系技术支持' },
  { code: '503', message: '模型服务不可用', solution: '模型维护中，请稍后重试' }
]

// 所有章节 id（与 toc 顺序一致）
const sectionIds = toc.flatMap(g => g.items.map(i => i.id))

function scrollToSection(id: string): void {
  activeSection.value = id
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 滚动时根据各章节顶部位置计算当前激活项：
// 取「顶部边缘已越过阈值线」的最后一节，对短章节也稳定。
function updateActiveSection(): void {
  const threshold = 120 // 距视口顶部的阈值线（px）
  let current = sectionIds[0]
  for (const id of sectionIds) {
    const el = document.getElementById(id)
    if (!el) continue
    if (el.getBoundingClientRect().top <= threshold) {
      current = id
    }
  }
  activeSection.value = current
}

onMounted(() => {
  updateActiveSection()
  window.addEventListener('scroll', updateActiveSection, { passive: true })
  onBeforeUnmount(() => window.removeEventListener('scroll', updateActiveSection))
})
</script>

<style lang="scss" scoped>
.docs-page {
  width: 100%;
  min-height: 100vh;
  background: var(--hp-bg, #f7f8fa);
}

.page-inner {
  display: flex;
  align-items: flex-start;
  max-width: 1200px;
  margin: 0 auto;
  padding: 48px 24px 96px;
  gap: 48px;
}

/* ── Sidebar ── */
.docs-sidebar {
  flex: 0 0 220px;
  position: sticky;
  top: 88px;
}

.sidebar-inner {
  padding: 4px 0;
}

.sidebar-header {
  margin-bottom: 20px;
  padding: 0 12px;
}

.sidebar-header h3 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--hp-text, #1f2329);
}

.sidebar-sub {
  margin: 0;
  font-size: 12px;
  color: var(--hp-text-muted, #8f959e);
}

.sidebar-group {
  margin-bottom: 20px;
}

.sidebar-group-title {
  margin: 0 0 8px;
  padding: 0 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--hp-text-muted, #8f959e);
}

.sidebar-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--hp-text-secondary, #4e5969);
  cursor: pointer;
  transition: all 0.2s ease;
}

.sidebar-item:hover {
  background: var(--hp-accent-soft, rgba(99, 102, 241, 0.08));
  color: var(--hp-accent-text, #6366f1);
}

.sidebar-item.is-active {
  background: var(--hp-accent-soft, rgba(99, 102, 241, 0.1));
  color: var(--hp-accent-text, #6366f1);
  font-weight: 600;
}

.sidebar-item-dot {
  flex: 0 0 auto;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--hp-accent-text, #6366f1);
}

.sidebar-item-text {
  flex: 1 1 auto;
  min-width: 0;
}

/* ── Content ── */
.docs-content {
  flex: 1 1 auto;
  min-width: 0;
}

.docs-article {
  background: var(--hp-bg-card, #fff);
  border: 1px solid var(--hp-border, #ebeef5);
  border-radius: 14px;
  padding: 40px 48px;
  box-shadow: var(--hp-shadow-card, 0 1px 3px rgba(0, 0, 0, 0.04));
}

.article-header {
  margin-bottom: 48px;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--hp-border, #ebeef5);
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.article-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--hp-accent-text, #6366f1);
  background: var(--hp-accent-soft, rgba(99, 102, 241, 0.1));
}

.article-updated {
  font-size: 13px;
  color: var(--hp-text-muted, #8f959e);
}

.article-title {
  margin: 0 0 12px;
  font-size: 30px;
  font-weight: 700;
  color: var(--hp-text, #1f2329);
  letter-spacing: -0.01em;
}

.article-intro {
  margin: 0 0 20px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--hp-text-secondary, #4e5969);
}

.article-tip :deep(a) {
  color: var(--hp-accent-text, #6366f1);
  font-weight: 600;
}

/* ── Sections ── */
.docs-section {
  margin-bottom: 48px;
  scroll-margin-top: 88px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 700;
  color: var(--hp-text, #1f2329);
}

.section-hash {
  color: var(--hp-text-muted, #c0c4cc);
  font-weight: 400;
}

.section-text {
  margin: 0 0 16px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--hp-text-secondary, #4e5969);
}

.inline-code {
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  background: var(--hp-accent-soft, rgba(99, 102, 241, 0.08));
  color: var(--hp-accent-text, #6366f1);
}

.inline-link {
  color: var(--hp-accent-text, #6366f1);
  text-decoration: none;
  font-weight: 600;
}

.inline-link:hover {
  text-decoration: underline;
}

.section-alert {
  margin-top: 16px;
}

/* ── Steps grid ── */
.steps-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 24px;
}

.step-card {
  padding: 20px;
  border-radius: 12px;
  background: var(--hp-bg, #f7f8fa);
  border: 1px solid var(--hp-border, #ebeef5);
}

.step-head {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
}

.step-num {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #7c3aed);
}

.step-line {
  flex: 1 1 auto;
  height: 2px;
  margin-left: 12px;
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.3), transparent);
}

.step-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text, #1f2329);
}

.step-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--hp-text-secondary, #4e5969);
}

/* ── Key-value card ── */
.kv-card {
  border: 1px solid var(--hp-border, #ebeef5);
  border-radius: 12px;
  overflow: hidden;
}

.kv-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;

  &:not(:last-child) {
    border-bottom: 1px solid var(--hp-border, #ebeef5);
  }
}

.kv-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text, #1f2329);
}

.kv-value {
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: var(--hp-accent-text, #6366f1);
}

/* ── Error table ── */
.error-table {
  border: 1px solid var(--hp-border, #ebeef5);
  border-radius: 12px;
  overflow: hidden;
}

.error-head,
.error-row {
  display: grid;
  grid-template-columns: 100px 1fr 1fr;
  gap: 16px;
  padding: 12px 20px;
  font-size: 14px;
}

.error-head {
  background: var(--hp-bg, #f7f8fa);
  font-weight: 600;
  color: var(--hp-text-muted, #8f959e);
}

.error-row {
  align-items: center;
  border-top: 1px solid var(--hp-border, #ebeef5);
  color: var(--hp-text-secondary, #4e5969);
}

.error-code {
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-weight: 700;
  color: #ef4444;
}

.error-message {
  color: var(--hp-text, #1f2329);
}

/* ── Article footer ── */
.article-footer {
  margin-top: 56px;
  padding-top: 28px;
  border-top: 1px solid var(--hp-border, #ebeef5);
}

.footer-help {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  font-size: 14px;
  color: var(--hp-text-secondary, #4e5969);
}

.footer-help-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: var(--hp-accent-text, #6366f1);
  background: var(--hp-accent-soft, rgba(99, 102, 241, 0.1));
}

.footer-nav {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.footer-nav-link {
  font-size: 14px;
  font-weight: 600;
  color: var(--hp-text-secondary, #4e5969);
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer-nav-link:hover {
  color: var(--hp-accent-text, #6366f1);
}

/* ── Responsive ── */
@media (max-width: 960px) {
  .page-inner {
    flex-direction: column;
    gap: 32px;
  }

  .docs-sidebar {
    position: static;
    flex: none;
    width: 100%;
  }

  .sidebar-inner {
    border: 1px solid var(--hp-border, #ebeef5);
    border-radius: 12px;
    padding: 16px;
    background: var(--hp-bg-card, #fff);
  }

  .sidebar-group {
    margin-bottom: 12px;
  }

  .docs-article {
    padding: 28px 20px;
  }

  .steps-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .steps-grid {
    grid-template-columns: 1fr;
  }

  .error-head,
  .error-row {
    grid-template-columns: 70px 1fr;
  }

  .error-solution {
    grid-column: 1 / -1;
    padding-top: 4px;
    color: var(--hp-text-muted, #8f959e);
  }
}
</style>
