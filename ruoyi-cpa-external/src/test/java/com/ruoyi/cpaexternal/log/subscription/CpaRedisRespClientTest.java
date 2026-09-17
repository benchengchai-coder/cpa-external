package com.ruoyi.cpaexternal.log.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** CLIProxyAPI RESP 编解码测试。 */
class CpaRedisRespClientTest
{
    @Test
    void shouldAuthenticateAndPopQueuedUsageItems() throws Exception
    {
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()))
        {
            Thread serverThread = Thread.ofPlatform().daemon(true).start(() ->
            {
                try (Socket connection = serverSocket.accept())
                {
                    assertEquals(List.of("AUTH", "management-key"),
                            CpaRedisRespClient.readRespValue(connection.getInputStream(), 1024));
                    OutputStream output = connection.getOutputStream();
                    output.write("+OK\r\n".getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    assertEquals(List.of("LPOP", "usage", "2"),
                            CpaRedisRespClient.readRespValue(connection.getInputStream(), 1024));
                    writeStringArray(output, List.of("{\"request_id\":\"one\"}", "{\"request_id\":\"two\"}"));
                }
                catch (Throwable exception)
                {
                    serverFailure.set(exception);
                }
            });

            CpaUsageSubscriptionProperties properties = new CpaUsageSubscriptionProperties();
            properties.setHost(InetAddress.getLoopbackAddress().getHostAddress());
            properties.setPort(serverSocket.getLocalPort());
            properties.setConnectTimeout(Duration.ofSeconds(1));
            properties.setReadTimeout(Duration.ofSeconds(1));
            try (CpaRedisRespClient client = CpaRedisRespClient.connect(properties))
            {
                client.authenticate("management-key");
                assertEquals(List.of("{\"request_id\":\"one\"}", "{\"request_id\":\"two\"}"),
                        client.popUsage(2));
            }
            serverThread.join(2000);
        }
        assertNull(serverFailure.get());
    }

    @Test
    void shouldSubscribeErrorsChannelAndReceiveMessage() throws Exception
    {
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()))
        {
            Thread serverThread = Thread.ofPlatform().daemon(true).start(() ->
            {
                try (Socket connection = serverSocket.accept())
                {
                    assertEquals(List.of("AUTH", "management-key"),
                            CpaRedisRespClient.readRespValue(connection.getInputStream(), 1024));
                    OutputStream output = connection.getOutputStream();
                    output.write("+OK\r\n".getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    assertEquals(List.of("SUBSCRIBE", "errors"),
                            CpaRedisRespClient.readRespValue(connection.getInputStream(), 1024));
                    output.write(("*3\r\n$9\r\nsubscribe\r\n$6\r\nerrors\r\n:1\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    writeStringArray(output, List.of("message", "errors", "{\"auth_index\":\"1\"}"));
                }
                catch (Throwable exception)
                {
                    serverFailure.set(exception);
                }
            });

            CpaUsageSubscriptionProperties properties = new CpaUsageSubscriptionProperties();
            properties.setHost(InetAddress.getLoopbackAddress().getHostAddress());
            properties.setPort(serverSocket.getLocalPort());
            properties.setConnectTimeout(Duration.ofSeconds(1));
            properties.setReadTimeout(Duration.ofSeconds(1));
            try (CpaRedisRespClient client = CpaRedisRespClient.connect(properties))
            {
                client.authenticate("management-key");
                client.subscribeErrors();
                assertEquals(List.of("message", "errors", "{\"auth_index\":\"1\"}"), client.readFrame());
            }
            serverThread.join(2000);
        }
        assertNull(serverFailure.get());
    }

    @Test
    void shouldRejectInvalidErrorsSubscribeHandshake() throws Exception
    {
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress()))
        {
            Thread serverThread = Thread.ofPlatform().daemon(true).start(() ->
            {
                try (Socket connection = serverSocket.accept())
                {
                    assertEquals(List.of("AUTH", "management-key"),
                            CpaRedisRespClient.readRespValue(connection.getInputStream(), 1024));
                    OutputStream output = connection.getOutputStream();
                    output.write("+OK\r\n".getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    CpaRedisRespClient.readRespValue(connection.getInputStream(), 1024);
                    output.write(("*3\r\n$9\r\nsubscribe\r\n$5\r\nusage\r\n:1\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    output.flush();
                }
                catch (Throwable exception)
                {
                    serverFailure.set(exception);
                }
            });

            CpaUsageSubscriptionProperties properties = new CpaUsageSubscriptionProperties();
            properties.setHost(InetAddress.getLoopbackAddress().getHostAddress());
            properties.setPort(serverSocket.getLocalPort());
            properties.setConnectTimeout(Duration.ofSeconds(1));
            properties.setReadTimeout(Duration.ofSeconds(1));
            try (CpaRedisRespClient client = CpaRedisRespClient.connect(properties))
            {
                client.authenticate("management-key");
                assertThrows(IOException.class, client::subscribeErrors);
            }
            serverThread.join(2000);
        }
        assertNull(serverFailure.get());
    }

    @Test
    void shouldEncodeCommandUsingUtf8ByteLength() throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        CpaRedisRespClient.writeCommand(output, "AUTH", "密钥");

        assertEquals("*2\r\n$4\r\nAUTH\r\n$6\r\n密钥\r\n",
                output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void shouldDecodeUsagePubSubMessage() throws IOException
    {
        String json = "{\"request_id\":\"req-1\"}";
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        String response = "*3\r\n$7\r\nmessage\r\n$5\r\nusage\r\n$"
                + jsonBytes.length + "\r\n" + json + "\r\n";

        Object frame = CpaRedisRespClient.readRespValue(
                new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), 1024);

        List<?> values = assertInstanceOf(List.class, frame);
        assertEquals(List.of("message", "usage", json), values);
    }

    @Test
    void shouldRejectRedisErrorResponse()
    {
        ByteArrayInputStream input = new ByteArrayInputStream(
                "-ERR invalid management key\r\n".getBytes(StandardCharsets.UTF_8));

        assertThrows(CpaRedisRespClient.CpaRedisCommandException.class,
                () -> CpaRedisRespClient.readRespValue(input, 1024));
    }

    @Test
    void shouldRejectOversizedPayload()
    {
        ByteArrayInputStream input = new ByteArrayInputStream(
                "$20\r\n".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> CpaRedisRespClient.readRespValue(input, 10));
    }

    private static void writeStringArray(OutputStream output, List<String> values) throws IOException
    {
        output.write(("*" + values.size() + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String value : values)
        {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        output.flush();
    }
}
