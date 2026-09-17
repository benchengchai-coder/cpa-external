package com.ruoyi.invite.domain.vo;

import java.math.BigDecimal;

/**
 * 管理员邀请返利参数视图。
 */
public class InviteConfigVO
{
    /** 邀请返利总开关 */
    private boolean enabled;

    /** 全局返利比例（百分比） */
    private BigDecimal rebateRate;

    /** 返利冻结期（小时） */
    private int freezeHours;

    /** 返利有效期（天） */
    private int durationDays;

    /** 单个被邀请人累计返利上限（美元，0表示无上限） */
    private BigDecimal perInviteeCap;

    public InviteConfigVO()
    {
    }

    public InviteConfigVO(boolean enabled, BigDecimal rebateRate, int freezeHours,
                          int durationDays, BigDecimal perInviteeCap)
    {
        this.enabled = enabled;
        this.rebateRate = rebateRate;
        this.freezeHours = freezeHours;
        this.durationDays = durationDays;
        this.perInviteeCap = perInviteeCap;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public BigDecimal getRebateRate()
    {
        return rebateRate;
    }

    public void setRebateRate(BigDecimal rebateRate)
    {
        this.rebateRate = rebateRate;
    }

    public int getFreezeHours()
    {
        return freezeHours;
    }

    public void setFreezeHours(int freezeHours)
    {
        this.freezeHours = freezeHours;
    }

    public int getDurationDays()
    {
        return durationDays;
    }

    public void setDurationDays(int durationDays)
    {
        this.durationDays = durationDays;
    }

    public BigDecimal getPerInviteeCap()
    {
        return perInviteeCap;
    }

    public void setPerInviteeCap(BigDecimal perInviteeCap)
    {
        this.perInviteeCap = perInviteeCap;
    }
}
