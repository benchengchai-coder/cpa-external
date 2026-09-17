package com.ruoyi.cpaexternal.model.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.cpaexternal.model.domain.CpaModel;
import com.ruoyi.cpaexternal.model.mapper.CpaModelMapper;
import com.ruoyi.cpaexternal.model.service.ICpaModelService;

/** 模型管理服务实现。 */
@Service
public class CpaModelServiceImpl implements ICpaModelService
{
    @Autowired
    private CpaModelMapper modelMapper;

    @Override
    public CpaModel selectById(Long modelId)
    {
        return modelMapper.selectById(modelId);
    }

    @Override
    public CpaModel selectByName(String modelName)
    {
        return modelMapper.selectByName(modelName);
    }

    @Override
    public List<CpaModel> selectList(CpaModel query)
    {
        return modelMapper.selectList(query);
    }

    @Override
    public int insert(CpaModel model)
    {
        return modelMapper.insert(model);
    }

    @Override
    public int update(CpaModel model)
    {
        return modelMapper.update(model);
    }

    @Override
    @Transactional
    public int deleteByIds(Long[] modelIds)
    {
        return modelMapper.deleteByIds(modelIds);
    }
}
