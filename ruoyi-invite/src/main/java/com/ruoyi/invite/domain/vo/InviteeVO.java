package com.ruoyi.invite.domain.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 被邀请人信息（用户视角，邮箱已脱敏）
 */
public class InviteeVO
{
    /** 被邀请人用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 邮箱（已脱敏） */
    private String email;

    /** 该被邀请人为我贡献的返利累计 */
    private BigDecimal totalRebate;

    /** 加入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

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

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public BigDecimal getTotalRebate()
    {
        return totalRebate;
    }

    public void setTotalRebate(BigDecimal totalRebate)
    {
        this.totalRebate = totalRebate;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
