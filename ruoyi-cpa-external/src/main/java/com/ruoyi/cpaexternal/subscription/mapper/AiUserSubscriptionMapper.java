package com.ruoyi.cpaexternal.subscription.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;

/**
 * AI用户订阅 数据层
 */
public interface AiUserSubscriptionMapper
{
    public AiUserSubscription selectAiUserSubscriptionById(Long subscriptionId);

    /** 计费结算用：按订阅 ID 加锁查询。 */
    public AiUserSubscription selectAiUserSubscriptionByIdForUpdate(Long subscriptionId);

    public List<AiUserSubscription> selectAiUserSubscriptionList(AiUserSubscription aiUserSubscription);

    public List<AiUserSubscription> selectExpiredActiveSubscriptionsByUserId(@Param("userId") Long userId);

    public AiUserSubscription selectActiveSubscriptionByUserId(@Param("userId") Long userId);

    public AiUserSubscription selectActiveSubscriptionForUpdate(@Param("userId") Long userId);

    public AiUserSubscription selectActiveSubscriptionSnapshotForBilling(@Param("userId") Long userId);

    public int freezeAiUserSubscriptionBalance(@Param("subscriptionId") Long subscriptionId,
                                               @Param("amount") java.math.BigDecimal amount);

    public int releaseAiUserSubscriptionFrozenBalance(@Param("subscriptionId") Long subscriptionId,
                                                      @Param("amount") java.math.BigDecimal amount);

    public int settleAiUserSubscriptionBilling(@Param("subscriptionId") Long subscriptionId,
                                               @Param("reservedAmount") java.math.BigDecimal reservedAmount,
                                               @Param("chargedAmount") java.math.BigDecimal chargedAmount);

    public AiUserSubscription selectLatestSubscriptionByUserIdAndSourceType(@Param("userId") Long userId,
                                                                            @Param("sourceType") String sourceType);

    public int countSubscriptionsByPlanId(Long planId);

    public int countUserPlanSubscriptions(@Param("userId") Long userId, @Param("planId") Long planId);

    public int insertAiUserSubscription(AiUserSubscription aiUserSubscription);

    public int updateAiUserSubscriptionStatus(@Param("subscriptionId") Long subscriptionId, @Param("status") String status);

    public int expireAiUserSubscriptionIfDue(@Param("subscriptionId") Long subscriptionId,
                                             @Param("operator") String operator);

    public int deleteAiUserSubscriptionByIds(Long[] subscriptionIds);
}
