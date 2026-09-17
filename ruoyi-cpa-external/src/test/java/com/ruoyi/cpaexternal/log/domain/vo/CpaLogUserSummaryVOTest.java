package com.ruoyi.cpaexternal.log.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import tools.jackson.databind.ObjectMapper;

/**
 * 全局使用记录用户摘要 VO 与前端 AiLogUserSummary 结构的序列化测试。
 *
 * <p>HTTP 数据绑定使用 Jackson 3，字段名采用前端约定的 camelCase。</p>
 */
class CpaLogUserSummaryVOTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void summaryShouldSerializeCamelCaseFieldsWithFormattedDates()
    {
        SysUser user = new SysUser();
        user.setUserId(100L);
        user.setUserName("tester");
        user.setNickName("测试用户");
        user.setStatus("0");
        user.setBalance(new BigDecimal("12.50"));
        user.setFrozenBalance(new BigDecimal("2.50"));
        user.setUsedBalance(new BigDecimal("8.00"));
        user.setCreateTime(new Date(1760000000000L));
        SysDept dept = new SysDept();
        dept.setDeptName("研发部");
        user.setDept(dept);
        AiUserSubscription subscription = new AiUserSubscription();
        subscription.setPlanTitle("月度套餐");

        CpaLogUserSummaryVO summary = CpaLogUserSummaryVO.from(user, List.of(subscription));
        summary.setTotalChargedAmount(new BigDecimal("9.99"));
        summary.setTotalUncoveredAmount(BigDecimal.ZERO);

        String json = objectMapper.writeValueAsString(summary);

        assertTrue(json.contains("\"userId\":100"));
        assertTrue(json.contains("\"username\":\"tester\""));
        assertTrue(json.contains("\"nickName\":\"测试用户\""));
        assertTrue(json.contains("\"deptName\":\"研发部\""));
        assertTrue(json.contains("\"balance\":12.5"));
        assertTrue(json.contains("\"frozenBalance\":2.5"));
        assertTrue(json.contains("\"availableBalance\":10.0"));
        assertTrue(json.contains("\"usedBalance\":8.0"));
        assertTrue(json.contains("\"totalChargedAmount\":9.99"));
        assertTrue(json.contains("\"totalUncoveredAmount\":0"));
        assertTrue(json.contains("\"createTime\":\"2025-10-09"));
        assertTrue(json.contains("\"subscriptions\":["));
        assertTrue(json.contains("\"planTitle\":\"月度套餐\""));
        assertEquals(new BigDecimal("10.00"), summary.getAvailableBalance());
    }

    @Test
    void fromShouldDefaultNullWalletFieldsToZero()
    {
        SysUser user = new SysUser();
        user.setUserId(100L);
        user.setUserName("tester");

        CpaLogUserSummaryVO summary = CpaLogUserSummaryVO.from(user, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getFrozenBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getAvailableBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getUsedBalance()));
        assertTrue(summary.getSubscriptions().isEmpty());
    }
}
