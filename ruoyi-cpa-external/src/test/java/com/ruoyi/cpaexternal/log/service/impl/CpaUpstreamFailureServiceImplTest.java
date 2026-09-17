package com.ruoyi.cpaexternal.log.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.cpaexternal.log.domain.CpaErrorEventAuthStatus;
import com.ruoyi.cpaexternal.log.domain.CpaErrorEventQuotaStatus;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailure;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailureEvent;
import com.ruoyi.cpaexternal.log.mapper.CpaUpstreamFailureMapper;
import tools.jackson.databind.ObjectMapper;

/** 上游失败事件落库映射测试。 */
class CpaUpstreamFailureServiceImplTest
{
    @Test
    void shouldMapErrorEventToEntityWithFlattenedColumns()
    {
        CpaUpstreamFailureMapper mapper = Mockito.mock(CpaUpstreamFailureMapper.class);
        CpaUpstreamFailureServiceImpl service = new CpaUpstreamFailureServiceImpl();
        ReflectionTestUtils.setField(service, "upstreamFailureMapper", mapper);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        CpaUpstreamFailureEvent event = new CpaUpstreamFailureEvent();
        event.setTimestamp("2026-09-17T10:59:53.738095654+08:00");
        event.setProvider("openai");
        event.setModel("gpt-5.4");
        event.setAuthId("auth-1");
        event.setAuthIndex("2");
        event.setStatusCode(429);
        event.setBody("rate limited");
        event.setCode("upstream_rate_limited");
        event.setRetryable(true);
        CpaErrorEventAuthStatus authStatus = new CpaErrorEventAuthStatus();
        authStatus.setStatus("active");
        authStatus.setStatusMessage("rate limited");
        authStatus.setDisabled(false);
        authStatus.setUnavailable(true);
        authStatus.setNextRetryAfter("2026-09-17T11:00:23Z");
        CpaErrorEventQuotaStatus quota = new CpaErrorEventQuotaStatus();
        quota.setExceeded(true);
        quota.setReason("rate_limit");
        quota.setBackoffLevel(2);
        authStatus.setQuota(quota);
        event.setAuthStatus(authStatus);

        service.record(event);

        ArgumentCaptor<CpaUpstreamFailure> captor = ArgumentCaptor.forClass(CpaUpstreamFailure.class);
        Mockito.verify(mapper).insert(captor.capture());
        CpaUpstreamFailure failure = captor.getValue();
        assertNotNull(failure.getEventTime());
        assertEquals("openai", failure.getProvider());
        assertEquals("gpt-5.4", failure.getModel());
        assertEquals("auth-1", failure.getAuthId());
        assertEquals("2", failure.getAuthIndex());
        assertEquals(429, failure.getStatusCode());
        assertEquals("rate limited", failure.getBody());
        assertEquals("upstream_rate_limited", failure.getCode());
        assertEquals(Boolean.TRUE, failure.getRetryable());
        assertEquals("active", failure.getAuthStatus());
        assertEquals(Boolean.FALSE, failure.getAuthDisabled());
        assertEquals(Boolean.TRUE, failure.getAuthUnavailable());
        assertNotNull(failure.getAuthNextRetryAt());
        assertEquals(Boolean.TRUE, failure.getQuotaExceeded());
        assertEquals("rate_limit", failure.getQuotaReason());
        assertTrue(failure.getAuthStatusSnapshot().contains("\"status_message\""));
        assertTrue(failure.getAuthStatusSnapshot().contains("\"backoff_level\":2"));
    }

    @Test
    void shouldFallBackToNowWhenEventTimeUnparsable()
    {
        CpaUpstreamFailureMapper mapper = Mockito.mock(CpaUpstreamFailureMapper.class);
        CpaUpstreamFailureServiceImpl service = new CpaUpstreamFailureServiceImpl();
        ReflectionTestUtils.setField(service, "upstreamFailureMapper", mapper);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        CpaUpstreamFailureEvent event = new CpaUpstreamFailureEvent();
        event.setTimestamp("not-a-date");
        event.setAuthIndex("1");
        event.setStatusCode(500);

        service.record(event);

        ArgumentCaptor<CpaUpstreamFailure> captor = ArgumentCaptor.forClass(CpaUpstreamFailure.class);
        Mockito.verify(mapper).insert(captor.capture());
        CpaUpstreamFailure failure = captor.getValue();
        assertNotNull(failure.getEventTime());
        assertNull(failure.getAuthStatus());
        // 无快照时 getter 返回 "{}"（与 ai_log.fail 的既有约定一致），JSON 列不会出现非法值
        assertEquals("{}", failure.getAuthStatusSnapshot());
        assertEquals(Boolean.FALSE, failure.getRetryable());
        assertTrue(failure.getEventTime().getTime() <= System.currentTimeMillis());
    }
}
