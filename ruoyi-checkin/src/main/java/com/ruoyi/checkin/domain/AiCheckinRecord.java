package com.ruoyi.checkin.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 每日签到记录对象 ai_checkin_record
 */
public class AiCheckinRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 签到记录ID */
    private Long checkinId;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 签到日期 */
    private LocalDate checkinDate;

    /** 奖励金额（美元） */
    private BigDecimal rewardAmount;

    /** 签到后余额快照 */
    private BigDecimal balanceAfter;

    /** 状态（0成功） */
    private String status;

    public Long getCheckinId()
    {
        return checkinId;
    }

    public void setCheckinId(Long checkinId)
    {
        this.checkinId = checkinId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public LocalDate getCheckinDate()
    {
        return checkinDate;
    }

    public void setCheckinDate(LocalDate checkinDate)
    {
        this.checkinDate = checkinDate;
    }

    public BigDecimal getRewardAmount()
    {
        return rewardAmount;
    }

    public void setRewardAmount(BigDecimal rewardAmount)
    {
        this.rewardAmount = rewardAmount;
    }

    public BigDecimal getBalanceAfter()
    {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter)
    {
        this.balanceAfter = balanceAfter;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
