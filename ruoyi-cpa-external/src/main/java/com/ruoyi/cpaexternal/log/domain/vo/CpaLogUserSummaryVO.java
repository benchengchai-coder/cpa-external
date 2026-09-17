package com.ruoyi.cpaexternal.log.domain.vo;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;

/**
 * 全局使用记录用户摘要
 */
public class CpaLogUserSummaryVO
{
    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 用户昵称 */
    private String nickName;

    /** 邮箱 */
    private String email;

    /** 备注 */
    private String remark;

    /** 账号状态 */
    private String status;

    /** 部门名称 */
    private String deptName;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginDate;

    /** 钱包余额 */
    private BigDecimal balance;

    /** 冻结余额 */
    private BigDecimal frozenBalance;

    /** 可用余额 */
    private BigDecimal availableBalance;

    /** 累计应计金额 */
    private BigDecimal usedBalance;

    /** 钱包与订阅累计实扣金额 */
    private BigDecimal totalChargedAmount;

    /** 累计未覆盖金额 */
    private BigDecimal totalUncoveredAmount;

    /** 请求次数 */
    private Integer requestCount;

    /** 扣费偏好 */
    private String billingPreference;

    /** AI并发上限 */
    private Integer aiConcurrencyLimit;

    /** 当前生效订阅 */
    private List<AiUserSubscription> subscriptions;

    public static CpaLogUserSummaryVO from(SysUser user, List<AiUserSubscription> subscriptions)
    {
        CpaLogUserSummaryVO summary = new CpaLogUserSummaryVO();
        BigDecimal balance = nvl(user.getBalance());
        BigDecimal frozenBalance = nvl(user.getFrozenBalance());
        BigDecimal availableBalance = balance.subtract(frozenBalance);
        SysDept dept = user.getDept();

        summary.setUserId(user.getUserId());
        summary.setUsername(user.getUserName());
        summary.setNickName(user.getNickName());
        summary.setEmail(user.getEmail());
        summary.setRemark(user.getRemark());
        summary.setStatus(user.getStatus());
        summary.setDeptName(dept == null ? null : dept.getDeptName());
        summary.setCreateTime(user.getCreateTime());
        summary.setLoginDate(user.getLoginDate());
        summary.setBalance(balance);
        summary.setFrozenBalance(frozenBalance);
        summary.setAvailableBalance(availableBalance.max(BigDecimal.ZERO));
        summary.setUsedBalance(nvl(user.getUsedBalance()));
        summary.setRequestCount(user.getRequestCount());
        summary.setBillingPreference(user.getBillingPreference());
        summary.setAiConcurrencyLimit(user.getAiConcurrencyLimit());
        summary.setSubscriptions(subscriptions == null ? Collections.emptyList() : subscriptions);
        return summary;
    }

    private static BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
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

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getLoginDate()
    {
        return loginDate;
    }

    public void setLoginDate(Date loginDate)
    {
        this.loginDate = loginDate;
    }

    public BigDecimal getBalance()
    {
        return balance;
    }

    public void setBalance(BigDecimal balance)
    {
        this.balance = balance;
    }

    public BigDecimal getFrozenBalance()
    {
        return frozenBalance;
    }

    public void setFrozenBalance(BigDecimal frozenBalance)
    {
        this.frozenBalance = frozenBalance;
    }

    public BigDecimal getAvailableBalance()
    {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance)
    {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getUsedBalance()
    {
        return usedBalance;
    }

    public void setUsedBalance(BigDecimal usedBalance)
    {
        this.usedBalance = usedBalance;
    }

    public BigDecimal getTotalChargedAmount()
    {
        return totalChargedAmount;
    }

    public void setTotalChargedAmount(BigDecimal totalChargedAmount)
    {
        this.totalChargedAmount = totalChargedAmount;
    }

    public BigDecimal getTotalUncoveredAmount()
    {
        return totalUncoveredAmount;
    }

    public void setTotalUncoveredAmount(BigDecimal totalUncoveredAmount)
    {
        this.totalUncoveredAmount = totalUncoveredAmount;
    }

    public Integer getRequestCount()
    {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount)
    {
        this.requestCount = requestCount;
    }

    public String getBillingPreference()
    {
        return billingPreference;
    }

    public void setBillingPreference(String billingPreference)
    {
        this.billingPreference = billingPreference;
    }

    public Integer getAiConcurrencyLimit()
    {
        return aiConcurrencyLimit;
    }

    public void setAiConcurrencyLimit(Integer aiConcurrencyLimit)
    {
        this.aiConcurrencyLimit = aiConcurrencyLimit;
    }

    public List<AiUserSubscription> getSubscriptions()
    {
        return subscriptions;
    }

    public void setSubscriptions(List<AiUserSubscription> subscriptions)
    {
        this.subscriptions = subscriptions;
    }
}
