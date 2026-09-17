<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-grid"></div>
        <div class="hero-glow hero-glow--1"></div>
        <div class="hero-glow hero-glow--2"></div>
      </div>
      <div class="hero-inner">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          <span>API Gateway</span>
        </div>

        <p class="hero-desc">统一接口、智能路由、精准计费 —— 让 AI 集成像调用函数一样简单</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" class="hero-btn-primary" @click="$router.push('/home/models')">
            浏览模型广场
            <el-icon class="hero-btn-arrow"><Right /></el-icon>
          </el-button>
          <el-button size="large" class="hero-btn-ghost" @click="$router.push('/home/docs')"> 快速接入文档 </el-button>
        </div>

        <!-- 代码预览 -->
        <div class="code-preview">
          <div class="code-header">
            <span class="code-dots"> <i></i><i></i><i></i> </span>
            <span class="code-filename">quickstart.py</span>
          </div>
          <pre
            class="code-body"
          ><code><span class="code-keyword">from</span> openai <span class="code-keyword">import</span> OpenAI

client = OpenAI(
    api_key=<span class="code-string">"***"</span>,
    base_url=<span class="code-string">"https://cbc.icu/v1"</span>
)

response = client.chat.completions.create(
    model=<span class="code-string">"gpt-5.6-sol"</span>,
    messages=[{<span class="code-string">"role"</span>: <span class="code-string">"user"</span>, <span class="code-string">"content"</span>: <span class="code-string">"你好"</span>}]
)
<span class="code-builtin">print</span>(response.choices[<span class="code-number">0</span>].message.content)</code></pre>
        </div>
      </div>
    </section>

    <!-- FAQ -->
    <section class="faq">
      <div class="section-inner">
        <div class="section-header">
          <h2 class="section-title">常见问题</h2>
          <p class="section-desc">关于接入、计费与使用的常见疑问解答</p>
        </div>
        <el-collapse v-model="activeFaq" class="faq-collapse" accordion>
          <el-collapse-item
            v-for="(item, index) in faqs"
            :key="item.question"
            :name="String(index)"
          >
            <template #title>
              <div class="faq-item-title">
                <span class="faq-item-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="faq-item-question">{{ item.question }}</span>
              </div>
            </template>
            <p class="faq-item-answer">{{ item.answer }}</p>
          </el-collapse-item>
        </el-collapse>
      </div>
    </section>

    <!-- Footer -->
    <footer class="site-footer">
      <div class="footer-inner">
        <div class="footer-grid">
          <!-- 品牌 -->
          <div class="footer-brand">
            <div class="footer-brand-lockup">
              <div class="footer-brand-mark">{{ brandInitial }}</div>
              <div>
                <p class="footer-brand-kicker">AI Gateway</p>
                <p class="footer-brand-title">{{ title }}</p>
              </div>
            </div>
            <p class="footer-brand-desc">
              统一接口、智能路由、精准计费 —— 让 AI 集成像调用函数一样简单
            </p>
          </div>

          <!-- 产品 -->
          <div class="footer-col">
            <h4 class="footer-col-title">产品</h4>
            <ul class="footer-col-list">
              <li><router-link to="/home/models">模型广场</router-link></li>
              <li><router-link to="/home/pricing">价格方案</router-link></li>
              <li><router-link to="/home/docs">接入文档</router-link></li>
            </ul>
          </div>

          <!-- 资源 -->
          <div class="footer-col">
            <h4 class="footer-col-title">资源</h4>
            <ul class="footer-col-list">
              <li><router-link to="/home/about">关于我们</router-link></li>
              <li><router-link to="/home/docs">使用指南</router-link></li>
              <li><router-link to="/register">免费注册</router-link></li>
            </ul>
          </div>

          <!-- 账户 -->
          <div class="footer-col">
            <h4 class="footer-col-title">账户</h4>
            <ul class="footer-col-list">
              <li><router-link to="/login">登录</router-link></li>
              <li><router-link to="/register">注册</router-link></li>
            </ul>
          </div>
        </div>

        <div class="footer-bottom">
          <span class="footer-copyright">{{ footerContent }}</span>
          <div class="footer-legal">
            <a href="javascript:void(0)">服务条款</a>
            <span class="footer-legal-divider">·</span>
            <a href="javascript:void(0)">隐私政策</a>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { Right } from '@element-plus/icons-vue'
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const brandInitial = computed(() => String(title || 'A').trim().slice(0, 1) || 'A')

// Base URL for code preview
const baseUrl = window.location.origin

const activeFaq = ref('')

const faqs = [
  {
    question: '是否兼容 OpenAI 接口格式？',
    answer: '完全兼容。你只需将 base_url 指向本服务，使用标准 OpenAI SDK 即可直接调用，无需改动业务代码，即可在多个大模型之间无缝切换。'
  },
  {
    question: '如何获取 API Key？',
    answer: '注册成功后，系统会自动为账号生成唯一的 API Key。登录控制台进入「API Key 管理」即可查看和复制；如怀疑密钥泄露，可随时更换，更换后旧密钥立即失效。'
  },
  {
    question: '计费方式是怎样的？',
    answer: '主打订阅制，按月计费;'
  },
  {
    question: '支持哪些大模型？',
    answer: '目前仅支持 OpenAI的对话、补全、多模态模型，并持续接入新模型。可在「模型广场」查看完整列表、上下文长度和单价信息。'
  },
  {
    question: '如何保障稳定性与可用性？',
    answer: '内置多渠道智能路由与负载均衡，自动故障转移；当某个渠道触发限流或异常时，请求会自动切换到健康渠道，保障业务连续性。'
  }
]
</script>

