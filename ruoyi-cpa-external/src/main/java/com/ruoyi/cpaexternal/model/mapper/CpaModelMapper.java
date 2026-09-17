package com.ruoyi.cpaexternal.model.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cpaexternal.model.domain.CpaModel;

/** 模型数据访问层。 */
public interface CpaModelMapper
{
    CpaModel selectById(Long modelId);

    CpaModel selectByName(String modelName);

    /** 按模型标识查询官方定价，不限制状态：停用模型的历史调用仍需按当时定价计费。 */
    CpaModel selectPricingByName(String modelName);

    List<CpaModel> selectList(CpaModel query);

    int insert(CpaModel model);

    int update(CpaModel model);

    int deleteById(Long modelId);

    int deleteByIds(@Param("modelIds") Long[] modelIds);

}
