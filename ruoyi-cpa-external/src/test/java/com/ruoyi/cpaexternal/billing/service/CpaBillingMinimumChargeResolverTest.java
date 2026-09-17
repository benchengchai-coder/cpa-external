package com.ruoyi.cpaexternal.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.system.service.ISysConfigService;

/** 请求级最低计费的解析与应用测试。 */
class CpaBillingMinimumChargeResolverTest
{
    private ISysConfigService configService;
    private CpaBillingMinimumChargeResolver resolver;

    @BeforeEach
    void setUp() throws Exception
    {
        configService = mock(ISysConfigService.class);
        resolver = new CpaBillingMinimumChargeResolver();
        Field field = CpaBillingMinimumChargeResolver.class.getDeclaredField("configService");
        field.setAccessible(true);
        field.set(resolver, configService);
    }

    /** 未定价模型的 cost 为 null，不应用最低计费，结算走 no_charge。 */
    @Test
    void applyShouldReturnZeroForNullCost()
    {
        when(configService.selectConfigByKey("ai.billing.minimumAmount")).thenReturn("0.001");

        assertEquals(0, BigDecimal.ZERO.compareTo(resolver.applyMinimumAmount(null)));
    }

    @Test
    void applyShouldRaiseCostToMinimum()
    {
        assertEquals(new BigDecimal("0.0010000000"),
                resolver.applyMinimumAmount(new BigDecimal("0.0001"), new BigDecimal("0.001")));
    }

    @Test
    void applyShouldKeepCostAboveMinimum()
    {
        assertEquals(new BigDecimal("0.5000000000"),
                resolver.applyMinimumAmount(new BigDecimal("0.5"), new BigDecimal("0.001")));
    }

    /** 配置缺失时按 0 处理，不改变原始费用。 */
    @Test
    void applyShouldKeepRawCostWhenMinimumMissing()
    {
        when(configService.selectConfigByKey("ai.billing.minimumAmount")).thenReturn("");

        assertEquals(new BigDecimal("0.0001000000"),
                resolver.applyMinimumAmount(new BigDecimal("0.0001")));
    }
}
