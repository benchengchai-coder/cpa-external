package com.ruoyi.checkin.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 签到结果视图对象。
 */
public class CheckinResultVO
{
    /** 签到日期（yyyy-MM-dd） */
    private String checkinDate;

    /** 本次奖励金额（美元） */
    private BigDecimal rewardAmount;

    /** 奖励后账户余额 */
    private BigDecimal balance;

    /** 当前月份已签到日期列表（yyyy-MM-dd） */
    private List<String> signedDates;

    /** 今天是否已签到 */
    private Boolean todaySigned;

    public String getCheckinDate()
    {
        return checkinDate;
    }

    public void setCheckinDate(String checkinDate)
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

    public BigDecimal getBalance()
    {
        return balance;
    }

    public void setBalance(BigDecimal balance)
    {
        this.balance = balance;
    }

    public List<String> getSignedDates()
    {
        return signedDates;
    }

    public void setSignedDates(List<String> signedDates)
    {
        this.signedDates = signedDates;
    }

    public Boolean getTodaySigned()
    {
        return todaySigned;
    }

    public void setTodaySigned(Boolean todaySigned)
    {
        this.todaySigned = todaySigned;
    }
}
