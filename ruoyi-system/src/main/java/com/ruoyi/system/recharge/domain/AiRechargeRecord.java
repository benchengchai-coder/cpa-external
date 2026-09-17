package com.ruoyi.system.recharge.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI充值记录对象 ai_recharge_record
 */
public class AiRechargeRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;

    private Long userId;

    private String username;

    /** 充值类型（1兑换码 2在线支付 3管理员调整 4新用户注册 5每日签到 6邀请返利） */
    private String type;

    /** 充值金额（美元） */
    private BigDecimal amount;

    /** 来源ID（兑换码ID、支付订单ID或业务流水ID） */
    private Long sourceId;

    /** 来源名称（兑换码名称、支付方式或业务来源） */
    private String sourceName;

    /** 状态（0成功 1失败） */
    private String status;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

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

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public Long getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(Long sourceId)
    {
        this.sourceId = sourceId;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public void setSourceName(String sourceName)
    {
        this.sourceName = sourceName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
