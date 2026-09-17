<template>
  <div class="app-container mail-manage-page">
    <el-form ref="mailRef" v-loading="loading" :model="form" :rules="rules" label-width="120px">
      <el-row :gutter="20">
        <!-- 左侧 -->
        <el-col :xs="24" :sm="24" :md="16" :lg="16">
          <!-- SMTP 服务配置卡片 -->
          <el-card class="manage-card">
            <template #header>
              <div class="card-header">
                <el-icon><Message /></el-icon>
                <span>SMTP 服务配置</span>
              </div>
            </template>
            <el-row :gutter="24">
              <el-col :xs="24" :sm="12">
                <el-form-item label="SMTP服务器" prop="host">
                  <el-input v-model="form.host" placeholder="请输入SMTP服务器" clearable />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="SMTP端口" prop="port">
                  <el-input-number v-model="form.port" :min="1" :max="65535" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="SMTP用户名" prop="username">
                  <el-input v-model="form.username" placeholder="请输入SMTP用户名" clearable />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="SMTP密码" prop="password">
                  <el-input
                    v-model="form.password"
                    type="password"
                    show-password
                    :placeholder="form.hasPassword ? '留空则保留原密码' : '请输入SMTP密码'"
                    clearable
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="SMTP认证" prop="authEnable">
                  <el-switch v-model="form.authEnable" active-value="Y" inactive-value="N" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="SSL" prop="sslEnable">
                  <el-switch v-model="form.sslEnable" active-value="Y" inactive-value="N" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="STARTTLS" prop="starttlsEnable">
                  <el-switch v-model="form.starttlsEnable" active-value="Y" inactive-value="N" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="超时时间(ms)" prop="timeout">
                  <el-input-number v-model="form.timeout" :min="1000" :step="1000" controls-position="right" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <!-- 发件人信息卡片 -->
          <el-card class="manage-card">
            <template #header>
              <div class="card-header">
                <el-icon><User /></el-icon>
                <span>发件人信息</span>
              </div>
            </template>
            <el-row :gutter="24">
              <el-col :xs="24" :sm="12">
                <el-form-item label="发件邮箱" prop="fromEmail">
                  <el-input v-model="form.fromEmail" placeholder="请输入发件邮箱" clearable />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="发件人名称" prop="fromName">
                  <el-input v-model="form.fromName" placeholder="请输入发件人名称" clearable />
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <!-- 备注卡片 -->
          <el-card class="manage-card">
            <template #header>
              <div class="card-header">
                <el-icon><Document /></el-icon>
                <span>备注</span>
              </div>
            </template>
            <el-form-item label="备注" prop="remark" label-width="60px">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-card>
        </el-col>

        <!-- 右侧 -->
        <el-col :xs="24" :sm="24" :md="8" :lg="8">
          <!-- 功能开关卡片 -->
          <el-card class="manage-card">
            <template #header>
              <div class="card-header">
                <el-icon><Setting /></el-icon>
                <span>功能开关</span>
              </div>
            </template>
            <div class="switch-group">
              <div class="switch-item">
                <div class="switch-label">
                  <span class="switch-title">启用邮件配置</span>
                  <span class="switch-desc">开启后系统将使用此SMTP配置发送邮件</span>
                </div>
                <el-switch v-model="form.enabled" active-value="Y" inactive-value="N" />
              </div>
              <el-divider />
              <div class="switch-item">
                <div class="switch-label">
                  <span class="switch-title">邮箱验证功能</span>
                  <span class="switch-desc">开启后用户注册时需要邮箱验证码，关闭则跳过邮箱验证</span>
                </div>
                <el-switch v-model="form.emailVerifyEnabled" active-value="Y" inactive-value="N" />
              </div>
            </div>
          </el-card>

          <!-- 操作按钮卡片 -->
          <el-card class="manage-card">
            <template #header>
              <div class="card-header">
                <el-icon><Operation /></el-icon>
                <span>操作</span>
              </div>
            </template>
            <div class="action-buttons">
              <el-button type="primary" icon="Check" @click="submitForm" v-hasPermi="['system:mail:edit']">保存配置</el-button>
              <el-button icon="Message" @click="openTestDialog" v-hasPermi="['system:mail:test']">测试邮件</el-button>
              <el-button icon="Refresh" @click="getConfig">刷新</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-form>

    <el-dialog title="发送测试邮件" v-model="testOpen" width="420px" append-to-body>
      <el-form ref="testRef" :model="testForm" :rules="testRules" label-width="90px">
        <el-form-item label="测试邮箱" prop="email">
          <el-input v-model="testForm.email" placeholder="请输入测试邮箱" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="testLoading" @click="submitTest">发 送</el-button>
          <el-button @click="testOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MailConfig">
