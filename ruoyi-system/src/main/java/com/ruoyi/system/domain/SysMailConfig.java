package com.ruoyi.system.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 邮件配置对象 sys_mail_config
 */
public class SysMailConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    @Excel(name = "配置ID")
    private Long configId;

    /** SMTP服务器 */
    @Excel(name = "SMTP服务器")
    private String host;

    /** SMTP端口 */
    @Excel(name = "SMTP端口")
    private Integer port;

    /** SMTP用户名 */
    @Excel(name = "SMTP用户名")
    private String username;

    /** SMTP密码，仅用于写入 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 加密后的SMTP密码 */
    @JsonIgnore
    private String encryptedPassword;

    /** 发件邮箱 */
    @Excel(name = "发件邮箱")
    private String fromEmail;

    /** 发件人名称 */
    @Excel(name = "发件人名称")
    private String fromName;

    /** 是否启用SSL */
    private String sslEnable;

    /** 是否启用STARTTLS */
    private String starttlsEnable;

    /** 是否启用认证 */
    private String authEnable;

    /** 是否启用配置 */
    private String enabled;

    /** 超时时间（毫秒） */
    private Integer timeout;

    /** 是否已配置密码 */
    private Boolean hasPassword;

    /** 密码掩码 */
    private String passwordMask;

    /** 邮箱验证开关（非数据库字段，来源于sys_config） */
    private String emailVerifyEnabled;

    public Long getConfigId()
    {
        return configId;
    }

    public void setConfigId(Long configId)
    {
        this.configId = configId;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getEncryptedPassword()
    {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword)
    {
        this.encryptedPassword = encryptedPassword;
    }

    public String getFromEmail()
    {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail)
    {
        this.fromEmail = fromEmail;
    }

    public String getFromName()
    {
        return fromName;
    }

    public void setFromName(String fromName)
    {
        this.fromName = fromName;
    }

    public String getSslEnable()
    {
        return sslEnable;
    }

    public void setSslEnable(String sslEnable)
    {
        this.sslEnable = sslEnable;
    }

    public String getStarttlsEnable()
    {
        return starttlsEnable;
    }

    public void setStarttlsEnable(String starttlsEnable)
    {
        this.starttlsEnable = starttlsEnable;
    }

    public String getAuthEnable()
    {
        return authEnable;
    }

    public void setAuthEnable(String authEnable)
    {
        this.authEnable = authEnable;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }

    public Integer getTimeout()
    {
        return timeout;
    }

    public void setTimeout(Integer timeout)
    {
        this.timeout = timeout;
    }

    public Boolean getHasPassword()
    {
        return hasPassword;
    }

    public void setHasPassword(Boolean hasPassword)
    {
        this.hasPassword = hasPassword;
    }

    public String getPasswordMask()
    {
        return passwordMask;
    }

    public void setPasswordMask(String passwordMask)
    {
        this.passwordMask = passwordMask;
    }

    public String getEmailVerifyEnabled()
    {
        return emailVerifyEnabled;
    }

    public void setEmailVerifyEnabled(String emailVerifyEnabled)
    {
        this.emailVerifyEnabled = emailVerifyEnabled;
    }
}
