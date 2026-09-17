package com.ruoyi.system.service;

import com.ruoyi.system.domain.SysMailConfig;

/**
 * 邮件配置 服务层
 */
public interface ISysMailConfigService
{
    /**
     * 查询邮件配置
     *
     * @return 邮件配置
     */
    public SysMailConfig selectMailConfig();

    /**
     * 查询启用的邮件配置
     *
     * @return 邮件配置
     */
    public SysMailConfig selectEnabledMailConfig();

    /**
     * 判断邮件服务是否已启用且发送配置完整
     *
     * @return true可用，false不可用
     */
    public boolean isMailServiceAvailable();

    /**
     * 保存邮件配置
     *
     * @param config 邮件配置
     * @return 结果
     */
    public int saveMailConfig(SysMailConfig config);

    /**
     * 解密邮件密码
     *
     * @param config 邮件配置
     * @return 明文密码
     */
    public String decryptPassword(SysMailConfig config);

    /**
     * 获取邮箱验证开关
     *
     * @return true开启，false关闭
     */
    public boolean selectEmailVerifyEnabled();
}
