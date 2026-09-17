package com.ruoyi.cpaexternal.subscription.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionPlan;

/**
 * AI订阅套餐 数据层
 */
public interface AiSubscriptionPlanMapper
{
    public AiSubscriptionPlan selectAiSubscriptionPlanById(Long planId);

    public List<AiSubscriptionPlan> selectAiSubscriptionPlanList(AiSubscriptionPlan aiSubscriptionPlan);

    public int insertAiSubscriptionPlan(AiSubscriptionPlan aiSubscriptionPlan);

    public int updateAiSubscriptionPlan(AiSubscriptionPlan aiSubscriptionPlan);

    public int updateAiSubscriptionPlanStatus(@Param("planId") Long planId, @Param("status") String status);

    public int deleteAiSubscriptionPlanByIds(Long[] planIds);
}
