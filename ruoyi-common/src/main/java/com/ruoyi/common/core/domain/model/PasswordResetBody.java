package com.ruoyi.common.core.domain.model;

/**
 * 密码重置请求体。
 */
public class PasswordResetBody
{
    /** 用户账号 */
    private String username;

    /** 绑定邮箱 */
    private String email;

    /** 邮箱验证码 */
    private String emailCode;

    /** 新密码 */
    private String newPassword;

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

    public String getEmailCode()
    {
        return emailCode;
    }

    public void setEmailCode(String emailCode)
    {
        this.emailCode = emailCode;
    }

    public String getNewPassword()
    {
        return newPassword;
    }

    public void setNewPassword(String newPassword)
    {
        this.newPassword = newPassword;
    }
}
