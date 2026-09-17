package com.ruoyi.invite.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 邀请返利关系对象 ai_invite_affiliate
 * <p>
 * 与 sys_user 一对一，记录用户的邀请码、邀请人、累计邀请人数及返利额度。
 */
public class AiInviteAffiliate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID（主键，关联 sys_user.user_id） */
    private Long userId;

    /** 用户名 */
    private String userName;

    /** 专属邀请码 */
    private String inviteCode;

    /** 邀请人用户ID（绑定一次不可改） */
    private Long inviterId;

    /** 邀请人用户名 */
    private String inviterUserName;

    /** 累计邀请人数 */
    private Integer inviteCount;

    /** 待领取返利额度 */
    private BigDecimal pendingRebate;

    /** 冻结中返利额度 */
    private BigDecimal frozenRebate;

    /** 历史累计返利（只增不减） */
    private BigDecimal historyRebate;

    /** 专属返利比例（百分比，NULL=沿用全局） */
    private BigDecimal rebateRate;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
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

    public String getInviterUserName()
    {
        return inviterUserName;
    }

    public void setInviterUserName(String inviterUserName)
    {
        this.inviterUserName = inviterUserName;
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
