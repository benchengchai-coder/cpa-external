package com.ruoyi.cpaexternal.apikey.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.cpaexternal.apikey.config.CpaManagementProperties;
import tools.jackson.databind.ObjectMapper;

/** CLIProxyAPI 管理接口客户端测试（基于 JDK 内置 HttpServer 模拟 CPA）。 */
class CpaManagementClientTest
{
    private static final String MANAGEMENT_KEY = "test-management-key";

    private HttpServer server;
    private CpaManagementClient client;
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();

        CpaManagementProperties properties = new CpaManagementProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(server.getAddress().getPort());
        properties.setManagementKey(MANAGEMENT_KEY);
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(2));
        client = new CpaManagementClient(properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown()
    {
        server.stop(0);
    }

    @Test
    void shouldUnwrapApiKeysResponse()
    {
        register("/v0/management/api-keys", "{\"api-keys\":[\"sk-1\",\"sk-2\"]}");
        assertEquals(List.of("sk-1", "sk-2"), client.listApiKeys());
        assertEquals("Bearer " + MANAGEMENT_KEY, lastAuthorization.get());
    }

    @Test
    void shouldPutApiKeysAsRawJsonArray()
    {
        AtomicReference<String> lastMethod = new AtomicReference<>();
        AtomicReference<String> lastBody = new AtomicReference<>();
        server.createContext("/v0/management/api-keys", exchange ->
        {
            lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastMethod.set(exchange.getRequestMethod());
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"status\":\"ok\"}");
        });

        client.putApiKeys(List.of("sk-1", "sk-2"));

        assertEquals("PUT", lastMethod.get());
        // CLIProxyAPI 契约：请求体为原始 JSON 数组（非对象包装）
        assertEquals("[\"sk-1\",\"sk-2\"]", lastBody.get());
        assertEquals("Bearer " + MANAGEMENT_KEY, lastAuthorization.get());
    }

    @Test
    void shouldThrowWhenPutStatusNotOk()
    {
        register("/v0/management/api-keys", "{\"status\":\"error\"}");
        CpaManagementException exception = assertThrows(CpaManagementException.class,
                () -> client.putApiKeys(List.of("sk-1")));
        assertTrue(exception.getMessage().contains("status"));
    }

    @Test
    void shouldThrowOnHttpError()
    {
        register("/v0/management/api-keys", "{\"error\":\"invalid management key\"}", 401);
        CpaManagementException exception = assertThrows(CpaManagementException.class, () -> client.listApiKeys());
        assertTrue(exception.getMessage().contains("HTTP 401"));
        assertTrue(exception.getMessage().contains("invalid management key"));
    }

    @Test
    void shouldThrowWhenApiKeysArrayMissing()
    {
        register("/v0/management/api-keys", "{\"unexpected\":true}");
        CpaManagementException exception = assertThrows(CpaManagementException.class, () -> client.listApiKeys());
        assertTrue(exception.getMessage().contains("api-keys"));
    }

    private void register(String path, String responseBody)
    {
        register(path, responseBody, 200);
    }

    private void register(String path, String responseBody, int status)
    {
        server.createContext(path, exchange ->
        {
            lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, status, responseBody);
        });
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
