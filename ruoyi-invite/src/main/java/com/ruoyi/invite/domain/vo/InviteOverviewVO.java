package com.ruoyi.invite.domain.vo;

import java.math.BigDecimal;

/**
 * 邀请返利概览（管理员视角单用户）
 */
public class InviteOverviewVO
{
    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 邀请码 */
    private String inviteCode;

    /** 邀请人用户ID */
    private Long inviterId;

    /** 累计邀请人数 */
    private Integer inviteCount;

    /** 待领取返利额度 */
    private BigDecimal pendingRebate;

    /** 冻结中返利额度 */
    private BigDecimal frozenRebate;

    /** 历史累计返利 */
    private BigDecimal historyRebate;

    /** 专属返利比例（NULL=沿用全局） */
    private BigDecimal rebateRate;

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

    public String getInviteCode()
    {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode)
    {
        this.inviteCode = inviteCode;
    }

    public Long getInviterId()
    {
        return inviterId;
    }

    public void setInviterId(Long inviterId)
    {
        this.inviterId = inviterId;
    }

    public Integer getInviteCount()
    {
        return inviteCount;
    }

    public void setInviteCount(Integer inviteCount)
    {
        this.inviteCount = inviteCount;
    }

    public BigDecimal getPendingRebate()
    {
        return pendingRebate;
    }

    public void setPendingRebate(BigDecimal pendingRebate)
    {
        this.pendingRebate = pendingRebate;
    }

    public BigDecimal getFrozenRebate()
    {
        return frozenRebate;
    }

    public void setFrozenRebate(BigDecimal frozenRebate)
    {
        this.frozenRebate = frozenRebate;
    }

    public BigDecimal getHistoryRebate()
    {
        return historyRebate;
    }

    public void setHistoryRebate(BigDecimal historyRebate)
    {
        this.historyRebate = historyRebate;
    }

    public BigDecimal getRebateRate()
    {
        return rebateRate;
    }

    public void setRebateRate(BigDecimal rebateRate)
    {
        this.rebateRate = rebateRate;
    }
}