<style lang="scss" scoped>
/* ── Theme tokens (light = default, dark via html.dark) ── */
.home-page {
  /* background */
  --hp-bg: #ffffff;
  --hp-bg-alt: #f7f8fa;
  --hp-bg-card: #ffffff;
  --hp-bg-card-hover: #f5f6f8;
  --hp-bg-code: #0d1117;

  /* text */
  --hp-text: #1a1a2e;
  --hp-text-secondary: #5a6070;
  --hp-text-muted: #9ca3b0;

  /* border */
  --hp-border: rgba(0, 0, 0, 0.06);
  --hp-border-hover: rgba(0, 0, 0, 0.1);

  /* accents */
  --hp-accent: #6366f1;
  --hp-accent-soft: rgba(99, 102, 241, 0.08);
  --hp-accent-text: #6366f1;

  /* hero glow opacity */
  --hp-glow-opacity: 0.12;
  --hp-grid-opacity: 0.04;

  /* shadow */
  --hp-shadow-card: 0 1px 3px rgba(0, 0, 0, 0.04);
  --hp-shadow-code: 0 8px 40px rgba(0, 0, 0, 0.08);

  /* code dots */
  --hp-code-text: #c9d1d9;
}

html.dark .home-page {
  --hp-bg: #0a0a0f;
  --hp-bg-alt: #111118;
  --hp-bg-card: #16161e;
  --hp-bg-card-hover: #1c1c28;
  --hp-bg-code: #0d1117;

  --hp-text: #f0f0f5;
  --hp-text-secondary: #9ca3b0;
  --hp-text-muted: #6b7280;

  --hp-border: rgba(255, 255, 255, 0.06);
  --hp-border-hover: rgba(255, 255, 255, 0.1);

  --hp-accent-soft: rgba(99, 102, 241, 0.08);
  --hp-accent-text: #a5b4fc;

  --hp-glow-opacity: 0.35;
  --hp-grid-opacity: 0.025;

  --hp-shadow-card: 0 1px 3px rgba(0, 0, 0, 0.2);
  --hp-shadow-code: 0 20px 60px rgba(0, 0, 0, 0.4);

  --hp-code-text: #c9d1d9;
}

/* ── Shared ── */
$radius: 16px;

.home-page {
  width: 100%;
  color: var(--hp-text);
  background: var(--hp-bg);
}

.section-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 100px 24px;
}

.section-header {
  text-align: center;
  margin-bottom: 64px;
}

.section-title {
  font-size: 36px;
  font-weight: 700;
  color: var(--hp-text);
  margin: 0 0 16px;
  letter-spacing: -0.02em;
}

.section-desc {
  font-size: 16px;
  color: var(--hp-text-secondary);
  margin: 0;
}

/* ── Hero ── */
.hero {
  position: relative;
  overflow: hidden;
  padding: 120px 24px 80px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, var(--hp-grid-opacity)) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, var(--hp-grid-opacity)) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, black 30%, transparent 100%);
}

.hero-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: var(--hp-glow-opacity);
  transition: opacity 0.3s;

  &--1 {
    width: 600px;
    height: 400px;
    background: #6366f1;
    top: -100px;
    left: 20%;
  }

  &--2 {
    width: 500px;
    height: 350px;
    background: #8b5cf6;
    top: -50px;
    right: 10%;
  }
}

.hero-inner {
  position: relative;
  max-width: 900px;
  margin: 0 auto;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 20px;
  background: var(--hp-accent-soft);
  border: 1px solid rgba(99, 102, 241, 0.18);
  font-size: 13px;
  color: var(--hp-accent-text);
  margin-bottom: 32px;
  font-weight: 500;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--hp-accent);
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%,
  100% {
    opacity: 1;
    box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.4);
  }
  50% {
    opacity: 0.7;
    box-shadow: 0 0 0 6px rgba(99, 102, 241, 0);
  }
}


.hero-desc {
  font-size: 18px;
  color: var(--hp-text-secondary);
  line-height: 1.7;
  margin: 0 auto 40px;
  max-width: 560px;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 64px;
}

.hero-btn-primary {
  padding: 12px 32px !important;
  height: auto !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #6366f1, #7c3aed) !important;
  border: none !important;
  box-shadow: 0 4px 24px rgba(99, 102, 241, 0.3);

  &:hover {
    box-shadow: 0 8px 32px rgba(99, 102, 241, 0.45);
    transform: translateY(-1px);
  }
}

.hero-btn-arrow {
  margin-left: 4px;
  transition: transform 0.2s;
}

.hero-btn-primary:hover .hero-btn-arrow {
  transform: translateX(3px);
}

