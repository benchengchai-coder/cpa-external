package com.ruoyi.cpaexternal.log.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** 校验 CLIProxyAPI 实际日志结构可以被 Spring Boot 4 的 Jackson 3 反序列化。 */
class CpaAiLogPayloadTest
{
    @Test
    void shouldDeserializeCliProxyApiLog()
    {
        String json = "{"
                + "\"timestamp\":\"2026-04-25T00:00:00Z\","
                + "\"latency_ms\":1500,"
                + "\"ttft_ms\":250,"
                + "\"source\":\"user@example.com\","
                + "\"auth_index\":\"0\","
                + "\"tokens\":{\"input_tokens\":10,\"output_tokens\":20,"
                + "\"reasoning_tokens\":0,\"cached_tokens\":0,\"total_tokens\":30},"
                + "\"failed\":false,\"provider\":\"openai\",\"model\":\"gpt-5.4\","
                + "\"alias\":\"client-gpt\",\"endpoint\":\"POST /v1/chat/completions\","
                + "\"auth_type\":\"apikey\",\"api_key\":\"test-key\","
                + "\"request_id\":\"ctx-request-id\","
                + "\"accounting_version\":2,"
                + "\"response_headers\":{\"Retry-After\":[\"30\"]}"
                + "}";

        ObjectMapper objectMapper = new ObjectMapper();
        CpaAiLogPayload payload = objectMapper.readValue(json, CpaAiLogPayload.class);

        assertEquals("2026-04-25T00:00:00Z", payload.getTimestamp());
        assertEquals(1500, payload.getLatencyMs());
        assertEquals("0", payload.getAuthIndex());
        assertEquals(10, payload.getTokens().getInputTokens());
        assertEquals(30, payload.getTokens().getTotalTokens());
        assertEquals("test-key", payload.getApiKey());
        assertEquals("30", payload.getResponseHeaders().get("Retry-After").get(0));
    }

