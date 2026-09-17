package com.ruoyi.cpaexternal.subscription.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** 校验订阅套餐保存请求可以由 Jackson 3 反序列化。 */
class AiSubscriptionPlanTest
{
    @Test
    void shouldDeserializePlanRequest()
    {
        String json = "{\"title\":\"月度套餐\",\"subTitle\":\"每月重置\",\"priceAmount\":29.9,"
                + "\"durationUnit\":\"month\",\"durationValue\":1,\"amountTotal\":100,"
                + "\"quotaResetPeriod\":\"month\",\"status\":\"0\",\"sortOrder\":2,"
                + "\"maxPurchasePerUser\":3,\"allowBalancePurchase\":1,"
                + "\"remark\":\"测试套餐\"}";

        AiSubscriptionPlan plan = new ObjectMapper().readValue(json, AiSubscriptionPlan.class);

        assertEquals("月度套餐", plan.getTitle());
        assertEquals(new BigDecimal("29.9"), plan.getPriceAmount());
        assertEquals("month", plan.getDurationUnit());
        assertEquals(1, plan.getDurationValue());
        assertEquals(new BigDecimal("100"), plan.getAmountTotal());
        assertEquals(1, plan.getAllowBalancePurchase());
        assertEquals("测试套餐", plan.getRemark());
    }
}
