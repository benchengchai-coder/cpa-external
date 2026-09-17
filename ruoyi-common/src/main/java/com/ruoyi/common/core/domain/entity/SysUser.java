package com.ruoyi.common.core.domain.entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import jakarta.validation.constraints.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.annotation.Excel.Type;
import com.ruoyi.common.annotation.Excels;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.xss.Xss;

/**
 * 用户对象 sys_user
 * 
 * @author ruoyi
 */
public class SysUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Excel(name = "用户序号", type = Type.EXPORT, cellType = ColumnType.NUMERIC, prompt = "用户编号")
    private Long userId;

    /** 部门ID */
    @Excel(name = "部门编号", type = Type.IMPORT)
    private Long deptId;

    /** 用户账号 */
    @Excel(name = "登录名称")
    private String userName;

    /** 用户昵称 */
    @Excel(name = "用户名称")
    private String nickName;

    /** 用户邮箱 */
    @Excel(name = "用户邮箱")
    private String email;

    /** 手机号码 */
    @Excel(name = "手机号码", cellType = ColumnType.TEXT)
    private String phonenumber;

    /** 用户性别 */
    @Excel(name = "用户性别", readConverterExp = "0=男,1=女,2=未知")
    private String sex;

    /** 用户头像 */
    private String avatar;

    /** 密码 */
    private String password;

    /** 账号状态（0正常 1停用） */
    @Excel(name = "账号状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 账户余额（美元） */
    private BigDecimal balance;

    /** 已冻结余额（美元） */
    private BigDecimal frozenBalance;

    /** 已用金额（美元） */
    private BigDecimal usedBalance;

    /** 请求次数 */
    private Integer requestCount;

    /** 用户计费倍率 */
    @DecimalMin(value = "0.00", message = "用户计费倍率不能小于0")
    @Digits(integer = 8, fraction = 2, message = "用户计费倍率最多支持8位整数和2位小数")
    private BigDecimal billingMultiplier;

    /** AI扣费偏好 */
    private String billingPreference;

    /** AI并发上限（0表示禁用AI中继） */
    private Integer aiConcurrencyLimit;

    /** AI在途并发计数（等于reserved状态账单数，随计费事务原子维护） */
    private Integer activeRequestCount;

    /** 当前生效订阅ID */
    private Long activeSubscriptionId;

    /** 当前生效订阅套餐名称 */
    private String activeSubscriptionTitle;

    /** 当前生效订阅总额度 */
    private BigDecimal activeSubscriptionAmountTotal;

    /** 当前生效订阅已用额度 */
    private BigDecimal activeSubscriptionAmountUsed;

    /** 当前生效订阅到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date activeSubscriptionEndTime;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 最后登录IP */
    @Excel(name = "最后登录IP", type = Type.EXPORT)
    private String loginIp;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后登录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Type.EXPORT)
    private Date loginDate;

    /** 密码最后更新时间 */
    private Date pwdUpdateDate;

    /** 部门对象 */
    @Excels({
        @Excel(name = "部门名称", targetAttr = "deptName", type = Type.EXPORT),
        @Excel(name = "部门负责人", targetAttr = "leader", type = Type.EXPORT)
    })
    private SysDept dept;

    /** 角色对象 */
    private List<SysRole> roles;

    /** 角色组 */
    private Long[] roleIds;

    /** 岗位组 */
    private Long[] postIds;

    /** 角色ID */
    private Long roleId;

    public SysUser()
    {

    }

    public SysUser(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public boolean isAdmin()
    {
        return SecurityUtils.isAdmin(this.userId);
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过30个字符")
    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    @Xss(message = "用户账号不能包含脚本字符")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过30个字符")
    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    @Size(min = 0, max = 11, message = "手机号码长度不能超过11个字符")
    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getSex()
    {
        return sex;
    }

    public void setSex(String sex)
    {
        this.sex = sex;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public BigDecimal getUsedBalance()
    {
        return usedBalance;
    }

    public void setUsedBalance(BigDecimal usedBalance)
    {
        this.usedBalance = usedBalance;
    }

    public Integer getRequestCount()
    {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount)
    {
        this.requestCount = requestCount;
    }

    public BigDecimal getBillingMultiplier()
    {
        return billingMultiplier;
    }

    public void setBillingMultiplier(BigDecimal billingMultiplier)
    {
        this.billingMultiplier = billingMultiplier;
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

    public Integer getActiveRequestCount()
    {
        return activeRequestCount;
    }

    public void setActiveRequestCount(Integer activeRequestCount)
    {
        this.activeRequestCount = activeRequestCount;
    }

    public Long getActiveSubscriptionId()
    {
        return activeSubscriptionId;
    }

    public void setActiveSubscriptionId(Long activeSubscriptionId)
    {
        this.activeSubscriptionId = activeSubscriptionId;
    }

    public String getActiveSubscriptionTitle()
    {
        return activeSubscriptionTitle;
    }

    public void setActiveSubscriptionTitle(String activeSubscriptionTitle)
    {
        this.activeSubscriptionTitle = activeSubscriptionTitle;
    }

    public BigDecimal getActiveSubscriptionAmountTotal()
    {
        return activeSubscriptionAmountTotal;
    }

    public void setActiveSubscriptionAmountTotal(BigDecimal activeSubscriptionAmountTotal)
    {
        this.activeSubscriptionAmountTotal = activeSubscriptionAmountTotal;
    }

    public BigDecimal getActiveSubscriptionAmountUsed()
    {
        return activeSubscriptionAmountUsed;
    }

    public void setActiveSubscriptionAmountUsed(BigDecimal activeSubscriptionAmountUsed)
    {
        this.activeSubscriptionAmountUsed = activeSubscriptionAmountUsed;
    }

    public Date getActiveSubscriptionEndTime()
    {
        return activeSubscriptionEndTime;
    }

    public void setActiveSubscriptionEndTime(Date activeSubscriptionEndTime)
    {
        this.activeSubscriptionEndTime = activeSubscriptionEndTime;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getLoginIp()
    {
        return loginIp;
    }

    public void setLoginIp(String loginIp)
    {
        this.loginIp = loginIp;
    }

    public Date getLoginDate()
    {
        return loginDate;
    }

    public void setLoginDate(Date loginDate)
    {
        this.loginDate = loginDate;
    }

    public Date getPwdUpdateDate()
    {
        return pwdUpdateDate;
    }

    public void setPwdUpdateDate(Date pwdUpdateDate)
    {
        this.pwdUpdateDate = pwdUpdateDate;
    }

    public SysDept getDept()
    {
        return dept;
    }

    public void setDept(SysDept dept)
    {
        this.dept = dept;
    }

    public List<SysRole> getRoles()
    {
        return roles;
    }

    public void setRoles(List<SysRole> roles)
    {
        this.roles = roles;
    }

    public Long[] getRoleIds()
    {
        return roleIds;
    }

    public void setRoleIds(Long[] roleIds)
    {
        this.roleIds = roleIds;
    }

    public Long[] getPostIds()
    {
        return postIds;
    }

    public void setPostIds(Long[] postIds)
    {
        this.postIds = postIds;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("deptId", getDeptId())
            .append("userName", getUserName())
            .append("nickName", getNickName())
            .append("email", getEmail())
            .append("phonenumber", getPhonenumber())
            .append("sex", getSex())
            .append("avatar", getAvatar())
            .append("password", getPassword())
            .append("status", getStatus())
            .append("balance", getBalance())
            .append("frozenBalance", getFrozenBalance())
            .append("usedBalance", getUsedBalance())
            .append("requestCount", getRequestCount())
            .append("billingMultiplier", getBillingMultiplier())
            .append("billingPreference", getBillingPreference())
            .append("aiConcurrencyLimit", getAiConcurrencyLimit())
            .append("activeRequestCount", getActiveRequestCount())
            .append("activeSubscriptionId", getActiveSubscriptionId())
            .append("activeSubscriptionTitle", getActiveSubscriptionTitle())
            .append("activeSubscriptionAmountTotal", getActiveSubscriptionAmountTotal())
            .append("activeSubscriptionAmountUsed", getActiveSubscriptionAmountUsed())
            .append("activeSubscriptionEndTime", getActiveSubscriptionEndTime())
            .append("delFlag", getDelFlag())
            .append("loginIp", getLoginIp())
            .append("loginDate", getLoginDate())
            .append("pwdUpdateDate", getPwdUpdateDate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .append("dept", getDept())
            .toString();
    }
}