import { Message, User, Document, Setting, Operation } from '@element-plus/icons-vue'
import { getMailConfig, testMailConfig, updateMailConfig } from "@/api/system/mail"
import type { MailTestForm, SysMailConfig } from "@/types"

const { proxy } = getCurrentInstance()

const loading = ref<boolean>(false)
const testOpen = ref<boolean>(false)
const testLoading = ref<boolean>(false)

const defaultForm = (): SysMailConfig => ({
  host: "",
  port: 25,
  username: "",
  password: "",
  fromEmail: "",
  fromName: "",
  sslEnable: "N",
  starttlsEnable: "Y",
  authEnable: "Y",
  enabled: "N",
  timeout: 10000,
  remark: "",
  emailVerifyEnabled: "Y"
})

const data = reactive({
  form: defaultForm(),
  testForm: {
    email: ""
  } as MailTestForm,
  rules: {
    host: [{ required: true, message: "SMTP服务器不能为空", trigger: "blur" }],
    port: [{ required: true, message: "SMTP端口不能为空", trigger: "change" }],
    fromEmail: [
      { required: true, message: "发件邮箱不能为空", trigger: "blur" },
      { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }
    ],
    timeout: [{ required: true, message: "超时时间不能为空", trigger: "change" }]
  },
  testRules: {
    email: [
      { required: true, message: "测试邮箱不能为空", trigger: "blur" },
      { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }
    ]
  }
})

const { form, testForm, rules, testRules } = toRefs(data)

function getConfig(): void {
  loading.value = true
  getMailConfig().then(res => {
    form.value = {
      ...defaultForm(),
      ...(res.data || {}),
      password: ""
    }
  }).finally(() => {
    loading.value = false
  })
}

function submitForm(): void {
  proxy.$refs.mailRef.validate((valid: boolean) => {
    if (!valid) {
      return
    }
    updateMailConfig(form.value).then(() => {
      proxy.$modal.msgSuccess("保存成功")
      getConfig()
    })
  })
}

function openTestDialog(): void {
  testForm.value.email = form.value.fromEmail || ""
  testOpen.value = true
}

function submitTest(): void {
  proxy.$refs.testRef.validate((valid: boolean) => {
    if (!valid) {
      return
    }
    testLoading.value = true
    testMailConfig(testForm.value).then(() => {
      proxy.$modal.msgSuccess("发送成功")
      testOpen.value = false
    }).finally(() => {
      testLoading.value = false
    })
  })
}

getConfig()
</script>

<style lang="scss" scoped>
.mail-manage-page {
  .manage-card {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: var(--el-text-color-primary);

    .el-icon {
      color: var(--el-color-primary);
      font-size: 18px;
    }
  }

  .switch-group {
    .switch-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
    }

    .switch-label {
      flex: 1;
      margin-right: 16px;
    }

    .switch-title {
      display: block;
      font-weight: 600;
      color: var(--el-text-color-primary);
      margin-bottom: 4px;
    }

    .switch-desc {
      display: block;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      line-height: 1.5;
    }
  }

  .action-buttons {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .el-button {
      width: 100%;
    }
  }

  :deep(.el-form-item__content) {
    .el-input-number {
      width: 100%;
    }
  }
}
</style>