    /** 使用 CLIProxyAPI 真实上报的完整日志结构，覆盖全部新增字段。 */
    @Test
    void shouldDeserializeCurrentUsagePayloadWithAllTopLevelFields()
    {
        String json = "{"
                + "\"timestamp\":\"2026-09-10T10:12:12.571678818+08:00\","
                + "\"latency_ms\":4394,"
                + "\"ttft_ms\":4176,"
                + "\"source\":\"15902193@qq.com\","
                + "\"auth_index\":\"7ec3fad61a5247ba\","
                + "\"access_token_sha256\":\"9d7827f6095541329f5ee4b87c352d55e101a8cd99601e12d0062f01d6494ad9\","
                + "\"client_ip\":\"38.175.103.97\","
                + "\"x_forwarded_for\":\"\","
                + "\"user_agent\":\"Codex Desktop/0.153.4 (Windows 10.0.26100; x86_64)\","
                + "\"tokens\":{\"input_tokens\":18159,\"output_tokens\":5,\"reasoning_tokens\":0,"
                + "\"cached_tokens\":17152,\"cache_read_tokens\":17152,\"cache_read_tokens_present\":true,"
                + "\"cache_creation_tokens\":0,\"total_tokens\":18164},"
                + "\"failed\":false,"
                + "\"generate\":true,"
                + "\"stream\":true,"
                + "\"fail\":{\"status_code\":200,\"body\":\"\"},"
                + "\"response_headers\":{\"X-Codex-Plan-Type\":[\"plus\"]},"
                + "\"accounting_version\":2,"
                + "\"token_breakdown\":{\"schema_version\":2,\"quality\":\"complete\",\"total_tokens\":18164,"
                + "\"input\":{\"total_tokens\":18159,\"uncached_tokens\":1007,\"cache_read_tokens\":17152,\"cache_write_tokens\":0},"
                + "\"output\":{\"total_tokens\":5,\"non_reasoning_tokens\":5,\"reasoning_tokens\":0},"
                + "\"unclassified_tokens\":0},"
                + "\"provider\":\"codex\","
                + "\"executor_type\":\"CodexExecutor\","
                + "\"model\":\"gpt-5.6-luna\","
                + "\"alias\":\"gpt-5.6-luna\","
                + "\"endpoint\":\"POST /v1/responses\","
                + "\"auth_type\":\"oauth\","
                + "\"api_key\":\"sk-test-key\","
                + "\"request_id\":\"8d7a173f\","
                + "\"session_id\":\"01a0890d-69b6-7a22-ab93-5993ffddfd73\","
                + "\"parent_session_id\":\"00000000-0000-0000-0000-000000000000\","
                + "\"reasoning_effort\":\"medium\","
                + "\"service_tier\":\"auto\","
                + "\"response_service_tier\":\"default\""
                + "}";

        ObjectMapper objectMapper = new ObjectMapper();
        CpaAiLogPayload payload = objectMapper.readValue(json, CpaAiLogPayload.class);

        assertEquals("2026-09-10T10:12:12.571678818+08:00", payload.getTimestamp());
        assertEquals(4394, payload.getLatencyMs());
        assertEquals(4176, payload.getTtftMs());
        assertEquals("15902193@qq.com", payload.getSource());
        assertEquals("7ec3fad61a5247ba", payload.getAuthIndex());
        assertEquals("9d7827f6095541329f5ee4b87c352d55e101a8cd99601e12d0062f01d6494ad9", payload.getAccessTokenSha256());
        assertEquals("38.175.103.97", payload.getClientIp());
        assertEquals("", payload.getXForwardedFor());
        assertEquals("Codex Desktop/0.153.4 (Windows 10.0.26100; x86_64)", payload.getUserAgent());

        CpaAiLogTokens tokens = payload.getTokens();
        assertEquals(18159, tokens.getInputTokens());
        assertEquals(5, tokens.getOutputTokens());
        assertEquals(0, tokens.getReasoningTokens());
        assertEquals(17152, tokens.getCachedTokens());
        assertEquals(17152, tokens.getCacheReadTokens());
        assertTrue(Boolean.TRUE.equals(tokens.getCacheReadTokensPresent()));
        assertEquals(0, tokens.getCacheCreationTokens());
        assertEquals(18164, tokens.getTotalTokens());

        assertEquals(Boolean.FALSE, payload.getFailed());
        assertEquals(Boolean.TRUE, payload.getGenerate());
        assertEquals(Boolean.TRUE, payload.getStream());
        assertEquals(200, payload.getFail().getStatusCode());
        assertEquals("", payload.getFail().getBody());
        assertEquals("plus", payload.getResponseHeaders().get("X-Codex-Plan-Type").get(0));
        assertEquals(2, payload.getAccountingVersion());

        CpaAiLogTokenBreakdown breakdown = payload.getTokenBreakdown();
        assertEquals(2, breakdown.getSchemaVersion());
        assertEquals("complete", breakdown.getQuality());
        assertEquals(18164, breakdown.getTotalTokens());
        assertEquals(18159, breakdown.getInput().getTotalTokens());
        assertEquals(1007, breakdown.getInput().getUncachedTokens());
        assertEquals(17152, breakdown.getInput().getCacheReadTokens());
        assertEquals(0, breakdown.getInput().getCacheWriteTokens());
        assertEquals(5, breakdown.getOutput().getTotalTokens());
        assertEquals(5, breakdown.getOutput().getNonReasoningTokens());
        assertEquals(0, breakdown.getOutput().getReasoningTokens());
        assertEquals(0, breakdown.getUnclassifiedTokens());

        assertEquals("codex", payload.getProvider());
        assertEquals("CodexExecutor", payload.getExecutorType());
        assertEquals("gpt-5.6-luna", payload.getModel());
        assertEquals("gpt-5.6-luna", payload.getAlias());
        assertEquals("POST /v1/responses", payload.getEndpoint());
        assertEquals("oauth", payload.getAuthType());
        assertEquals("sk-test-key", payload.getApiKey());
        assertEquals("8d7a173f", payload.getRequestId());
        assertEquals("01a0890d-69b6-7a22-ab93-5993ffddfd73", payload.getSessionId());
        assertEquals("00000000-0000-0000-0000-000000000000", payload.getParentSessionId());
        assertEquals("medium", payload.getReasoningEffort());
        assertEquals("auto", payload.getServiceTier());
        assertEquals("default", payload.getResponseServiceTier());
    }
}
