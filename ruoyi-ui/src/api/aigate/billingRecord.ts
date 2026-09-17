import request from '@/utils/request'
import type { AiBillingRecordDetail, AjaxResult } from '@/types'

/** 查询当前用户指定调用日志对应的账单。 */
export function getUserBillingRecordByLogId(logId: number): Promise<AjaxResult<AiBillingRecordDetail | null>> {
  return request({
    url: `/aigate/billing/records/by-log/${logId}`,
    method: 'get'
  })
}

/** 管理员查询指定调用日志对应的账单。 */
export function getAdminBillingRecordByLogId(logId: number): Promise<AjaxResult<AiBillingRecordDetail | null>> {
  return request({
    url: `/aigate/billing/records/admin/by-log/${logId}`,
    method: 'get'
  })
}
