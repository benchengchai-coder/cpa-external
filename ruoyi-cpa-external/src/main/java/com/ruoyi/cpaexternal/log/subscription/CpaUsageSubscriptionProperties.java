package com.ruoyi.cpaexternal.log.subscription;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** CLIProxyAPI Redis 用量订阅配置。 */
@Component
@ConfigurationProperties(prefix = "cpa.cli-proxy.usage-subscription")
public class CpaUsageSubscriptionProperties
{
    private boolean enabled;
    private String host = "127.0.0.1";
    private int port = 8317;
    private String managementKey = "";
    private boolean tls;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
    private Duration reconnectDelay = Duration.ofSeconds(5);
    private Duration maxReconnectDelay = Duration.ofMinutes(1);
    private int backlogBatchSize = 50;
    private int maxPayloadBytes = 1024 * 1024;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
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

    public Duration getReconnectDelay()
    {
        return reconnectDelay;
    }

    public void setReconnectDelay(Duration reconnectDelay)
    {
        this.reconnectDelay = reconnectDelay;
    }

    public Duration getMaxReconnectDelay()
    {
        return maxReconnectDelay;
    }

    public void setMaxReconnectDelay(Duration maxReconnectDelay)
    {
        this.maxReconnectDelay = maxReconnectDelay;
    }

    public int getMaxPayloadBytes()
    {
        return maxPayloadBytes;
    }

    public void setMaxPayloadBytes(int maxPayloadBytes)
    {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public int getBacklogBatchSize()
    {
        return backlogBatchSize;
    }

    public void setBacklogBatchSize(int backlogBatchSize)
    {
        this.backlogBatchSize = backlogBatchSize;
    }

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
        if (!isPositive(reconnectDelay))
        {
            return "reconnect-delay 必须大于 0";
        }
        if (!isPositive(maxReconnectDelay) || maxReconnectDelay.compareTo(reconnectDelay) < 0)
        {
            return "max-reconnect-delay 不能小于 reconnect-delay";
        }
        if (backlogBatchSize <= 0 || backlogBatchSize > 1000)
        {
            return "backlog-batch-size 必须在 1 到 1000 之间";
        }
        if (maxPayloadBytes <= 0)
        {
            return "max-payload-bytes 必须大于 0";
        }
        return null;
    }

    int connectTimeoutMillis()
    {
        return durationMillis(connectTimeout);
    }

    int readTimeoutMillis()
    {
        return durationMillis(readTimeout);
    }

    long reconnectDelayMillis()
    {
        return Math.max(1L, reconnectDelay.toMillis());
    }

    long maxReconnectDelayMillis()
    {
        return Math.max(1L, maxReconnectDelay.toMillis());
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
