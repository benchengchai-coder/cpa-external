package com.ruoyi.redeem.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 兑换码核销结果（纯域返回给聚合层）。
 * <p>
 * 兑换域只标记码已用，把结果交给聚合层，
 * 由聚合层完成「加余额 + 写账户流水 type=1 + 触发返利」。
 */
public class RedeemResult
{
    /** 兑换用户ID */
    private final Long userId;

    /** 用户名 */
    private final String username;

    /** 兑换额度 */
    private final BigDecimal quota;

    /** 兑换码ID（用于账户流水 sourceId 关联） */
    private final Long codeId;

    /** 兑换码名称 */
    private final String codeName;

    /** 核销时间 */
    private final Date redeemedTime;

    public RedeemResult(Long userId, String username, BigDecimal quota, Long codeId, String codeName, Date redeemedTime)
    {
        this.userId = userId;
        this.username = username;
        this.quota = quota;
        this.codeId = codeId;
        this.codeName = codeName;
        this.redeemedTime = redeemedTime;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getUsername()
    {
        return username;
    }

    public BigDecimal getQuota()
    {
        return quota;
    }

    public Long getCodeId()
    {
        return codeId;
    }

    public String getCodeName()
    {
        return codeName;
    }

    public Date getRedeemedTime()
    {
        return redeemedTime;
    }
}
