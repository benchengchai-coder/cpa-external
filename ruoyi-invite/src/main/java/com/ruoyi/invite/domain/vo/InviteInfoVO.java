package com.ruoyi.invite.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 邀请返利信息（用户视角聚合对象）
 */
public class InviteInfoVO
{
    /** 邀请码 */
    private String inviteCode;

    /** 邀请链接 */
    private String inviteLink;

    /** 生效返利比例（百分比，专属优先全局，已 clamp 到 0-100） */
    private BigDecimal effectiveRebateRate;

    /** 累计邀请人数 */
    private Integer inviteCount;

    /** 待领取返利额度 */
    private BigDecimal pendingRebate;

    /** 冻结中返利额度 */
    private BigDecimal frozenRebate;

    /** 历史累计返利 */
    private BigDecimal historyRebate;

    /** 被邀请人列表（最多100条） */
    private List<InviteeVO> invitees;

    public String getInviteCode()
    {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode)
    {
        this.inviteCode = inviteCode;
    }

    public String getInviteLink()
    {
        return inviteLink;
    }

    public void setInviteLink(String inviteLink)
    {
        this.inviteLink = inviteLink;
    }

    public BigDecimal getEffectiveRebateRate()
    {
        return effectiveRebateRate;
    }

    public void setEffectiveRebateRate(BigDecimal effectiveRebateRate)
    {
        this.effectiveRebateRate = effectiveRebateRate;
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

    public List<InviteeVO> getInvitees()
    {
        return invitees;
    }

    public void setInvitees(List<InviteeVO> invitees)
    {
        this.invitees = invitees;
    }
}
