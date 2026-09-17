package com.ruoyi.cpaexternal.platform.domain;

import java.io.Serializable;

/**
 * AI平台对象 ai_platform
 */
public class AiPlatform implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 平台ID */
    private Long platformId;

    /** 平台字典值（对应 ai_platform_name.dict_value） */
    private String platformVal;

    /** 平台名称（对应 ai_platform_name.dict_label） */
    private String platformName;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    public Long getPlatformId()
    {
        return platformId;
    }

    public void setPlatformId(Long platformId)
    {
        this.platformId = platformId;
    }

    public String getPlatformVal()
    {
        return platformVal;
    }

    public void setPlatformVal(String platformVal)
    {
        this.platformVal = platformVal;
    }

    public String getPlatformName()
    {
        return platformName;
    }

    public void setPlatformName(String platformName)
    {
        this.platformName = platformName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
