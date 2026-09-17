package com.ruoyi.cpaexternal.platform.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.cpaexternal.platform.domain.AiPlatform;
import com.ruoyi.cpaexternal.platform.mapper.AiPlatformMapper;
import com.ruoyi.cpaexternal.platform.service.IAiPlatformService;

/**
 * AI平台 服务实现
 */
@Service
public class AiPlatformServiceImpl implements IAiPlatformService
{
    @Autowired
    private AiPlatformMapper aiPlatformMapper;

    @Override
    public AiPlatform selectAiPlatformById(Long platformId)
    {
        return aiPlatformMapper.selectAiPlatformById(platformId);
    }

    @Override
    public List<AiPlatform> selectAiPlatformList(AiPlatform aiPlatform)
    {
        return aiPlatformMapper.selectAiPlatformList(aiPlatform);
    }

    @Override
    public int insertAiPlatform(AiPlatform aiPlatform)
    {
        return aiPlatformMapper.insertAiPlatform(aiPlatform);
    }

    @Override
    public int updateAiPlatform(AiPlatform aiPlatform)
    {
        return aiPlatformMapper.updateAiPlatform(aiPlatform);
    }

    @Override
    @Transactional
    public int deleteAiPlatformById(Long platformId)
    {
        return aiPlatformMapper.deleteAiPlatformById(platformId);
    }

    @Override
    @Transactional
    public int deleteAiPlatformByIds(Long[] platformIds)
    {
        int rows = 0;
        for (Long platformId : platformIds)
        {
            rows += deleteAiPlatformById(platformId);
        }
        return rows;
    }
}
