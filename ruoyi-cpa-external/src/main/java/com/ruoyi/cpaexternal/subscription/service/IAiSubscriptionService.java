package com.ruoyi.cpaexternal.subscription.service;

import java.util.List;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionGrantRequest;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionPlan;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionRecord;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;

/**
 * AI订阅 服务层
 */
public interface IAiSubscriptionService
{
    public AiSubscriptionPlan selectPlanById(Long planId);

    public List<AiSubscriptionPlan> selectPlanList(AiSubscriptionPlan query);

    public List<AiSubscriptionPlan> selectUserVisiblePlans();

    public int insertPlan(AiSubscriptionPlan plan);

    public int updatePlan(AiSubscriptionPlan plan);

    public int changePlanStatus(Long planId, String status);

    public int deletePlanByIds(Long[] planIds);

    public List<AiUserSubscription> selectUserSubscriptionList(AiUserSubscription query);

    public List<AiUserSubscription> selectSelfSubscriptions(Long userId);

    public AiUserSubscription grantSubscription(Long userId, AiSubscriptionGrantRequest request, String operator);

    public AiUserSubscription grantRegisterTrial(Long userId, Long planId);

    public AiUserSubscription purchaseWithBalance(Long userId, String username, Long planId);

    public void cancelSubscription(Long subscriptionId, String operator);

    public int deleteUserSubscriptionByIds(Long[] subscriptionIds);

    public List<AiSubscriptionRecord> selectRecordList(AiSubscriptionRecord query);

    public String getBalancePurchaseEnabled();

    public void updateBalancePurchaseEnabled(String enabled, String operator);

    public void updateBillingPreference(Long userId, String billingPreference);
}
