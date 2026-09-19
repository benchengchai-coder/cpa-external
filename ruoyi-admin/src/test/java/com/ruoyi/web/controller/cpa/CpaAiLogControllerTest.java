package com.ruoyi.web.controller.cpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingRecordQueryService;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;
import com.ruoyi.cpaexternal.log.domain.vo.CpaAiLogListVO;
import com.ruoyi.cpaexternal.log.domain.vo.CpaAiLogUsageListVO;
import com.ruoyi.cpaexternal.log.domain.vo.CpaLogUserSummaryVO;
import com.ruoyi.cpaexternal.log.service.ICpaAiLogService;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.service.IAiSubscriptionService;
import com.ruoyi.system.service.ISysUserService;

/** 个人使用记录接口测试。 */
@ExtendWith(MockitoExtension.class)
class CpaAiLogControllerTest
{
    private static final Long USER_ID = 100L;

    @Mock
    private ICpaAiLogService aiLogService;

    @Mock
    private ISysUserService userService;

    @Mock
    private IAiSubscriptionService subscriptionService;

    @Mock
    private ICpaBillingRecordQueryService billingRecordQueryService;

    private CpaAiLogController controller;

    @BeforeEach
    void setUp()
    {
        controller = new TestCpaAiLogController();
        ReflectionTestUtils.setField(controller, "aiLogService", aiLogService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "subscriptionService", subscriptionService);
        ReflectionTestUtils.setField(controller, "billingRecordQueryService", billingRecordQueryService);
    }

    @Test
    void usageListShouldRestrictQueryToCurrentUserAndMaskKey()
    {
        CpaAiLog log = new CpaAiLog();
        log.setUserId(USER_ID);
        log.setApiKey("sk-1234567890123456");
        when(aiLogService.selectList(any(CpaAiLog.class))).thenReturn(List.of(log));

        TableDataInfo response = controller.usageList(new CpaAiLog());

        ArgumentCaptor<CpaAiLog> queryCaptor = ArgumentCaptor.forClass(CpaAiLog.class);
        verify(aiLogService).selectList(queryCaptor.capture());
        assertEquals(USER_ID, queryCaptor.getValue().getUserId());
        assertEquals("sk-123******3456", log.getApiKey());
        assertEquals(1L, response.getTotal());
        assertEquals(CpaAiLogUsageListVO.class, response.getRows().get(0).getClass());
        assertNull(((CpaAiLogUsageListVO) response.getRows().get(0)).getRelayMode());
    }

    @Test
    void globalListShouldReturnOnlyListFields()
    {
        CpaAiLog log = new CpaAiLog();
        log.setLogId(1L);
        log.setRequestId("request-detail");
        log.setSessionId("session-detail");
        log.setApiKey("sk-1234567890123456");
        log.setModel("gpt-test");
        when(aiLogService.selectList(any(CpaAiLog.class))).thenReturn(List.of(log));

        TableDataInfo response = controller.list(new CpaAiLog());

        assertEquals(CpaAiLogListVO.class, response.getRows().get(0).getClass());
        CpaAiLogListVO row = (CpaAiLogListVO) response.getRows().get(0);
        assertEquals(1L, row.getLogId());
        assertEquals("gpt-test", row.getModelName());
    }

    @Test
    void usageDetailShouldRejectAnotherUsersLog()
    {
        CpaAiLog log = new CpaAiLog();
        log.setUserId(101L);
        when(aiLogService.selectById(1L)).thenReturn(log);

        ServiceException exception = assertThrows(ServiceException.class, () -> controller.getUsageInfo(1L));

        assertEquals("使用记录不存在", exception.getMessage());
    }

    @Test
    void usageDetailShouldReturnCurrentUsersLogAndMaskKey()
    {
        CpaAiLog log = new CpaAiLog();
        log.setUserId(USER_ID);
        log.setApiKey("sk-1234567890123456");
        when(aiLogService.selectById(1L)).thenReturn(log);

        AjaxResult response = controller.getUsageInfo(1L);

        assertEquals(200, response.get("code"));
        assertSame(log, response.get("data"));
        assertEquals("sk-123******3456", log.getApiKey());
    }

    @Test
    void userSummaryShouldRejectMissingUser()
    {
        when(userService.selectUserById(999L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class, () -> controller.getUserSummary(999L));

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void userSummaryShouldReturnUserInfoWalletAndActiveSubscriptions()
    {
        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        user.setUserName("tester");
        user.setNickName("测试用户");
        user.setBalance(new BigDecimal("12.50"));
        user.setFrozenBalance(new BigDecimal("2.50"));
        when(userService.selectUserById(USER_ID)).thenReturn(user);
        AiUserSubscription subscription = new AiUserSubscription();
        subscription.setPlanTitle("月度套餐");
        when(subscriptionService.selectUserSubscriptionList(any(AiUserSubscription.class))).thenReturn(List.of(subscription));
        CpaBillingAmountSummaryVO amountSummary = new CpaBillingAmountSummaryVO();
        amountSummary.setTotalChargedAmount(new BigDecimal("9.99"));
        amountSummary.setTotalUncoveredAmount(BigDecimal.ZERO);
        when(billingRecordQueryService.selectUserAmountSummary(USER_ID)).thenReturn(amountSummary);

        AjaxResult response = controller.getUserSummary(USER_ID);

        assertEquals(200, response.get("code"));
        CpaLogUserSummaryVO summary = (CpaLogUserSummaryVO) response.get("data");
        assertEquals("tester", summary.getUsername());
        assertEquals(new BigDecimal("12.50"), summary.getBalance());
        assertEquals(new BigDecimal("10.00"), summary.getAvailableBalance());
        assertEquals(new BigDecimal("9.99"), summary.getTotalChargedAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getTotalUncoveredAmount()));
        assertSame(subscription, summary.getSubscriptions().get(0));
        ArgumentCaptor<AiUserSubscription> queryCaptor = ArgumentCaptor.forClass(AiUserSubscription.class);
        verify(subscriptionService).selectUserSubscriptionList(queryCaptor.capture());
        assertEquals(USER_ID, queryCaptor.getValue().getUserId());
        assertEquals(AiSubscriptionConstants.SUB_STATUS_ACTIVE, queryCaptor.getValue().getStatus());
    }

    private static class TestCpaAiLogController extends CpaAiLogController
    {
        @Override
        public Long getUserId()
        {
            return USER_ID;
        }

        @Override
        protected void startPage()
        {
            // 单元测试只验证用户条件传递，不启动请求分页上下文。
        }
    }
}
