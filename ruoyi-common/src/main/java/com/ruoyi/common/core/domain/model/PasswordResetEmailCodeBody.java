package com.ruoyi.common.core.domain.model;

/**
 * 密码重置邮箱验证码请求体。
 */
public class PasswordResetEmailCodeBody
{
    /** 用户账号 */
    private String username;

    /** 绑定邮箱 */
    private String email;

    /** 图形验证码 */
    private String code;

    /** 图形验证码唯一标识 */
    private String uuid;

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

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }
}
