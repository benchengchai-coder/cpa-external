package com.ruoyi.cpaexternal.subscription.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** 校验后台授予订阅的真实请求结构可以由 Jackson 3 反序列化。 */
class AiSubscriptionGrantRequestTest
{
    @Test
    void shouldDeserializeGrantRequest()
    {
        String json = "{\"planId\":6,\"startTime\":\"2026-09-10 08:00:00\","
                + "\"endTime\":\"2026-09-13 08:00:00\",\"remark\":\"新人试用\"}";

        AiSubscriptionGrantRequest request = new ObjectMapper().readValue(json, AiSubscriptionGrantRequest.class);

        assertEquals(6L, request.getPlanId());
        assertNotNull(request.getStartTime());
        assertNotNull(request.getEndTime());
        assertEquals("新人试用", request.getRemark());
    }
}
