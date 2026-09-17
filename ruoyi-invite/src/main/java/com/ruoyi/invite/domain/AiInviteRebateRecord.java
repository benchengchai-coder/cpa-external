package com.ruoyi.invite.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 邀请返利流水对象 ai_invite_rebate_record
 * <p>
 * 记录每一笔返利的产生与领取，支持审计与防重。
 */
public class AiInviteRebateRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 流水ID */
    private Long recordId;

    /** 收益人（邀请人）用户ID */
    private Long inviterId;

    /** 收益人账号（关联sys_user查询字段） */
    private String inviterUserName;

    /** 被邀请人用户ID（产生返利时记录） */
    private Long inviteeId;

    /** 被邀请人账号（关联sys_user查询字段） */
    private String inviteeUserName;

    /** 动作（1产生返利 2领取返利） */
    private String action;

    /** 金额 */
    private BigDecimal amount;

    /** 返利来源（1在线支付 2兑换码） */
    private String sourceType;

    /** 来源ID（充值记录ID或订单ID） */
    private Long sourceId;

    /** 领取后余额快照 */
    private BigDecimal balanceAfter;

    /** 冻结到期时间（NULL=已解冻或从未冻结） */
    private java.util.Date frozenUntil;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
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

    public Long getInviteeId()
    {
        return inviteeId;
    }

    public void setInviteeId(Long inviteeId)
    {
        this.inviteeId = inviteeId;
    }

    public String getInviteeUserName()
    {
        return inviteeUserName;
    }

    public void setInviteeUserName(String inviteeUserName)
    {
        this.inviteeUserName = inviteeUserName;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getSourceType()
    {
        return sourceType;
    }

    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }

    public Long getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(Long sourceId)
    {
        this.sourceId = sourceId;
    }

    public BigDecimal getBalanceAfter()
    {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter)
    {
        this.balanceAfter = balanceAfter;
    }

    public java.util.Date getFrozenUntil()
    {
        return frozenUntil;
    }

    public void setFrozenUntil(java.util.Date frozenUntil)
    {
        this.frozenUntil = frozenUntil;
    }
}
