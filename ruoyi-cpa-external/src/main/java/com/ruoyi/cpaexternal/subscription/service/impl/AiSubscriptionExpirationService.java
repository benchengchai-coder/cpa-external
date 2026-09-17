package com.ruoyi.cpaexternal.subscription.service.impl;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionRecord;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.mapper.AiSubscriptionRecordMapper;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;

/**
 * AI订阅到期处理服务。
 */
@Service
public class AiSubscriptionExpirationService
{
    @Autowired
    private AiUserSubscriptionMapper userSubscriptionMapper;

    @Autowired
    private AiSubscriptionRecordMapper recordMapper;

    /**
     * 原子处理单个已到期订阅。
     * <p>
     * 只有成功将状态从 active 更新为 expired 的调用方，才会写入过期流水，
     * 从而避免定时扫描与请求触发的懒过期并发执行时产生重复流水。
     *
     * @param subscriptionId 用户订阅ID
     * @param operator 操作者
     * @return 是否实际完成过期处理
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean expireIfDue(Long subscriptionId, String operator)
    {
        if (subscriptionId == null)
        {
            return false;
        }
        AiUserSubscription subscription = userSubscriptionMapper.selectAiUserSubscriptionById(subscriptionId);
        if (subscription == null)
        {
            return false;
        }
        String actualOperator = StrUtil.isBlank(operator) ? "system" : operator;
        if (userSubscriptionMapper.expireAiUserSubscriptionIfDue(subscriptionId, actualOperator) != 1)
        {
            return false;
        }
        writeExpirationRecord(subscription, actualOperator);
        return true;
    }

    private void writeExpirationRecord(AiUserSubscription subscription, String operator)
    {
        AiSubscriptionRecord record = new AiSubscriptionRecord();
        record.setUserId(subscription.getUserId());
        record.setUsername(subscription.getUsername());
        record.setPlanId(subscription.getPlanId());
        record.setPlanTitle(subscription.getPlanTitle());
        record.setUserSubscriptionId(subscription.getSubscriptionId());
        record.setType(AiSubscriptionConstants.RECORD_TYPE_EXPIRE);
        record.setAmount(BigDecimal.ZERO);
        record.setSourceName("系统过期");
        record.setOperatorName(operator);
        record.setStatus(AiSubscriptionConstants.RECORD_STATUS_SUCCESS);
        record.setCreateBy(operator);
        record.setRemark("订阅已过期");
        recordMapper.insertAiSubscriptionRecord(record);
    }
}
