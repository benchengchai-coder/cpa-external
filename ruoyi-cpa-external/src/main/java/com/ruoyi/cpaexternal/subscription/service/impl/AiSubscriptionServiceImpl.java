package com.ruoyi.cpaexternal.subscription.service.impl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionGrantRequest;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionPlan;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionRecord;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.mapper.AiSubscriptionPlanMapper;
import com.ruoyi.cpaexternal.subscription.mapper.AiSubscriptionRecordMapper;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;
import com.ruoyi.cpaexternal.subscription.service.IAiSubscriptionService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysConfigService;

/**
 * AI订阅 服务层实现
 */
@Service
public class AiSubscriptionServiceImpl implements IAiSubscriptionService
{
    @Autowired
    private AiSubscriptionPlanMapper planMapper;

    @Autowired
    private AiUserSubscriptionMapper userSubscriptionMapper;

    @Autowired
    private AiSubscriptionRecordMapper recordMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private AiSubscriptionExpirationService subscriptionExpirationService;

    @Override
    public AiSubscriptionPlan selectPlanById(Long planId)
    {
        return planMapper.selectAiSubscriptionPlanById(planId);
    }

    @Override
    public List<AiSubscriptionPlan> selectPlanList(AiSubscriptionPlan query)
    {
        return planMapper.selectAiSubscriptionPlanList(query);
    }

    @Override
    public List<AiSubscriptionPlan> selectUserVisiblePlans()
    {
        AiSubscriptionPlan query = new AiSubscriptionPlan();
        query.setStatus(AiSubscriptionConstants.PLAN_STATUS_NORMAL);
        return planMapper.selectAiSubscriptionPlanList(query);
    }

    @Override
    public int insertPlan(AiSubscriptionPlan plan)
    {
        normalizePlan(plan);
        return planMapper.insertAiSubscriptionPlan(plan);
    }

    @Override
    public int updatePlan(AiSubscriptionPlan plan)
    {
        normalizePlan(plan);
        return planMapper.updateAiSubscriptionPlan(plan);
    }

    @Override
    public int changePlanStatus(Long planId, String status)
    {
        if (planId == null)
        {
            throw new ServiceException("套餐ID不能为空");
        }
        return planMapper.updateAiSubscriptionPlanStatus(planId, status);
    }

    @Override
    public int deletePlanByIds(Long[] planIds)
    {
        if (planIds == null || planIds.length == 0)
        {
            return 0;
        }
        for (Long planId : planIds)
        {
            if (planMapper.selectAiSubscriptionPlanById(planId) == null)
            {
                continue;
            }
            if (userSubscriptionMapper.countSubscriptionsByPlanId(planId) > 0)
            {
                throw new ServiceException("套餐已存在用户订阅，不能删除，请改为停用");
            }
        }
        return planMapper.deleteAiSubscriptionPlanByIds(planIds);
    }

    @Override
    public List<AiUserSubscription> selectUserSubscriptionList(AiUserSubscription query)
    {
        if (query != null && query.getUserId() != null)
        {
            expireDueUserSubscriptions(query.getUserId(), "system");
        }
        return userSubscriptionMapper.selectAiUserSubscriptionList(query);
    }

    @Override
    public List<AiUserSubscription> selectSelfSubscriptions(Long userId)
    {
        expireDueUserSubscriptions(userId, "system");
        AiUserSubscription query = new AiUserSubscription();
        query.setUserId(userId);
        return userSubscriptionMapper.selectAiUserSubscriptionList(query);
    }

