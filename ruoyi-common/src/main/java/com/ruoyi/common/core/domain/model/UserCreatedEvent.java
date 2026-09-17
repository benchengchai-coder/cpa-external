package com.ruoyi.common.core.domain.model;

/**
 * 用户创建完成事件。
 * <p>
 * 后台新增、公开注册和用户导入统一发布此事件，业务模块可在同一事务中
 * 初始化用户必备资源。监听器异常会回滚本次用户创建，避免产生不完整账号。
 */
public class UserCreatedEvent
{
    /** 新用户ID */
    private final Long userId;

    /** 用户名 */
    private final String username;

    public UserCreatedEvent(Long userId, String username)
    {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getUsername()
    {
        return username;
    }
}
