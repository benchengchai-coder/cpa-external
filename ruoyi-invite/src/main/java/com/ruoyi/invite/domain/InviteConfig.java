package com.ruoyi.invite.domain;

import java.math.BigDecimal;

/**
 * 邀请返利运行时配置（值对象）。
 * <p>
 * 由聚合层（facade）从系统参数读取后组装传入，邀请域本身不读取配置，
 * 从而避免依赖 ruoyi-system 的 {@code ISysConfigService}。
 */
public class InviteConfig
{
    /** 总开关 */
    private final boolean enabled;

    /** 全局返利比例（百分比） */
    private final BigDecimal globalRebateRate;

    /** 冻结期（小时） */
    private final int freezeHours;

    /** 有效期（天） */
    private final int durationDays;

    /** 单人累计上限（美元，0 表示无上限） */
    private final BigDecimal perInviteeCap;

    public InviteConfig(boolean enabled, BigDecimal globalRebateRate, int freezeHours,
                        int durationDays, BigDecimal perInviteeCap)
    {
        this.enabled = enabled;
        this.globalRebateRate = globalRebateRate;
        this.freezeHours = freezeHours;
        this.durationDays = durationDays;
        this.perInviteeCap = perInviteeCap;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public BigDecimal getGlobalRebateRate()
    {
        return globalRebateRate;
    }

    public int getFreezeHours()
    {
        return freezeHours;
    }

    public int getDurationDays()
    {
        return durationDays;
    }

    public BigDecimal getPerInviteeCap()
    {
        return perInviteeCap;
    }
}
