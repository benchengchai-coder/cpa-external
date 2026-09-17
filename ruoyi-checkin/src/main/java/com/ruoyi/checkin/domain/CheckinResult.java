package com.ruoyi.checkin.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 签到结果（纯域返回给聚合层）。
 * <p>
 * 签到域只产生签到记录与奖励金额，把结果交给聚合层，
 * 由聚合层完成「加余额 + 回填余额快照 + 写账户流水 type=5」。
 */
public class CheckinResult
{
    /** 签到记录ID（用于账户流水 sourceId 关联） */
    private final Long checkinId;

    /** 本次奖励金额 */
    private final BigDecimal rewardAmount;

    /** 当前月份已签到日期列表（yyyy-MM-dd） */
    private final List<String> signedDates;

    public CheckinResult(Long checkinId, BigDecimal rewardAmount, List<String> signedDates)
    {
        this.checkinId = checkinId;
        this.rewardAmount = rewardAmount;
        this.signedDates = signedDates;
    }

    public Long getCheckinId()
    {
        return checkinId;
    }

    public BigDecimal getRewardAmount()
    {
        return rewardAmount;
    }

    public List<String> getSignedDates()
    {
        return signedDates;
    }
}
