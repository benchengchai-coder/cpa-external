package com.ruoyi.redeem.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI兑换码对象 ai_redemption_code
 */
public class AiRedemptionCode extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long codeId;

    private String codeName;

    private BigDecimal quota;

    private String redemptionKey;

    private String status;

    private Date expiredTime;

    private Long usedUserId;

    private String usedUsername;

    private Date redeemedTime;

    /** 批量生成数量（非数据库字段） */
    private Integer batchCount;

    public Long getCodeId()
    {
        return codeId;
    }

    public void setCodeId(Long codeId)
    {
        this.codeId = codeId;
    }

    public String getCodeName()
    {
        return codeName;
    }

    public void setCodeName(String codeName)
    {
        this.codeName = codeName;
    }

    public BigDecimal getQuota()
    {
        return quota;
    }

    public void setQuota(BigDecimal quota)
    {
        this.quota = quota;
    }

    public String getRedemptionKey()
    {
        return redemptionKey;
    }

    public void setRedemptionKey(String redemptionKey)
    {
        this.redemptionKey = redemptionKey;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getExpiredTime()
    {
        return expiredTime;
    }

    public void setExpiredTime(Date expiredTime)
    {
        this.expiredTime = expiredTime;
    }

    public Long getUsedUserId()
    {
        return usedUserId;
    }

    public void setUsedUserId(Long usedUserId)
    {
        this.usedUserId = usedUserId;
    }

    public String getUsedUsername()
    {
        return usedUsername;
    }

    public void setUsedUsername(String usedUsername)
    {
        this.usedUsername = usedUsername;
    }

    public Date getRedeemedTime()
    {
        return redeemedTime;
    }

    public void setRedeemedTime(Date redeemedTime)
    {
        this.redeemedTime = redeemedTime;
    }

    public Integer getBatchCount()
    {
        return batchCount;
    }

    public void setBatchCount(Integer batchCount)
    {
        this.batchCount = batchCount;
    }
}
