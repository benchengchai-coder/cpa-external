import request from '@/utils/request'
import type { AjaxResult, MailTestForm, SysMailConfig } from '@/types'

// 查询邮件配置
export function getMailConfig(): Promise<AjaxResult<SysMailConfig>> {
  return request({
    url: '/system/mail/config',
    method: 'get'
  })
}

// 修改邮件配置
export function updateMailConfig(data: SysMailConfig): Promise<AjaxResult> {
  return request({
    url: '/system/mail/config',
    method: 'put',
    data: data
  })
}

// 发送测试邮件
export function testMailConfig(data: MailTestForm): Promise<AjaxResult> {
  return request({
    url: '/system/mail/config/test',
    method: 'post',
    data: data
  })
}
