package com.ruoyi.cpaexternal.apikey.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.cpaexternal.apikey.config.CpaManagementProperties;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * CLIProxyAPI 管理接口（/v0/management）客户端。
 *
 * <p>仅保留 API Key 推送同步所需的入口 Key 列表读写能力：
 * 通过 PUT /v0/management/api-keys 整体改写 CLIProxyAPI 自身的入口 Key 列表
 * （其持久化到配置并热加载生效）。本客户端不承载任何 AI 请求转发。</p>
 */
@Component
public class CpaManagementClient
{
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>()
    {
    };

    private final CpaManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CpaManagementClient(CpaManagementProperties properties, ObjectMapper objectMapper)
    {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMillis()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** 拉取入口 API Key 列表（GET /v0/management/api-keys）。 */
    public List<String> listApiKeys()
    {
        JsonNode keys = readJsonTree("/v0/management/api-keys").path("api-keys");
        if (!keys.isArray())
        {
            throw new CpaManagementException("CLIProxyAPI 管理接口响应缺少 api-keys 数组: GET /v0/management/api-keys");
        }
        return objectMapper.convertValue(keys, STRING_LIST_TYPE);
    }

    /**
     * 整体改写入口 API Key 列表（PUT /v0/management/api-keys）。
     *
     * <p>CLIProxyAPI 会把列表持久化到配置文件并热加载生效；调用方必须先拉取现有列表
     * 合并后再回写，避免误删平台未登记的 Key。</p>
     */
    public void putApiKeys(List<String> keys)
    {
        String body = objectMapper.writeValueAsString(keys);
        String response = request("PUT", "/v0/management/api-keys", body);
        requireOkStatus("PUT", "/v0/management/api-keys", response);
    }

    private void requireOkStatus(String method, String path, String response)
    {
        JsonNode status = objectMapper.readTree(response).path("status");
        if (!"ok".equals(status.asText()))
        {
            throw new CpaManagementException("CLIProxyAPI 管理接口响应 status 异常: " + method + " " + path + ": "
                    + abbreviate(response));
        }
    }

    private JsonNode readJsonTree(String path)
    {
        return objectMapper.readTree(get(path));
    }

    private String get(String path)
    {
        return request("GET", path, null);
    }

    private String request(String method, String path, String body)
    {
        String url = properties.baseUrl() + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(properties.readTimeoutMillis()))
                .header("Authorization", "Bearer " + properties.getManagementKey())
                .header("Accept", "application/json, application/yaml, */*");
        if (body == null)
        {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        else
        {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response;
        try
        {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new CpaManagementException("CLIProxyAPI 管理接口请求被中断: " + method + " " + path, exception);
        }
        catch (IOException exception)
        {
            throw new CpaManagementException("CLIProxyAPI 管理接口请求失败: " + method + " " + path + ": "
                    + exception.getMessage(), exception);
        }
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300)
        {
            throw new CpaManagementException("CLIProxyAPI 管理接口返回异常: " + method + " " + path + " HTTP "
                    + statusCode + ": " + abbreviate(response.body()));
        }
        return response.body();
    }

    private static String abbreviate(String value)
    {
        if (value == null)
        {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300) + "...";
    }
}
