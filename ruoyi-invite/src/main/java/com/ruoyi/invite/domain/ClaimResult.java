package com.ruoyi.invite.domain;

import java.math.BigDecimal;

/**
 * 领取返利结果（纯域返回给聚合层）。
 * <p>
 * 邀请域领取返利后，把领取金额与领取流水ID 交给聚合层，
 * 由聚合层完成「加余额 + 写账户流水 type=6」。
 */
public class ClaimResult
{
    /** 邀请人用户ID */
    private final Long inviterId;

    /** 实际领取金额 */
    private final BigDecimal amount;

    /** 领取返利流水ID（用于账户流水 sourceId 关联） */
    private final Long rebateRecordId;

    public ClaimResult(Long inviterId, BigDecimal amount, Long rebateRecordId)
    {
        this.inviterId = inviterId;
        this.amount = amount;
        this.rebateRecordId = rebateRecordId;
    }

    public Long getInviterId()
    {
        return inviterId;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public Long getRebateRecordId()
    {
        return rebateRecordId;
    }
}
