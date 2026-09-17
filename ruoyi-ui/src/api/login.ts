import request from '@/utils/request'
import type {
  LoginInfoResult,
  UserInfoResult,
  CaptchaInfoResult,
  RegisterEmailCodeForm,
  RegisterPayload,
  RegisterStatusResult,
  AjaxResult,
  PasswordResetStatusResult,
  PasswordResetEmailCodeForm,
  PasswordResetPayload
} from '@/types'

// API Key 密钥登录
export function loginByApiKey(apiKey: string): Promise<LoginInfoResult> {
  const data = { apiKey }
  return request({
    url: '/apikey/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data
  })
}

// 登录方法
export function login(username: string, password: string, code: string, uuid: string): Promise<LoginInfoResult> {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 注册方法
export function register(data: RegisterPayload): Promise<AjaxResult> {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取注册开关
export function getRegisterStatus(): Promise<RegisterStatusResult> {
  return request({
    url: '/register/status',
    headers: {
      isToken: false
    },
    method: 'get'
  })
}

// 发送注册邮箱验证码
export function sendRegisterEmailCode(data: RegisterEmailCodeForm): Promise<AjaxResult> {
  return request({
    url: '/register/email/code',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取密码重置功能状态
export function getPasswordResetStatus(): Promise<PasswordResetStatusResult> {
  return request({
    url: '/password/reset/status',
    headers: {
      isToken: false
    },
    method: 'get'
  })
}

// 发送密码重置邮箱验证码
export function sendPasswordResetEmailCode(data: PasswordResetEmailCodeForm): Promise<AjaxResult> {
  return request({
    url: '/password/reset/email/code',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 重置密码
export function resetPassword(data: PasswordResetPayload): Promise<AjaxResult> {
  return request({
    url: '/password/reset',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo(): Promise<UserInfoResult> {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 解锁屏幕
export function unlockScreen(password: string) {
  return request({
    url: '/unlockscreen',
    method: 'post',
    data: { password }
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg(): Promise<CaptchaInfoResult> {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}
