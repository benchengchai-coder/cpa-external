import type { AjaxResult } from './common'
import type { SysUser } from './system/user'

// 登录响应
export interface LoginInfoResult extends AjaxResult {
  /** 令牌 */
  token: string
}

/** 用户信息响应 */
export interface UserInfoResult extends AjaxResult {
  /** 用户信息 */
  user: SysUser
  /** 角色数据 */
  roles: string[]
  /** 权限数据 */
  permissions: string[]
  /** 初始密码是否提醒修改 */
  isDefaultModifyPwd?: boolean
  /** 密码是否过期 */
  isPasswordExpired?: boolean
}

/** 验证码响应 */
export interface CaptchaInfoResult extends AjaxResult {
  /** 验证码缓存key */
  uuid: string;
  /** 验证码图片Base64 */
  img: string;
  /** 验证码开关 */
  captchaEnabled: boolean
}

/** 注册状态响应 */
export interface RegisterStatusResult extends AjaxResult {
  /** 注册开关 */
  registerEnabled: boolean
  /** 邮箱验证开关 */
  emailVerifyEnabled: boolean
}

/** 密码重置功能状态响应 */
export interface PasswordResetStatusResult extends AjaxResult {
  /** 密码重置功能是否可用 */
  resetEnabled: boolean
  /** 图形验证码开关 */
  captchaEnabled: boolean
  /** 密码字符规则 */
  pwdChrtype: string
}

/** 注册邮箱验证码提交信息 */
export interface RegisterEmailCodeForm {
  email: string
}

/** 密码重置邮箱验证码提交信息 */
export interface PasswordResetEmailCodeForm {
  username: string
  email: string
  code: string
  uuid: string
}

/** 密码重置提交信息 */
export interface PasswordResetPayload {
  username: string
  email: string
  emailCode: string
  newPassword: string
}

/** 忘记密码页面表单信息 */
export interface ForgotPasswordForm extends PasswordResetEmailCodeForm, PasswordResetPayload {
  confirmPassword: string
}

/** 注册接口提交信息 */
export interface RegisterPayload {
  username: string
  password: string
  email: string
  emailCode: string
  code: string
  uuid: string
  /** 邀请码（邀请返利，可选） */
  inviteCode?: string
}

/** 注册表单信息 */
export interface RegisterForm extends RegisterPayload {
  confirmPassword: string
}

/** 登录提交信息 */
export interface LoginForm {
  username: string
  password: string
  rememberMe?: boolean | string
  code: string
  uuid: string
}

/** 密钥登录提交信息 */
export interface ApiKeyLoginForm {
  apiKey: string
}

/** 登录模式枚举 */
export type LoginMode = 'password' | 'apikey'
