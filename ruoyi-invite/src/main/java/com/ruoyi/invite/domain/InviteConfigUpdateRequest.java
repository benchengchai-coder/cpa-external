package com.ruoyi.invite.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 管理员更新邀请返利参数请求。
 */
public class InviteConfigUpdateRequest
{
    /** 邀请返利总开关 */
    @NotNull(message = "邀请返利总开关不能为空")
    private Boolean enabled;

    /** 全局返利比例（百分比） */
    @NotNull(message = "全局返利比例不能为空")
    @DecimalMin(value = "0", message = "全局返利比例不能小于0")
    @DecimalMax(value = "100", message = "全局返利比例不能大于100")
    private BigDecimal rebateRate;

    /** 返利冻结期（小时） */
    @NotNull(message = "返利冻结期不能为空")
    @Min(value = 0, message = "返利冻结期不能小于0小时")
    @Max(value = AiInviteConstants.MAX_FREEZE_HOURS, message = "返利冻结期不能大于720小时")
    private Integer freezeHours;

    /** 返利有效期（天） */
    @NotNull(message = "返利有效期不能为空")
    @Min(value = 0, message = "返利有效期不能小于0天")
    @Max(value = AiInviteConstants.MAX_DURATION_DAYS, message = "返利有效期不能大于3650天")
    private Integer durationDays;

    /** 单个被邀请人累计返利上限（美元，0表示无上限） */
    @NotNull(message = "单人返利上限不能为空")
    @DecimalMin(value = "0", message = "单人返利上限不能小于0")
    private BigDecimal perInviteeCap;

    public Boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Boolean enabled)
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

    public Integer getFreezeHours()
    {
        return freezeHours;
    }

    public void setFreezeHours(Integer freezeHours)
    {
        this.freezeHours = freezeHours;
    }

    public Integer getDurationDays()
    {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays)
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
