package com.ruoyi.cpaexternal.model.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/** 模型管理对象。 */
public class CpaModel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long modelId;
    private String modelName;
    private String description;
    private String icon;
    private String platform;
    private BigDecimal officialInputPrice;
    private BigDecimal officialOutputPrice;
    private BigDecimal officialCacheReadPrice;
    private BigDecimal officialCacheWritePrice;
    private String status;

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public BigDecimal getOfficialInputPrice() { return officialInputPrice; }
    public void setOfficialInputPrice(BigDecimal officialInputPrice) { this.officialInputPrice = officialInputPrice; }
    public BigDecimal getOfficialOutputPrice() { return officialOutputPrice; }
    public void setOfficialOutputPrice(BigDecimal officialOutputPrice) { this.officialOutputPrice = officialOutputPrice; }
    public BigDecimal getOfficialCacheReadPrice() { return officialCacheReadPrice; }
    public void setOfficialCacheReadPrice(BigDecimal officialCacheReadPrice) { this.officialCacheReadPrice = officialCacheReadPrice; }
    public BigDecimal getOfficialCacheWritePrice() { return officialCacheWritePrice; }
    public void setOfficialCacheWritePrice(BigDecimal officialCacheWritePrice) { this.officialCacheWritePrice = officialCacheWritePrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
