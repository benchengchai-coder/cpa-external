package com.ruoyi.cpaexternal.log.subscription;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailureEvent;
import com.ruoyi.cpaexternal.log.service.ICpaUpstreamFailureService;
import tools.jackson.databind.ObjectMapper;

/**
 * 订阅 CLIProxyAPI 的 errors 通道，把上游失败事件保存到 ai_upstream_failure。
 *
 * <p>该组件只消费 CLIProxyAPI 已产生的失败事件，不参与请求转发。errors 通道
 * 只向在线订阅者实时推送（{@code redisqueue.EnqueueError} 无积压暂存），因此本订阅器
 * 不做补收；断线期间的事件会丢失，只能靠 ai_log 中的 failed 记录做事后对账。</p>
 */
@Component
@ConditionalOnProperty(prefix = "cpa.cli-proxy.errors-subscription", name = "enabled", havingValue = "true")
public class CpaErrorEventSubscriber implements SmartLifecycle
{
    private static final Logger log = LoggerFactory.getLogger(CpaErrorEventSubscriber.class);
    private static final int MAX_PERSIST_ATTEMPTS = 3;
    private static final long PERSIST_RETRY_DELAY_MILLIS = 1000L;

    private final CpaUsageSubscriptionProperties connectionProperties;
    private final CpaErrorsSubscriptionProperties properties;
    private final ICpaUpstreamFailureService upstreamFailureService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean();

    private volatile Thread worker;
    private volatile CpaRedisRespClient activeClient;

