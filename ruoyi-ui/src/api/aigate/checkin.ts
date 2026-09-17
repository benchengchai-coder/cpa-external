import request from "@/utils/request";
import type { AjaxResult, CheckinCalendar, CheckinResult } from "@/types";

/** 查询签到日历 */
export function getCheckinCalendar(month: string): Promise<AjaxResult<CheckinCalendar>> {
  return request({ url: "/aigate/checkin/calendar", method: "get", params: { month } });
}

/** 今日签到 */
export function dailyCheckin(checkinDate: string): Promise<AjaxResult<CheckinResult>> {
  return request({ url: "/aigate/checkin/daily", method: "post", data: { checkinDate } });
}