.hero-btn-ghost {
  padding: 12px 32px !important;
  height: auto !important;
  font-size: 15px !important;
  border-radius: 12px !important;
  background: var(--hp-accent-soft) !important;
  border: 1px solid var(--hp-border) !important;
  color: var(--hp-text-secondary) !important;

  &:hover {
    background: var(--hp-bg-card-hover) !important;
    color: var(--hp-text) !important;
    border-color: var(--hp-border-hover) !important;
  }
}

/* ── Code Preview ── */
.code-preview {
  max-width: 580px;
  margin: 0 auto;
  border-radius: $radius;
  background: var(--hp-bg-code);
  border: 1px solid var(--hp-border);
  overflow: hidden;
  text-align: left;
  box-shadow: var(--hp-shadow-code);
}

.code-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid var(--hp-border);
}

.code-dots {
  display: flex;
  gap: 6px;

  i {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);

    &:nth-child(1) {
      background: #ff5f57;
    }
    &:nth-child(2) {
      background: #febc2e;
    }
    &:nth-child(3) {
      background: #28c840;
    }
  }
}

.code-filename {
  font-size: 12px;
  color: var(--hp-text-muted);
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.code-body {
  margin: 0;
  padding: 20px;
  overflow-x: auto;

  code {
    font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
    font-size: 13px;
    line-height: 1.75;
    color: var(--hp-code-text);
  }
}

.code-keyword {
  color: #ff7b72;
}
.code-string {
  color: #a5d6ff;
}
.code-number {
  color: #79c0ff;
}
.code-builtin {
  color: #d2a8ff;
}

/* ── FAQ ── */
.faq {
  background: var(--hp-bg);
}

.faq .section-inner {
  max-width: 820px;
}

.faq .section-header {
  margin-bottom: 48px;
}

.faq-collapse {
  border: none;
}

.faq-collapse :deep(.el-collapse-item) {
  margin-bottom: 12px;
  border: 1px solid var(--hp-border) !important;
  border-radius: 12px !important;
  background: var(--hp-bg-card);
  box-shadow: var(--hp-shadow-card);
  overflow: hidden;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

.faq-collapse :deep(.el-collapse-item:hover) {
  border-color: var(--hp-border-hover) !important;
}

.faq-collapse :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 64px;
  padding: 18px 24px;
  font-size: 15px;
  font-weight: 600;
  color: var(--hp-text);
  border-bottom: none;
  line-height: 1.5;
  align-items: center;
}

.faq-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.faq-collapse :deep(.el-collapse-item__content) {
  padding: 0 24px 20px;
}

.faq-collapse :deep(.el-collapse-item:last-child) {
  margin-bottom: 0;
}

.faq-item-title {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.faq-item-index {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--hp-accent-soft);
  color: var(--hp-accent-text);
  font-size: 13px;
  font-weight: 700;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}

.faq-item-question {
  flex: 1 1 auto;
  min-width: 0;
}

.faq-item-answer {
  margin: 0;
  padding-left: 46px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--hp-text-secondary);
}

/* ── Footer ── */
.site-footer {
  background: var(--hp-bg-alt);
  border-top: 1px solid var(--hp-border);
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 72px 24px 32px;
}

.footer-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 48px;
  padding-bottom: 40px;
  border-bottom: 1px solid var(--hp-border);
}

.footer-brand-lockup {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.footer-brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3);
}

.footer-brand-kicker {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--hp-accent-text);
  letter-spacing: 0.04em;
}

.footer-brand-title {
  margin: 2px 0 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--hp-text);
}

.footer-brand-desc {
  margin: 0;
  max-width: 320px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--hp-text-secondary);
}

.footer-col-title {
  margin: 0 0 18px;
  font-size: 14px;
  font-weight: 700;
  color: var(--hp-text);
}

.footer-col-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 12px;
}

.footer-col-list a {
  font-size: 14px;
  color: var(--hp-text-secondary);
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer-col-list a:hover {
  color: var(--hp-accent-text);
}

.footer-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 24px;
  font-size: 13px;
  color: var(--hp-text-muted);
}

.footer-legal {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.footer-legal a {
  color: var(--hp-text-muted);
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer-legal a:hover {
  color: var(--hp-accent-text);
}

.footer-legal-divider {
  color: var(--hp-text-muted);
  opacity: 0.6;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .hero {
    padding: 80px 16px 60px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-desc {
    font-size: 15px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: center;
    margin-bottom: 40px;
  }

  .code-preview {
    display: none;
  }

  .section-inner {
    padding: 60px 16px;
  }

  .section-title {
    font-size: 28px;
  }

  .faq-collapse :deep(.el-collapse-item__header) {
    padding: 16px 18px;
    min-height: 56px;
  }

  .faq-collapse :deep(.el-collapse-item__content) {
    padding: 0 18px 16px;
  }

  .faq-item-answer {
    padding-left: 46px;
  }

  .footer-grid {
    grid-template-columns: 1fr 1fr;
    gap: 32px;
  }

  .footer-brand {
    grid-column: 1 / -1;
  }

  .footer-bottom {
    justify-content: center;
    text-align: center;
  }
}
</style>
