import { isHttp } from '@/utils/validate'

/**
 * 将头像存储地址转换为浏览器可访问地址。
 * 本地存储使用 /profile/** 相对路径，统一经 VITE_APP_BASE_API 转发；
 * Minio 等外部存储保留完整 HTTP(S) 地址。
 */
export function resolveAvatarUrl(avatar: string): string {
  if (isHttp(avatar)) {
    return avatar
  }
  const baseApi = import.meta.env.VITE_APP_BASE_API.replace(/\/+$/, '')
  const avatarPath = avatar.startsWith('/') ? avatar : `/${avatar}`
  return baseApi + avatarPath
}