    @Override
    @Transactional
    public AiUserSubscription grantSubscription(Long userId, AiSubscriptionGrantRequest request, String operator)
    {
        if (request == null || request.getPlanId() == null)
        {
            throw new ServiceException("套餐不能为空");
        }
        SysUser user = requireUser(userId);
        AiSubscriptionPlan plan = requirePlan(request.getPlanId());
        Date startTime = request.getStartTime() == null ? new Date() : request.getStartTime();
        Date endTime = request.getEndTime() == null ? calculateEndTime(startTime, plan) : request.getEndTime();
        if (!endTime.after(startTime))
        {
            throw new ServiceException("订阅结束时间必须晚于开始时间");
        }
        AiUserSubscription subscription = createSubscription(user, plan, startTime, endTime,
            AiSubscriptionConstants.SOURCE_GRANT, operator, request.getRemark(), true);
        writeRecord(subscription, AiSubscriptionConstants.RECORD_TYPE_GRANT, BigDecimal.ZERO,
            "后台授予", operator, request.getRemark());
        return subscription;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiUserSubscription grantRegisterTrial(Long userId, Long planId)
    {
        SysUser user = requireUser(userId);
        AiUserSubscription existing = userSubscriptionMapper.selectLatestSubscriptionByUserIdAndSourceType(userId,
            AiSubscriptionConstants.SOURCE_REGISTER_TRIAL);
        if (existing != null)
        {
            return existing;
        }

        AiSubscriptionPlan plan = requireEnabledPlan(planId);
        Date startTime = new Date();
        AiUserSubscription subscription = createSubscription(user, plan, startTime, calculateEndTime(startTime, plan),
            AiSubscriptionConstants.SOURCE_REGISTER_TRIAL, "system", "新人注册自动发放试用订阅", false);
        writeRecord(subscription, AiSubscriptionConstants.RECORD_TYPE_GRANT, BigDecimal.ZERO,
            "新人注册试用", "system", "新人注册自动发放试用订阅");
        return subscription;
    }

    @Override
    @Transactional
    public AiUserSubscription purchaseWithBalance(Long userId, String username, Long planId)
    {
        if (!"true".equalsIgnoreCase(getBalancePurchaseEnabled()))
        {
            throw new ServiceException("暂未开放余额购买订阅");
        }
        SysUser user = requireUser(userId);
        AiSubscriptionPlan plan = requireEnabledPlan(planId);
        if (plan.getAllowBalancePurchase() == null || plan.getAllowBalancePurchase() != 1)
        {
            throw new ServiceException("该套餐不支持余额购买");
        }
        if (plan.getMaxPurchasePerUser() != null && plan.getMaxPurchasePerUser() > 0
            && userSubscriptionMapper.countUserPlanSubscriptions(userId, planId) >= plan.getMaxPurchasePerUser())
        {
            throw new ServiceException("已达到该套餐购买次数上限");
        }
        BigDecimal price = nvl(plan.getPriceAmount());
        if (price.compareTo(BigDecimal.ZERO) > 0 && sysUserMapper.subUserBalance(userId, price) != 1)
        {
            throw new ServiceException("用户余额不足");
        }
        Date startTime = new Date();
        AiUserSubscription subscription = createSubscription(user, plan, startTime, calculateEndTime(startTime, plan),
            AiSubscriptionConstants.SOURCE_BALANCE_PURCHASE, username, "余额购买订阅", false);
        writeRecord(subscription, AiSubscriptionConstants.RECORD_TYPE_PURCHASE, price,
            "余额购买", username, "余额购买订阅");
        return subscription;
    }

    @Override
    @Transactional
    public void cancelSubscription(Long subscriptionId, String operator)
    {
        AiUserSubscription subscription = requireSubscription(subscriptionId);
        if (!AiSubscriptionConstants.SUB_STATUS_ACTIVE.equals(subscription.getStatus()))
        {
            return;
        }
        userSubscriptionMapper.updateAiUserSubscriptionStatus(subscriptionId, AiSubscriptionConstants.SUB_STATUS_CANCELLED);
        writeRecord(subscription, AiSubscriptionConstants.RECORD_TYPE_CANCEL, BigDecimal.ZERO,
            "后台取消", operator, "取消订阅");
    }

    @Override
    @Transactional
    public int deleteUserSubscriptionByIds(Long[] subscriptionIds)
    {
        if (subscriptionIds == null || subscriptionIds.length == 0)
        {
            return 0;
        }
        for (Long subscriptionId : subscriptionIds)
        {
            AiUserSubscription subscription = requireSubscription(subscriptionId);
            if (AiSubscriptionConstants.SUB_STATUS_ACTIVE.equals(subscription.getStatus()))
            {
                throw new ServiceException("存在生效中的订阅，请先取消后再删除");
            }
        }
        return userSubscriptionMapper.deleteAiUserSubscriptionByIds(subscriptionIds);
    }

    @Override
    public List<AiSubscriptionRecord> selectRecordList(AiSubscriptionRecord query)
    {
        return recordMapper.selectAiSubscriptionRecordList(query);
    }

    @Override
    public String getBalancePurchaseEnabled()
    {
        String value = configService.selectConfigByKey(AiSubscriptionConstants.CONFIG_BALANCE_PURCHASE_ENABLED);
        return StringUtils.isEmpty(value) ? "false" : value;
    }

    @Override
    public void updateBalancePurchaseEnabled(String enabled, String operator)
    {
        String value = "true".equalsIgnoreCase(enabled) ? "true" : "false";
        SysConfig query = new SysConfig();
        query.setConfigKey(AiSubscriptionConstants.CONFIG_BALANCE_PURCHASE_ENABLED);
        List<SysConfig> configs = configService.selectConfigList(query);
        SysConfig config = configs == null || configs.isEmpty() ? null : configs.get(0);
        if (config == null)
        {
            config = new SysConfig();
            config.setConfigName("AI订阅余额购买开关");
            config.setConfigKey(AiSubscriptionConstants.CONFIG_BALANCE_PURCHASE_ENABLED);
            config.setConfigValue(value);
            config.setConfigType("Y");
            config.setCreateBy(operator);
            config.setRemark("控制用户是否可以使用钱包余额购买订阅");
            configService.insertConfig(config);
        }
        else
        {
            config.setConfigValue(value);
            config.setUpdateBy(operator);
            configService.updateConfig(config);
        }
    }

    @Override
    public void updateBillingPreference(Long userId, String billingPreference)
    {
        String preference = normalizePreference(billingPreference);
        if (sysUserMapper.updateBillingPreference(userId, preference) != 1)
        {
            throw new ServiceException("更新扣费偏好失败");
        }
    }

    private void normalizePlan(AiSubscriptionPlan plan)
    {
        if (plan == null)
        {
            throw new ServiceException("套餐不能为空");
        }
        if (StringUtils.isEmpty(plan.getTitle()))
        {
            throw new ServiceException("套餐标题不能为空");
        }
        if (plan.getPriceAmount() == null)
        {
            plan.setPriceAmount(BigDecimal.ZERO);
        }
        if (StringUtils.isEmpty(plan.getDurationUnit()))
        {
            plan.setDurationUnit("month");
        }
        if (plan.getDurationValue() == null || plan.getDurationValue() <= 0)
        {
            plan.setDurationValue(1);
        }
        if (plan.getAmountTotal() == null)
        {
            plan.setAmountTotal(BigDecimal.ZERO);
        }
        if (StringUtils.isEmpty(plan.getQuotaResetPeriod()))
        {
            plan.setQuotaResetPeriod("none");
        }
        if (StringUtils.isEmpty(plan.getStatus()))
        {
            plan.setStatus(AiSubscriptionConstants.PLAN_STATUS_NORMAL);
        }
        if (plan.getSortOrder() == null)
        {
            plan.setSortOrder(0);
        }
        if (plan.getAllowBalancePurchase() == null)
        {
            plan.setAllowBalancePurchase(0);
        }
    }

    private SysUser requireUser(Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("用户ID不能为空");
        }
        SysUser user = sysUserMapper.selectUserById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    private AiSubscriptionPlan requireEnabledPlan(Long planId)
    {
        AiSubscriptionPlan plan = requirePlan(planId);
        if (!AiSubscriptionConstants.PLAN_STATUS_NORMAL.equals(plan.getStatus()))
        {
            throw new ServiceException("套餐已停用");
        }
        return plan;
    }

    private AiSubscriptionPlan requirePlan(Long planId)
    {
        if (planId == null)
        {
            throw new ServiceException("套餐ID不能为空");
        }
        AiSubscriptionPlan plan = planMapper.selectAiSubscriptionPlanById(planId);
        if (plan == null)
        {
            throw new ServiceException("套餐不存在");
        }
        return plan;
    }

    private AiUserSubscription requireSubscription(Long subscriptionId)
    {
        if (subscriptionId == null)
        {
            throw new ServiceException("订阅ID不能为空");
        }
        AiUserSubscription subscription = userSubscriptionMapper.selectAiUserSubscriptionById(subscriptionId);
        if (subscription == null)
        {
            throw new ServiceException("订阅不存在");
        }
        return subscription;
    }

    private AiUserSubscription createSubscription(SysUser user, AiSubscriptionPlan plan, Date startTime,
                                                  Date endTime, String sourceType, String operator, String remark,
                                                  boolean forceReplace)
    {
        // 检查用户是否已有有效订阅
        AiUserSubscription existing = forceReplace
            ? userSubscriptionMapper.selectActiveSubscriptionForUpdate(user.getUserId())
            : userSubscriptionMapper.selectActiveSubscriptionByUserId(user.getUserId());
        if (existing != null)
        {
            if (!forceReplace)
            {
                throw new ServiceException("用户已有生效中的订阅（" + existing.getPlanTitle() + "），请先取消后再操作");
            }
            // 自动替换：取消旧订阅
            userSubscriptionMapper.updateAiUserSubscriptionStatus(existing.getSubscriptionId(),
                AiSubscriptionConstants.SUB_STATUS_CANCELLED);
            writeRecord(existing, AiSubscriptionConstants.RECORD_TYPE_REPLACE, BigDecimal.ZERO,
                "订阅替换", operator, "因授予新订阅而自动取消");
        }

        AiUserSubscription subscription = new AiUserSubscription();
        subscription.setUserId(user.getUserId());
        subscription.setUsername(user.getUserName());
        subscription.setPlanId(plan.getPlanId());
        subscription.setPlanTitle(plan.getTitle());
        subscription.setPlanSubTitle(plan.getSubTitle());
        subscription.setPriceAmount(nvl(plan.getPriceAmount()));
        subscription.setStartTime(startTime);
        subscription.setEndTime(endTime);
        subscription.setStatus(AiSubscriptionConstants.SUB_STATUS_ACTIVE);
        subscription.setAmountTotal(nvl(plan.getAmountTotal()));
        subscription.setAmountUsed(BigDecimal.ZERO);
        subscription.setQuotaResetPeriod(plan.getQuotaResetPeriod());
        subscription.setQuotaResetCustomSeconds(plan.getQuotaResetCustomSeconds());
        subscription.setLastResetTime(startTime);
        subscription.setNextResetTime(calculateNextResetTime(startTime, plan.getQuotaResetPeriod(), plan.getQuotaResetCustomSeconds()));
        subscription.setSourceType(sourceType);
        subscription.setCreateBy(operator);
        subscription.setRemark(remark);
        userSubscriptionMapper.insertAiUserSubscription(subscription);
        return subscription;
    }

    private void expireDueUserSubscriptions(Long userId, String operator)
    {
        if (userId == null)
        {
            return;
        }
        List<AiUserSubscription> expired = userSubscriptionMapper.selectExpiredActiveSubscriptionsByUserId(userId);
        for (AiUserSubscription subscription : expired)
        {
            subscriptionExpirationService.expireIfDue(subscription.getSubscriptionId(), operator);
        }
    }

    private void writeRecord(AiUserSubscription subscription, String type, BigDecimal amount,
                             String sourceName, String operator, String remark)
    {
        AiSubscriptionRecord record = new AiSubscriptionRecord();
        record.setUserId(subscription.getUserId());
        record.setUsername(subscription.getUsername());
        record.setPlanId(subscription.getPlanId());
        record.setPlanTitle(subscription.getPlanTitle());
        record.setUserSubscriptionId(subscription.getSubscriptionId());
        record.setType(type);
        record.setAmount(nvl(amount));
        record.setSourceName(sourceName);
        record.setOperatorName(operator);
        record.setStatus(AiSubscriptionConstants.RECORD_STATUS_SUCCESS);
        record.setCreateBy(operator);
        record.setRemark(remark);
        recordMapper.insertAiSubscriptionRecord(record);
    }

    private Date calculateEndTime(Date startTime, AiSubscriptionPlan plan)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        Integer value = plan.getDurationValue() == null ? 1 : plan.getDurationValue();
        String unit = StringUtils.isEmpty(plan.getDurationUnit()) ? "month" : plan.getDurationUnit();
        switch (unit)
        {
            case "day":
                calendar.add(Calendar.DAY_OF_MONTH, value);
                break;
            case "week":
                calendar.add(Calendar.WEEK_OF_YEAR, value);
                break;
            case "year":
                calendar.add(Calendar.YEAR, value);
                break;
            case "custom":
                long seconds = plan.getCustomSeconds() == null || plan.getCustomSeconds() <= 0
                    ? value.longValue() * 86400L : plan.getCustomSeconds();
                return Date.from(startTime.toInstant().plusSeconds(seconds));
            case "month":
            default:
                calendar.add(Calendar.MONTH, value);
                break;
        }
        return calendar.getTime();
    }

    private Date calculateNextResetTime(Date baseTime, String period, Long customSeconds)
    {
        if (StringUtils.isEmpty(period) || "none".equals(period))
        {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(baseTime);
        switch (period)
        {
            case "day":
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case "week":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "month":
                calendar.add(Calendar.MONTH, 1);
                break;
            case "custom":
                if (customSeconds == null || customSeconds <= 0)
                {
                    return null;
                }
                return Date.from(baseTime.toInstant().plusSeconds(customSeconds));
            default:
                return null;
        }
        return calendar.getTime();
    }

    private String normalizePreference(String preference)
    {
        if (AiSubscriptionConstants.PREFERENCE_WALLET_FIRST.equals(preference)
            || AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_ONLY.equals(preference)
            || AiSubscriptionConstants.PREFERENCE_WALLET_ONLY.equals(preference))
        {
            return preference;
        }
        return AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_FIRST;
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }
}
