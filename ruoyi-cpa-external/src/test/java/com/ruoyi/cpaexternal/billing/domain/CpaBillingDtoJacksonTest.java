package com.ruoyi.cpaexternal.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * 公开计费 API 的 DTO 与真实请求/响应结构互转测试。
 *
 * <p>HTTP 数据绑定使用 Jackson 3，字段名采用 CLIProxyAPI 侧的 snake_case。</p>
 */
class CpaBillingDtoJacksonTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** CLIProxyAPI 插件按真实结构提交预占请求（含未知字段时必须容忍）。 */
    @Test
    void reserveRequestShouldDeserializeFromRealPluginJson()
    {
        String json = "{\"request_id\":\"1a2b3c4d\",\"api_key\":\"sk-plain-key-12345\","
                + "\"model\":\"gpt-4o\",\"extra_field\":{\"ignored\":true}}";

        CpaBillingReserveRequest request = objectMapper.readValue(json, CpaBillingReserveRequest.class);

        assertEquals("1a2b3c4d", request.getRequestId());
        assertEquals("sk-plain-key-12345", request.getApiKey());
    }

    @Test
    void releaseRequestShouldDeserializeFromRealPluginJson()
    {
        String json = "{\"request_id\":\"1a2b3c4d\",\"reason\":\"rejected_by_gate\"}";

        CpaBillingReleaseRequest request = objectMapper.readValue(json, CpaBillingReleaseRequest.class);

        assertEquals("1a2b3c4d", request.getRequestId());
        assertEquals("rejected_by_gate", request.getReason());
    }

    /** 预占结果必须以 snake_case 返回给 CLIProxyAPI。 */
    @Test
    void reserveResultShouldSerializeSnakeCaseFields()
    {
        CpaBillingReserveResult result = new CpaBillingReserveResult();
        result.setAllowed(true);
        result.setRequestId("1a2b3c4d");
        result.setBillingId(600L);
        result.setUserId(100L);
        result.setKeyId(5L);
        result.setSubscriptionId(12L);
        result.setBillingPreference("subscription_first");
        result.setReservedAmount(new BigDecimal("0.01"));
        result.setWalletReservedAmount(new BigDecimal("0.01"));
        result.setSubscriptionReservedAmount(BigDecimal.ZERO);
        result.setKeyReservedAmount(new BigDecimal("0.01"));

        String json = objectMapper.writeValueAsString(result);

        assertTrue(json.contains("\"allowed\":true"));
        assertTrue(json.contains("\"request_id\":\"1a2b3c4d\""));
        assertTrue(json.contains("\"billing_id\":600"));
        assertTrue(json.contains("\"user_id\":100"));
        assertTrue(json.contains("\"key_id\":5"));
        assertTrue(json.contains("\"subscription_id\":12"));
        assertTrue(json.contains("\"billing_preference\":\"subscription_first\""));
        assertTrue(json.contains("\"reserved_amount\":0.01"));
        assertTrue(json.contains("\"wallet_reserved_amount\":0.01"));
        assertTrue(json.contains("\"subscription_reserved_amount\":0"));
        assertTrue(json.contains("\"key_reserved_amount\":0.01"));
        assertFalse(json.contains("\"requestId\""));
    }

    /** 并发字段以 snake_case 序列化，且能从真实响应结构反序列化（容忍未知字段）。 */
    @Test
    void reserveResultShouldRoundTripConcurrencyFields()
    {
        CpaBillingReserveResult result = new CpaBillingReserveResult();
        result.setAllowed(false);
        result.setRequestId("1a2b3c4d");
        result.setConcurrencyLimit(100);
        result.setActiveRequestCount(100);
        result.setReason("AI并发数已达上限(100)");

        String json = objectMapper.writeValueAsString(result);

        assertTrue(json.contains("\"concurrency_limit\":100"));
        assertTrue(json.contains("\"active_request_count\":100"));
        assertTrue(json.contains("\"reason\":\"AI并发数已达上限(100)\""));

        String responseJson = "{\"allowed\":true,\"request_id\":\"1a2b3c4d\",\"billing_id\":600,"
                + "\"user_id\":100,\"key_id\":5,\"concurrency_limit\":50,\"active_request_count\":3,"
                + "\"reason\":null,\"extra_field\":\"ignored\"}";
        CpaBillingReserveResult parsed = objectMapper.readValue(responseJson, CpaBillingReserveResult.class);

        assertTrue(parsed.isAllowed());
        assertEquals(Integer.valueOf(50), parsed.getConcurrencyLimit());
        assertEquals(Integer.valueOf(3), parsed.getActiveRequestCount());
    }
}
