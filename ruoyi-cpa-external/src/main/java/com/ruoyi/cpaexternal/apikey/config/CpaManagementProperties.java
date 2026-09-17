package com.ruoyi.cpaexternal.apikey.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** CLIProxyAPI 管理接口连接配置（API Key 推送同步使用）。 */
@Component
@ConfigurationProperties(prefix = "cpa.cli-proxy.management")
public class CpaManagementProperties
{
    /** API Key 推送同步开关：开启后平台侧 Key 变更会写入 CLIProxyAPI 的 api-keys 列表。 */
    private boolean apiKeyPushEnabled;
    private String host = "127.0.0.1";
    private int port = 8317;
    private String managementKey = "";
    private boolean tls;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);

    /** 返回配置错误；返回 {@code null} 表示配置有效。 */
    public String validate()
    {
        if (host == null || host.isBlank())
        {
            return "host 不能为空";
        }
        if (port <= 0 || port > 65535)
        {
            return "port 必须在 1 到 65535 之间";
        }
        if (managementKey == null || managementKey.isBlank())
        {
            return "management-key 不能为空";
        }
        if (!isPositive(connectTimeout))
        {
            return "connect-timeout 必须大于 0";
        }
        if (!isPositive(readTimeout))
        {
            return "read-timeout 必须大于 0";
        }
        return null;
    }

    public boolean isApiKeyPushEnabled()
    {
        return apiKeyPushEnabled;
    }

    public void setApiKeyPushEnabled(boolean apiKeyPushEnabled)
    {
        this.apiKeyPushEnabled = apiKeyPushEnabled;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getManagementKey()
    {
        return managementKey;
    }

    public void setManagementKey(String managementKey)
    {
        this.managementKey = managementKey;
    }

    public boolean isTls()
    {
        return tls;
    }

    public void setTls(boolean tls)
    {
        this.tls = tls;
    }

    public Duration getConnectTimeout()
    {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout()
    {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout)
    {
        this.readTimeout = readTimeout;
    }

    public String baseUrl()
    {
        return (tls ? "https://" : "http://") + host + ":" + port;
    }

    public int connectTimeoutMillis()
    {
        return durationMillis(connectTimeout);
    }

    public int readTimeoutMillis()
    {
        return durationMillis(readTimeout);
    }

    private boolean isPositive(Duration value)
    {
        return value != null && !value.isZero() && !value.isNegative();
    }

    private int durationMillis(Duration value)
    {
        long millis = value.toMillis();
        return (int) Math.max(1L, Math.min(millis, Integer.MAX_VALUE));
    }
}
