import type { BaseEntity } from "../common";

/** 邮件配置 */
export interface SysMailConfig extends BaseEntity {
  /** 配置ID */
  configId?: number;
  /** SMTP服务器 */
  host?: string;
  /** SMTP端口 */
  port?: number;
  /** SMTP用户名 */
  username?: string;
  /** SMTP密码，仅提交时使用 */
  password?: string;
  /** 发件邮箱 */
  fromEmail?: string;
  /** 发件人名称 */
  fromName?: string;
  /** 是否启用SSL */
  sslEnable?: 'Y' | 'N';
  /** 是否启用STARTTLS */
  starttlsEnable?: 'Y' | 'N';
  /** 是否启用认证 */
  authEnable?: 'Y' | 'N';
  /** 是否启用配置 */
  enabled?: 'Y' | 'N';
  /** 超时时间（毫秒） */
  timeout?: number;
  /** 是否已配置密码 */
  hasPassword?: boolean;
  /** 密码掩码 */
  passwordMask?: string;
  /** 邮箱验证开关（来源于邮件配置表） */
  emailVerifyEnabled?: 'Y' | 'N';
}

/** 测试邮件提交信息 */
export interface MailTestForm {
  email: string;
}
