package com.ruoyi.cpaexternal.log.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** CLIProxyAPI 失败事件载荷反序列化测试（按 errors 通道真实消息结构）。 */
class CpaUpstreamFailureEventTest
{
    @Test
    void shouldDeserializeFullErrorEvent()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{"
                + "\"timestamp\":\"2026-09-17T10:59:53.738095654+08:00\","
                + "\"provider\":\"openai\","
                + "\"model\":\"gpt-5.4\","
                + "\"auth_id\":\"auth-1\","
                + "\"auth_index\":\"2\","
                + "\"status_code\":429,"
                + "\"body\":\"{\\\"error\\\":\\\"quota\\\"}\","
                + "\"code\":\"upstream_rate_limited\","
                + "\"retryable\":true,"
                + "\"auth_status\":{"
                + "\"status\":\"active\","
                + "\"status_message\":\"rate limited\","
                + "\"disabled\":false,"
                + "\"unavailable\":true,"
                + "\"next_retry_after\":\"2026-09-17T11:00:23Z\","
                + "\"quota\":{\"exceeded\":true,\"reason\":\"rate_limit\","
                + "\"next_recover_at\":\"2026-09-17T11:05:00Z\",\"backoff_level\":2},"
                + "\"model\":{\"name\":\"gpt-5.4\",\"status\":\"active\","
                + "\"status_message\":\"\",\"unavailable\":true,"
                + "\"next_retry_after\":\"2026-09-17T11:00:23Z\","
                + "\"quota\":{\"exceeded\":false}}"
                + "}}";

        CpaUpstreamFailureEvent event = objectMapper.readValue(json, CpaUpstreamFailureEvent.class);

        assertEquals("2026-09-17T10:59:53.738095654+08:00", event.getTimestamp());
        assertEquals("openai", event.getProvider());
        assertEquals("gpt-5.4", event.getModel());
        assertEquals("auth-1", event.getAuthId());
        assertEquals("2", event.getAuthIndex());
        assertEquals(429, event.getStatusCode());
        assertEquals("{\"error\":\"quota\"}", event.getBody());
        assertEquals("upstream_rate_limited", event.getCode());
        assertEquals(Boolean.TRUE, event.getRetryable());

        CpaErrorEventAuthStatus authStatus = event.getAuthStatus();
        assertEquals("active", authStatus.getStatus());
        assertEquals("rate limited", authStatus.getStatusMessage());
        assertEquals(Boolean.FALSE, authStatus.getDisabled());
        assertEquals(Boolean.TRUE, authStatus.getUnavailable());
        assertEquals("2026-09-17T11:00:23Z", authStatus.getNextRetryAfter());
        assertEquals("rate_limit", authStatus.getQuota().getReason());
        assertEquals(2, authStatus.getQuota().getBackoffLevel());
        assertEquals("gpt-5.4", authStatus.getModel().getName());
        assertTrue(authStatus.getModel().getUnavailable());
        assertFalse(authStatus.getModel().getQuota().getExceeded());
    }

    @Test
    void shouldDeserializeMinimalErrorEventWithOmittedFields()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{"
                + "\"timestamp\":\"2026-09-17T03:00:00Z\","
                + "\"auth_index\":\"1\","
                + "\"status_code\":500,"
                + "\"body\":\"request failed\","
                + "\"auth_status\":{\"status\":\"active\",\"disabled\":false,\"unavailable\":false}"
                + "}";

        CpaUpstreamFailureEvent event = objectMapper.readValue(json, CpaUpstreamFailureEvent.class);

        assertNull(event.getProvider());
        assertNull(event.getModel());
        assertNull(event.getAuthId());
        assertNull(event.getCode());
        assertNull(event.getRetryable());
        assertNull(event.getAuthStatus().getNextRetryAfter());
        assertNull(event.getAuthStatus().getQuota());
        assertNull(event.getAuthStatus().getModel());
        assertEquals(Boolean.FALSE, event.getAuthStatus().getDisabled());
    }

    @Test
    void shouldIgnoreUnknownFields()
    {
        ObjectMapper objectMapper = new ObjectMapper();

        CpaUpstreamFailureEvent event = objectMapper.readValue(
                "{\"auth_index\":\"1\",\"status_code\":502,\"body\":\"x\",\"future_field\":true}",
                CpaUpstreamFailureEvent.class);

        assertEquals("1", event.getAuthIndex());
        assertEquals(502, event.getStatusCode());
    }
}
