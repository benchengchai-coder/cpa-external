package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.SysMailConfig;

/**
 * 邮件配置 数据层
 */
public interface SysMailConfigMapper
{
    /**
     * 查询全局邮件配置
     *
     * @return 邮件配置
     */
    public SysMailConfig selectMailConfig();

    /**
     * 新增邮件配置
     *
     * @param config 邮件配置
     * @return 结果
     */
    public int insertMailConfig(SysMailConfig config);

    /**
     * 修改邮件配置
     *
     * @param config 邮件配置
     * @return 结果
     */
    public int updateMailConfig(SysMailConfig config);
}
