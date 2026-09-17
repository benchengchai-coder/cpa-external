package com.ruoyi.web.controller.aigate;

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
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingRecordQueryService;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;
import com.ruoyi.system.recharge.service.IAiRechargeRecordService;
import com.ruoyi.system.service.ISysUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 钱包及充值记录管理接口测试。 */
@ExtendWith(MockitoExtension.class)
class AiWalletControllerTest
{
    private static final Long USER_ID = 100L;

    @Mock
    private ISysUserService userService;

    @Mock
    private IAiRechargeRecordService rechargeRecordService;

    @Mock
    private ICpaBillingRecordQueryService billingRecordQueryService;

    private AiWalletController controller;

    @BeforeEach
    void setUp()
    {
        controller = new TestAiWalletController();
        ReflectionTestUtils.setField(controller, "sysUserService", userService);
        ReflectionTestUtils.setField(controller, "rechargeRecordService", rechargeRecordService);
        ReflectionTestUtils.setField(controller, "billingRecordQueryService", billingRecordQueryService);
        CpaBillingAmountSummaryVO summary = new CpaBillingAmountSummaryVO();
        summary.setTotalChargedAmount(BigDecimal.ZERO);
        summary.setTotalUncoveredAmount(BigDecimal.ZERO);
        // 与钱包信息无关的用例不触发该查询，使用 lenient 避免严格模式误报。
        lenient().when(billingRecordQueryService.selectUserAmountSummary(USER_ID)).thenReturn(summary);
    }

    @Test
    void adminWalletInfoShouldIncludeBillingMultiplier()
    {
        SysUser user = new SysUser(USER_ID);
        user.setUserName("testUser");
        user.setBalance(new BigDecimal("12.3400"));
        user.setFrozenBalance(BigDecimal.ZERO);
        user.setBillingMultiplier(new BigDecimal("1.25"));
        when(userService.selectUserById(USER_ID)).thenReturn(user);

        AjaxResult response = controller.adminGetWalletInfo(USER_ID);

        assertEquals(200, response.get("code"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.get("data");
        assertEquals(new BigDecimal("1.25"), data.get("billingMultiplier"));
        assertEquals(new BigDecimal("12.3400"), data.get("availableBalance"));
    }

    @Test
    void adminRechargeListShouldRestrictQueryToPathUser()
    {
        AiRechargeRecord record = new AiRechargeRecord();
        record.setUserId(USER_ID);
        when(rechargeRecordService.selectAiRechargeRecordList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(record));

        TableDataInfo response = controller.adminRechargeList(USER_ID, new AiRechargeRecord());

        ArgumentCaptor<AiRechargeRecord> queryCaptor = ArgumentCaptor.forClass(AiRechargeRecord.class);
        verify(rechargeRecordService).selectAiRechargeRecordList(queryCaptor.capture());
        assertEquals(USER_ID, queryCaptor.getValue().getUserId());
        assertEquals(1L, response.getTotal());
        assertSame(record, response.getRows().get(0));
    }

    private static class TestAiWalletController extends AiWalletController
    {
        @Override
        protected void startPage()
        {
            // 单元测试只验证用户条件传递，不启动请求分页上下文。
        }
    }
}
