package com.ruoyi.cpaexternal.platform.service;

import java.util.List;
import com.ruoyi.cpaexternal.platform.domain.AiPlatform;

/**
 * AI平台 服务层
 */
public interface IAiPlatformService
{
    public AiPlatform selectAiPlatformById(Long platformId);

    public List<AiPlatform> selectAiPlatformList(AiPlatform aiPlatform);

    public int insertAiPlatform(AiPlatform aiPlatform);

    public int updateAiPlatform(AiPlatform aiPlatform);

    public int deleteAiPlatformById(Long platformId);

    public int deleteAiPlatformByIds(Long[] platformIds);
}
