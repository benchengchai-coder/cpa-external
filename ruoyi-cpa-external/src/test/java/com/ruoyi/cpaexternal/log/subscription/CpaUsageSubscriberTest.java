package com.ruoyi.cpaexternal.log.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.ruoyi.cpaexternal.log.domain.CpaAiLogPayload;
import com.ruoyi.cpaexternal.log.service.ICpaAiLogService;
import tools.jackson.databind.ObjectMapper;

/** CLIProxyAPI usage 消息消费测试。 */
class CpaUsageSubscriberTest
{
    @Test
    void shouldAuthenticateSubscribeAndConsumeUsageMessage() throws Exception
    {
        CountDownLatch persisted = new CountDownLatch(1);
        ICpaAiLogService aiLogService = mock(ICpaAiLogService.class);
        when(aiLogService.ingest(any(CpaAiLogPayload.class))).thenAnswer(invocation ->
        {
            persisted.countDown();
            return null;
        });
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
                    assertEquals(List.of("SUBSCRIBE", "usage"), subscribe);
                    output.write(("*3\r\n$9\r\nsubscribe\r\n$5\r\nusage\r\n:1\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    writeUsageMessage(output, "{\"support_refresh\":true}");
                    writeUsageMessage(output, "{\"timestamp\":\"2026-09-10T00:00:00Z\","
                            + "\"request_id\":\"req-live-1\",\"tokens\":{\"total_tokens\":7}}");
                }
                catch (Throwable exception)
                {
                    serverFailure.set(exception);
                }
            });

            CpaUsageSubscriptionProperties properties = validProperties();
            properties.setHost(InetAddress.getLoopbackAddress().getHostAddress());
            properties.setPort(serverSocket.getLocalPort());
            CpaUsageSubscriber subscriber = new CpaUsageSubscriber(properties, aiLogService,
                    new ObjectMapper());
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
        ArgumentCaptor<CpaAiLogPayload> captor = ArgumentCaptor.forClass(CpaAiLogPayload.class);
        verify(aiLogService).ingest(captor.capture());
        assertEquals("req-live-1", captor.getValue().getRequestId());
    }

    @Test
    void shouldDeserializeAndPersistCurrentUsagePayload()
    {
        ICpaAiLogService aiLogService = mock(ICpaAiLogService.class);
        CpaUsageSubscriber subscriber = new CpaUsageSubscriber(validProperties(), aiLogService,
                new ObjectMapper());
        String json = "{"
                + "\"timestamp\":\"2026-09-10T00:00:00Z\",\"latency_ms\":1250,\"ttft_ms\":120,"
                + "\"source\":\"account@example.com\",\"auth_index\":\"2\","
                + "\"client_ip\":\"203.0.113.10\",\"user_agent\":\"Codex Desktop/0.153.4\","
                + "\"tokens\":{\"input_tokens\":12,\"output_tokens\":8,\"reasoning_tokens\":3,"
                + "\"cached_tokens\":4,\"cache_read_tokens\":4,\"cache_creation_tokens\":1,"
                + "\"cache_read_tokens_present\":true,\"total_tokens\":20},"
                + "\"failed\":false,\"generate\":true,\"stream\":false,"
                + "\"fail\":{\"status_code\":200,\"body\":\"\"},"
                + "\"provider\":\"openai\",\"executor_type\":\"OpenAIResponsesExecutor\","
                + "\"model\":\"gpt-5.4\","
                + "\"alias\":\"gpt-main\",\"endpoint\":\"POST /v1/responses\","
                + "\"auth_type\":\"apikey\",\"api_key\":\"sk-user\","
                + "\"request_id\":\"req-usage-1\",\"session_id\":\"session-1\","
                + "\"reasoning_effort\":\"medium\",\"service_tier\":\"auto\","
                + "\"accounting_version\":2,"
                + "\"token_breakdown\":{\"schema_version\":2,\"quality\":\"complete\",\"total_tokens\":20,"
                + "\"input\":{\"total_tokens\":12,\"uncached_tokens\":8,\"cache_read_tokens\":4,\"cache_write_tokens\":0},"
                + "\"output\":{\"total_tokens\":8,\"non_reasoning_tokens\":5,\"reasoning_tokens\":3},"
                + "\"unclassified_tokens\":0},"
                + "\"response_headers\":{\"X-Request-Id\":[\"upstream-1\"]}}";

        subscriber.processPayload(json);

        ArgumentCaptor<CpaAiLogPayload> captor = ArgumentCaptor.forClass(CpaAiLogPayload.class);
        verify(aiLogService).ingest(captor.capture());
        CpaAiLogPayload payload = captor.getValue();
        assertEquals("req-usage-1", payload.getRequestId());
        assertEquals(120, payload.getTtftMs());
        assertEquals("203.0.113.10", payload.getClientIp());
        assertEquals(12, payload.getTokens().getInputTokens());
        assertEquals(1, payload.getTokens().getCacheCreationTokens());
        assertEquals(20, payload.getTokens().getTotalTokens());
        assertEquals(Boolean.FALSE, payload.getStream());
        assertEquals(200, payload.getFail().getStatusCode());
        assertEquals("session-1", payload.getSessionId());
        assertEquals("medium", payload.getReasoningEffort());
        assertEquals(20, payload.getTokenBreakdown().getTotalTokens());
        assertEquals("OpenAIResponsesExecutor", payload.getExecutorType());
    }

    @Test
    void shouldIgnoreCliProxyControlMessages()
    {
        ICpaAiLogService aiLogService = mock(ICpaAiLogService.class);
        CpaUsageSubscriber subscriber = new CpaUsageSubscriber(validProperties(), aiLogService,
                new ObjectMapper());

        subscriber.processPayload("{\"support_refresh\":true}");
        subscriber.processPayload("{\"refresh\":true}");

        verify(aiLogService, never()).ingest(any(CpaAiLogPayload.class));
    }

    @Test
    void shouldRejectMissingManagementKeyBeforeStarting()
    {
        CpaUsageSubscriptionProperties properties = validProperties();
        properties.setManagementKey("");
        CpaUsageSubscriber subscriber = new CpaUsageSubscriber(properties,
                mock(ICpaAiLogService.class), new ObjectMapper());

        subscriber.start();

        assertEquals(false, subscriber.isRunning());
    }

    private CpaUsageSubscriptionProperties validProperties()
    {
        CpaUsageSubscriptionProperties properties = new CpaUsageSubscriptionProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(8317);
        properties.setManagementKey("management-key");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        properties.setReconnectDelay(Duration.ofSeconds(1));
        properties.setMaxReconnectDelay(Duration.ofSeconds(2));
        return properties;
    }

    private static void writeUsageMessage(OutputStream output, String payload) throws Exception
    {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        output.write(("*3\r\n$7\r\nmessage\r\n$5\r\nusage\r\n$" + bytes.length + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
}
