package com.ruoyi.checkin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import com.ruoyi.checkin.domain.CheckinResult;
import com.ruoyi.checkin.domain.vo.CheckinCalendarVO;

/**
 * 每日签到 服务层（纯签到域）。
 * <p>
 * 本接口仅依赖 ruoyi-common，不操作用户余额、不写账户流水；
 * 加余额、回填余额快照、写账户流水 type=5 由聚合层（CheckinFacade）编排。
 */
public interface IAiCheckinService
{
    /**
     * 查询签到日历（纯读，无副作用）。
     */
    public CheckinCalendarVO getCalendar(Long userId, YearMonth month);

    /**
     * 执行今日签到（纯域：只插入签到记录，返回结果给聚合层）。
     * <p>
     * 不在此加余额、不写账户流水 —— 由聚合层基于 {@link CheckinResult} 编排。
     * @return 签到结果（checkinId + 奖励 + 已签到日期）
     */
    public CheckinResult checkin(Long userId, String username, LocalDate requestDate, boolean hasActiveSubscription);

    /**
     * 回填签到记录的余额快照（聚合层加完余额后调用，保留审计信息）。
     */
    public void updateBalanceAfter(Long checkinId, BigDecimal balanceAfter);
}