    public CpaErrorEventSubscriber(CpaUsageSubscriptionProperties connectionProperties,
            CpaErrorsSubscriptionProperties properties, ICpaUpstreamFailureService upstreamFailureService,
            ObjectMapper objectMapper)
    {
        this.connectionProperties = connectionProperties;
        this.properties = properties;
        this.upstreamFailureService = upstreamFailureService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start()
    {
        String connectionError = connectionProperties.validate();
        if (connectionError != null)
        {
            log.error("CLIProxyAPI 失败事件订阅未启动：usage-subscription 连接配置无效：{}", connectionError);
            return;
        }
        String validationError = properties.validate();
        if (validationError != null)
        {
            log.error("CLIProxyAPI 失败事件订阅未启动：{}", validationError);
            return;
        }
        if (!running.compareAndSet(false, true))
        {
            return;
        }
        worker = Thread.ofPlatform()
                .name("cliproxy-error-subscriber")
                .daemon(true)
                .start(this::runSubscriptionLoop);
    }

    @Override
    public void stop()
    {
        if (!running.compareAndSet(true, false))
        {
            return;
        }
        closeActiveClient();
        Thread currentWorker = worker;
        if (currentWorker != null)
        {
            currentWorker.interrupt();
        }
    }

    @Override
    public void stop(Runnable callback)
    {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning()
    {
        return running.get();
    }

    @Override
    public boolean isAutoStartup()
    {
        return true;
    }

    @Override
    public int getPhase()
    {
        return Integer.MAX_VALUE;
    }

    private void runSubscriptionLoop()
    {
        long reconnectDelay = connectionProperties.reconnectDelayMillis();
        while (running.get())
        {
            boolean subscribed = false;
            try (CpaRedisRespClient client = CpaRedisRespClient.connect(connectionProperties))
            {
                activeClient = client;
                client.authenticate(connectionProperties.getManagementKey());
                client.subscribeErrors();
                subscribed = true;
                reconnectDelay = connectionProperties.reconnectDelayMillis();
                log.info("已订阅 CLIProxyAPI 失败事件通道 {}:{}（TLS={}）",
                        connectionProperties.getHost(), connectionProperties.getPort(),
                        connectionProperties.isTls());
                consumeMessages(client);
            }
            catch (CpaRedisRespClient.CpaRedisAuthenticationException exception)
            {
                if (running.get())
                {
                    log.error("CLIProxyAPI Management Key 认证失败，失败事件订阅已停止；请修正配置后重启服务：{}",
                            exception.getMessage());
                }
                running.set(false);
                break;
            }
            catch (Exception exception)
            {
                if (!running.get())
                {
                    break;
                }
                log.warn("CLIProxyAPI 失败事件订阅连接中断，{} 毫秒后重连：{}",
                        reconnectDelay, exception.getMessage());
            }
            finally
            {
                activeClient = null;
            }

            if (!running.get())
            {
                break;
            }
            if (!waitForReconnect(reconnectDelay))
            {
                break;
            }
            if (!subscribed)
            {
                reconnectDelay = nextReconnectDelay(reconnectDelay);
            }
        }
    }

    private void consumeMessages(CpaRedisRespClient client) throws IOException
    {
        while (running.get())
        {
            Object frame;
            try
            {
                frame = client.readFrame();
            }
            catch (SocketTimeoutException exception)
            {
                client.ping();
                continue;
            }
            if (!(frame instanceof List<?> values) || values.size() < 2)
            {
                throw new IOException("CLIProxyAPI 返回了无效的 Pub/Sub 消息");
            }
            String kind = stringValue(values.get(0));
            if ("pong".equalsIgnoreCase(kind))
            {
                continue;
            }
            if (!"message".equalsIgnoreCase(kind) || values.size() != 3
                    || !"errors".equalsIgnoreCase(stringValue(values.get(1))))
            {
                throw new IOException("CLIProxyAPI 返回了无法识别的 Pub/Sub 消息");
            }
            Object rawPayload = values.get(2);
            if (!(rawPayload instanceof String payload))
            {
                throw new IOException("CLIProxyAPI errors 消息不是 JSON 字符串");
            }
            processPayload(payload);
        }
    }

    void processPayload(String payload)
    {
        String normalized = payload == null ? "" : payload.trim();
        if (normalized.isEmpty())
        {
            log.warn("忽略 CLIProxyAPI 的空 errors 消息");
            return;
        }

        CpaUpstreamFailureEvent event;
        try
        {
            event = objectMapper.readValue(normalized, CpaUpstreamFailureEvent.class);
        }
        catch (Exception exception)
        {
            log.warn("忽略无法解析的 CLIProxyAPI errors 消息：{}", exception.getMessage());
            return;
        }
        if (event == null || event.getAuthIndex() == null || event.getAuthIndex().isBlank())
        {
            log.warn("忽略缺少 auth_index 的 CLIProxyAPI errors 消息");
            return;
        }
        persistWithRetry(event);
    }

    /**
     * 落库失败时短暂重试，最终失败只记录日志并放弃该事件。
     *
     * <p>与 usage 订阅不同：errors 通道没有积压补收，断连重连也找不回丢失的事件，
     * 因此落库失败不应放大成断连重连，保持订阅在线能接收更多后续事件。</p>
     */
    private void persistWithRetry(CpaUpstreamFailureEvent event)
    {
        for (int attempt = 1; attempt <= MAX_PERSIST_ATTEMPTS; attempt++)
        {
            try
            {
                upstreamFailureService.record(event);
                return;
            }
            catch (ServiceException exception)
            {
                log.warn("忽略不符合入库要求的 CLIProxyAPI 失败事件：{}", exception.getMessage());
                return;
            }
            catch (RuntimeException exception)
            {
                if (attempt == MAX_PERSIST_ATTEMPTS)
                {
                    log.error("CLIProxyAPI 失败事件入库失败，已放弃该事件：authIndex={}, statusCode={}",
                            event.getAuthIndex(), event.getStatusCode(), exception);
                    return;
                }
                if (!waitMillis(PERSIST_RETRY_DELAY_MILLIS))
                {
                    return;
                }
            }
        }
    }

    private boolean waitForReconnect(long delayMillis)
    {
        long remaining = delayMillis;
        while (running.get() && remaining > 0)
        {
            long currentDelay = Math.min(remaining, 60_000L);
            if (!waitMillis(currentDelay))
            {
                return false;
            }
            remaining -= currentDelay;
        }
        return running.get();
    }

    private boolean waitMillis(long delayMillis)
    {
        try
        {
            Thread.sleep(delayMillis);
            return true;
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private long nextReconnectDelay(long currentDelay)
    {
        long maximum = connectionProperties.maxReconnectDelayMillis();
        if (currentDelay >= maximum / 2)
        {
            return maximum;
        }
        return Math.min(currentDelay * 2, maximum);
    }

    private void closeActiveClient()
    {
        CpaRedisRespClient client = activeClient;
        if (client == null)
        {
            return;
        }
        try
        {
            client.close();
        }
        catch (IOException exception)
        {
            log.debug("关闭 CLIProxyAPI 失败事件订阅连接失败", exception);
        }
    }

    private String stringValue(Object value)
    {
        return value instanceof String text ? text : "";
    }
}
