package com.ruoyi.common.core.domain.model;

/**
 * 用户注册成功事件。
 * <p>
 * 由注册主流程（{@code SysRegisterService}）在用户落库后发布，
 * 业务域（邀请返利、注册赠送额度等）通过 {@code @EventListener} 监听并各自处理，
 * 使 framework 层无需反向依赖业务模块。
 * <p>
 * 监听方应自行 try/catch，确保异常不阻断注册主流程。
 */
public class UserRegisteredEvent
{
    /** 新注册用户ID */
    private final Long userId;

    /** 用户名 */
    private final String username;

    /** 注册时填写的邀请码（可为空） */
    private final String inviteCode;

    public UserRegisteredEvent(Long userId, String username, String inviteCode)
    {
        this.userId = userId;
        this.username = username;
        this.inviteCode = inviteCode;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getUsername()
    {
        return username;
    }

    public String getInviteCode()
    {
        return inviteCode;
    }
}
