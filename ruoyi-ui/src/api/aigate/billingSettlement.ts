import request from '@/utils/request'
import type {
  AiBillingSettlementFailed,
  AiBillingSettlementFailedQuery,
  AjaxResult,
  TableDataInfo
} from '@/types'

/** 查询待处置的结算失败任务。 */
export function listFailedBillingSettlements(
  query: AiBillingSettlementFailedQuery
): Promise<TableDataInfo<AiBillingSettlementFailed[]>> {
  return request({
    url: '/aigate/billing/settlement/failed/list',
    method: 'get',
    params: query
  })
}

/** 查询待处置的结算失败任务数量。 */
export function countFailedBillingSettlements(): Promise<AjaxResult<number>> {
  return request({
    url: '/aigate/billing/settlement/failed/count',
    method: 'get'
  })
}

/** 将失败任务重新放回正常结算队列。 */
export function retryFailedBillingSettlement(requestId: string): Promise<AjaxResult> {
  return request({
    url: `/aigate/billing/settlement/${encodeURIComponent(requestId)}/retry`,
    method: 'post'
  })
}

/** 明确放弃追收并释放该请求的预占额度。 */
export function writeOffFailedBillingSettlement(requestId: string, reason: string): Promise<AjaxResult> {
  return request({
    url: `/aigate/billing/settlement/${encodeURIComponent(requestId)}/write-off`,
    method: 'post',
    data: { reason }
  })
}
