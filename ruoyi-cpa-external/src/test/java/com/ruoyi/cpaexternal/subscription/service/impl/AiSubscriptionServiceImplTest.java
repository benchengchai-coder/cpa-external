package com.ruoyi.cpaexternal.subscription.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionGrantRequest;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionPlan;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionRecord;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.mapper.AiSubscriptionPlanMapper;
import com.ruoyi.cpaexternal.subscription.mapper.AiSubscriptionRecordMapper;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;
import com.ruoyi.system.mapper.SysUserMapper;

/** 独立订阅业务核心流程测试。 */
@ExtendWith(MockitoExtension.class)
class AiSubscriptionServiceImplTest
{
    @Mock
    private AiSubscriptionPlanMapper planMapper;

    @Mock
    private AiUserSubscriptionMapper userSubscriptionMapper;

    @Mock
    private AiSubscriptionRecordMapper recordMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    private AiSubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setUp()
    {
        subscriptionService = new AiSubscriptionServiceImpl();
        ReflectionTestUtils.setField(subscriptionService, "planMapper", planMapper);
        ReflectionTestUtils.setField(subscriptionService, "userSubscriptionMapper", userSubscriptionMapper);
        ReflectionTestUtils.setField(subscriptionService, "recordMapper", recordMapper);
        ReflectionTestUtils.setField(subscriptionService, "sysUserMapper", sysUserMapper);
    }

    @Test
    void shouldGrantSubscriptionAndWriteRecord()
    {
        SysUser user = user(100L, "test-user");
        AiSubscriptionPlan plan = plan(6L, "新人试用");
        AiSubscriptionGrantRequest request = new AiSubscriptionGrantRequest();
        request.setPlanId(plan.getPlanId());
        request.setRemark("后台补发");
        when(sysUserMapper.selectUserById(user.getUserId())).thenReturn(user);
        when(planMapper.selectAiSubscriptionPlanById(plan.getPlanId())).thenReturn(plan);
        when(userSubscriptionMapper.insertAiUserSubscription(any(AiUserSubscription.class))).thenAnswer(invocation ->
        {
            AiUserSubscription subscription = invocation.getArgument(0);
            subscription.setSubscriptionId(20L);
            return 1;
        });

        AiUserSubscription result = subscriptionService.grantSubscription(user.getUserId(), request, "admin");

        assertEquals(20L, result.getSubscriptionId());
        assertEquals(AiSubscriptionConstants.SOURCE_GRANT, result.getSourceType());
        assertEquals("后台补发", result.getRemark());
        ArgumentCaptor<AiSubscriptionRecord> recordCaptor = ArgumentCaptor.forClass(AiSubscriptionRecord.class);
        verify(recordMapper).insertAiSubscriptionRecord(recordCaptor.capture());
        assertEquals(AiSubscriptionConstants.RECORD_TYPE_GRANT, recordCaptor.getValue().getType());
        assertEquals("后台授予", recordCaptor.getValue().getSourceName());
    }

    @Test
    void shouldNotGrantRegisterTrialTwice()
    {
        SysUser user = user(100L, "test-user");
        AiUserSubscription existing = new AiUserSubscription();
        existing.setSubscriptionId(20L);
        existing.setSourceType(AiSubscriptionConstants.SOURCE_REGISTER_TRIAL);
        when(sysUserMapper.selectUserById(user.getUserId())).thenReturn(user);
        when(userSubscriptionMapper.selectLatestSubscriptionByUserIdAndSourceType(user.getUserId(),
            AiSubscriptionConstants.SOURCE_REGISTER_TRIAL)).thenReturn(existing);

        AiUserSubscription result = subscriptionService.grantRegisterTrial(user.getUserId(), 6L);

        assertSame(existing, result);
        verify(planMapper, never()).selectAiSubscriptionPlanById(any());
        verify(userSubscriptionMapper, never()).insertAiUserSubscription(any(AiUserSubscription.class));
    }

    @Test
    void shouldRejectDeletingActiveSubscription()
    {
        AiUserSubscription subscription = new AiUserSubscription();
        subscription.setSubscriptionId(20L);
        subscription.setStatus(AiSubscriptionConstants.SUB_STATUS_ACTIVE);
        when(userSubscriptionMapper.selectAiUserSubscriptionById(20L)).thenReturn(subscription);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> subscriptionService.deleteUserSubscriptionByIds(new Long[] { 20L }));

        assertEquals("存在生效中的订阅，请先取消后再删除", exception.getMessage());
        verify(userSubscriptionMapper, never()).deleteAiUserSubscriptionByIds(any());
    }

    private SysUser user(Long userId, String username)
    {
        SysUser user = new SysUser(userId);
        user.setUserName(username);
        return user;
    }

    private AiSubscriptionPlan plan(Long planId, String title)
    {
        AiSubscriptionPlan plan = new AiSubscriptionPlan();
        plan.setPlanId(planId);
        plan.setTitle(title);
        plan.setPriceAmount(BigDecimal.ZERO);
        plan.setDurationUnit("day");
        plan.setDurationValue(3);
        plan.setAmountTotal(BigDecimal.ONE);
        plan.setQuotaResetPeriod("none");
        plan.setStatus(AiSubscriptionConstants.PLAN_STATUS_NORMAL);
        return plan;
    }
}
