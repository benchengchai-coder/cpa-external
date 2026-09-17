package com.ruoyi.cpaexternal.log.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailureEvent;
import com.ruoyi.cpaexternal.log.service.ICpaUpstreamFailureService;
import tools.jackson.databind.ObjectMapper;

/** CLIProxyAPI errors 通道失败事件消费测试。 */
class CpaErrorEventSubscriberTest
{
    @Test
    void shouldAuthenticateSubscribeAndConsumeErrorMessage() throws Exception
    {
        CountDownLatch persisted = new CountDownLatch(1);
        ICpaUpstreamFailureService failureService = mock(ICpaUpstreamFailureService.class);
        doAnswer(_invocation ->
        {
            persisted.countDown();
            return null;
        }).when(failureService).record(any(CpaUpstreamFailureEvent.class));
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();

        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()))
        {
            Thread serverThread = Thread.ofPlatform().daemon(true).start(() ->
            {
                try (Socket connection = serverSocket.accept())
                {
                    Object auth = CpaRedisRespClient.readRespValue(connection.getInputStream(), 1024);
                    assertEquals(List.of("AUTH", "management-key"), auth);
                    OutputStream output = connection.getOutputStream();
                    output.write("+OK\r\n".getBytes(StandardCharsets.UTF_8));
                    output.flush();

                    Object subscribe = CpaRedisRespClient.readRespValue(connection.getInputStream(), 1024);
                    assertEquals(List.of("SUBSCRIBE", "errors"), subscribe);
                    output.write(("*3\r\n$9\r\nsubscribe\r\n$6\r\nerrors\r\n:1\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    writeErrorMessage(output, "{\"timestamp\":\"2026-09-17T03:00:00Z\","
                            + "\"auth_index\":\"2\",\"status_code\":429,\"body\":\"rate limited\"}");
                }
                catch (Throwable exception)
                {
                    serverFailure.set(exception);
                }
            });

            CpaErrorEventSubscriber subscriber = newSubscriber(failureService, serverSocket);
            try
            {
                subscriber.start();
                assertTrue(persisted.await(5, TimeUnit.SECONDS));
            }
            finally
            {
                subscriber.stop();
            }
            serverThread.join(2000);
        }

        assertNull(serverFailure.get());
        ArgumentCaptor<CpaUpstreamFailureEvent> captor = ArgumentCaptor.forClass(CpaUpstreamFailureEvent.class);
        verify(failureService).record(captor.capture());
        assertEquals("2", captor.getValue().getAuthIndex());
        assertEquals(429, captor.getValue().getStatusCode());
    }

    @Test
    void shouldDeserializeAndPersistCurrentErrorEvent()
    {
        ICpaUpstreamFailureService failureService = mock(ICpaUpstreamFailureService.class);
        CpaErrorEventSubscriber subscriber = newSubscriber(failureService, null);
        String json = "{"
                + "\"timestamp\":\"2026-09-17T10:59:53.738095654+08:00\","
                + "\"provider\":\"openai\",\"model\":\"gpt-5.4\",\"auth_id\":\"auth-1\","
                + "\"auth_index\":\"2\",\"status_code\":429,\"body\":\"rate limited\","
                + "\"code\":\"upstream_rate_limited\",\"retryable\":true,"
                + "\"auth_status\":{\"status\":\"active\",\"disabled\":false,\"unavailable\":true,"
                + "\"next_retry_after\":\"2026-09-17T11:00:23Z\","
                + "\"quota\":{\"exceeded\":true,\"reason\":\"rate_limit\",\"backoff_level\":2}}"
                + "}";

        subscriber.processPayload(json);

        ArgumentCaptor<CpaUpstreamFailureEvent> captor = ArgumentCaptor.forClass(CpaUpstreamFailureEvent.class);
        verify(failureService).record(captor.capture());
        CpaUpstreamFailureEvent event = captor.getValue();
        assertEquals("openai", event.getProvider());
        assertEquals("gpt-5.4", event.getModel());
        assertEquals("auth-1", event.getAuthId());
        assertEquals(429, event.getStatusCode());
        assertEquals(Boolean.TRUE, event.getRetryable());
        assertEquals("upstream_rate_limited", event.getCode());
        assertEquals(Boolean.TRUE, event.getAuthStatus().getUnavailable());
        assertEquals("rate_limit", event.getAuthStatus().getQuota().getReason());
    }

    @Test
    void shouldIgnoreInvalidErrorPayloads()
    {
        ICpaUpstreamFailureService failureService = mock(ICpaUpstreamFailureService.class);
        CpaErrorEventSubscriber subscriber = newSubscriber(failureService, null);

        subscriber.processPayload("");
        subscriber.processPayload("not-json");
        subscriber.processPayload("{\"timestamp\":\"2026-09-17T03:00:00Z\",\"status_code\":500}");
        subscriber.processPayload(null);

        verify(failureService, never()).record(any(CpaUpstreamFailureEvent.class));
    }

    @Test
    void shouldKeepConsumingWhenPersistKeepsFailing()
    {
        ICpaUpstreamFailureService failureService = mock(ICpaUpstreamFailureService.class);
        doThrow(new RuntimeException("db down")).when(failureService).record(any(CpaUpstreamFailureEvent.class));
        CpaErrorEventSubscriber subscriber = newSubscriber(failureService, null);

        subscriber.processPayload("{\"timestamp\":\"2026-09-17T03:00:00Z\",\"auth_index\":\"1\","
                + "\"status_code\":500,\"body\":\"request failed\"}");

        verify(failureService, times(3)).record(any(CpaUpstreamFailureEvent.class));
    }

    @Test
    void shouldSkipNonRetryableRecordFailureImmediately()
    {
        ICpaUpstreamFailureService failureService = mock(ICpaUpstreamFailureService.class);
        doThrow(new ServiceException("非法事件")).when(failureService).record(any(CpaUpstreamFailureEvent.class));
        CpaErrorEventSubscriber subscriber = newSubscriber(failureService, null);

        subscriber.processPayload("{\"timestamp\":\"2026-09-17T03:00:00Z\",\"auth_index\":\"1\","
                + "\"status_code\":500,\"body\":\"request failed\"}");

        verify(failureService, times(1)).record(any(CpaUpstreamFailureEvent.class));
    }

    @Test
    void shouldRejectInvalidConnectionConfigBeforeStarting()
    {
        ICpaUpstreamFailureService failureService = mock(ICpaUpstreamFailureService.class);
        CpaUsageSubscriptionProperties connectionProperties = new CpaUsageSubscriptionProperties();
        connectionProperties.setHost("127.0.0.1");
        connectionProperties.setPort(8317);
        connectionProperties.setManagementKey("");
        CpaErrorEventSubscriber subscriber = new CpaErrorEventSubscriber(connectionProperties,
                new CpaErrorsSubscriptionProperties(), failureService, new ObjectMapper());

        subscriber.start();

        assertFalse(subscriber.isRunning());
    }

    private CpaErrorEventSubscriber newSubscriber(ICpaUpstreamFailureService failureService,
            ServerSocket serverSocket)
    {
        CpaUsageSubscriptionProperties connectionProperties = new CpaUsageSubscriptionProperties();
        connectionProperties.setHost("127.0.0.1");
        connectionProperties.setPort(serverSocket == null ? 8317 : serverSocket.getLocalPort());
        connectionProperties.setManagementKey("management-key");
        connectionProperties.setConnectTimeout(Duration.ofSeconds(1));
        connectionProperties.setReadTimeout(Duration.ofSeconds(1));
        connectionProperties.setReconnectDelay(Duration.ofSeconds(1));
        connectionProperties.setMaxReconnectDelay(Duration.ofSeconds(2));
        return new CpaErrorEventSubscriber(connectionProperties, new CpaErrorsSubscriptionProperties(),
                failureService, new ObjectMapper());
    }

    private static void writeErrorMessage(OutputStream output, String payload) throws Exception
    {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        output.write(("*3\r\n$7\r\nmessage\r\n$6\r\nerrors\r\n$" + bytes.length + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
}
