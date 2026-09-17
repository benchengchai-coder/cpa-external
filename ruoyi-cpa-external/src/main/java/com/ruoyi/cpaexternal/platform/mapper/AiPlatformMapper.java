package com.ruoyi.cpaexternal.platform.mapper;

import java.util.List;
import com.ruoyi.cpaexternal.platform.domain.AiPlatform;

/**
 * AI平台 数据层
 */
public interface AiPlatformMapper
{
    public AiPlatform selectAiPlatformById(Long platformId);

    public List<AiPlatform> selectAiPlatformList(AiPlatform aiPlatform);

    public int insertAiPlatform(AiPlatform aiPlatform);

    public int updateAiPlatform(AiPlatform aiPlatform);

    public int deleteAiPlatformById(Long platformId);

    public int deleteAiPlatformByIds(Long[] platformIds);
}
