package com.ruoyi.cpaexternal.model.service;

import java.util.List;
import com.ruoyi.cpaexternal.model.domain.CpaModel;

/** 模型管理服务。 */
public interface ICpaModelService
{
    CpaModel selectById(Long modelId);

    CpaModel selectByName(String modelName);

    List<CpaModel> selectList(CpaModel query);

    int insert(CpaModel model);

    int update(CpaModel model);

    int deleteByIds(Long[] modelIds);
}
