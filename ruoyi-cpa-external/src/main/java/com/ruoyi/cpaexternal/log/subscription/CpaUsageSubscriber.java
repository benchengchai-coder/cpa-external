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
import com.ruoyi.cpaexternal.log.domain.CpaAiLogPayload;
import com.ruoyi.cpaexternal.log.service.ICpaAiLogService;
import tools.jackson.databind.ObjectMapper;

/**
 * 订阅 CLIProxyAPI 的 usage 通道并将单次请求用量保存到 ai_log。
 *
 * <p>该组件只消费 CLIProxyAPI 已完成请求产生的统计消息，不参与请求转发。</p>
 */
@Component
@ConditionalOnProperty(prefix = "cpa.cli-proxy.usage-subscription", name = "enabled", havingValue = "true")
public class CpaUsageSubscriber implements SmartLifecycle
{
    private static final Logger log = LoggerFactory.getLogger(CpaUsageSubscriber.class);
    private static final int MAX_PERSIST_ATTEMPTS = 3;
    private static final long PERSIST_RETRY_DELAY_MILLIS = 1000L;

    private final CpaUsageSubscriptionProperties properties;
    private final ICpaAiLogService aiLogService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean drainingBacklog = new AtomicBoolean();

    private volatile Thread worker;
    private volatile Thread backlogWorker;
    private volatile CpaRedisRespClient activeClient;
    private volatile CpaRedisRespClient activeBacklogClient;

    public CpaUsageSubscriber(CpaUsageSubscriptionProperties properties, ICpaAiLogService aiLogService,
            ObjectMapper objectMapper)
    {
        this.properties = properties;
        this.aiLogService = aiLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start()
    {
        String validationError = properties.validate();
        if (validationError != null)
        {
            log.error("CLIProxyAPI 用量订阅未启动：{}", validationError);
            return;
        }
        if (!running.compareAndSet(false, true))
        {
            return;
        }
        worker = Thread.ofPlatform()
                .name("cliproxy-usage-subscriber")
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
        closeActiveBacklogClient();
        Thread currentWorker = worker;
        if (currentWorker != null)
        {
            currentWorker.interrupt();
        }
        Thread currentBacklogWorker = backlogWorker;
        if (currentBacklogWorker != null)
        {
            currentBacklogWorker.interrupt();
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
        long reconnectDelay = properties.reconnectDelayMillis();
        while (running.get())
        {
            boolean subscribed = false;
            try (CpaRedisRespClient client = CpaRedisRespClient.connect(properties))
            {
                activeClient = client;
                client.authenticate(properties.getManagementKey());
                client.subscribeUsage();
                subscribed = true;
                reconnectDelay = properties.reconnectDelayMillis();
                log.info("已订阅 CLIProxyAPI 用量通道 {}:{}（TLS={}）",
                        properties.getHost(), properties.getPort(), properties.isTls());
                startBacklogDrain();
                consumeMessages(client);
            }
            catch (CpaRedisRespClient.CpaRedisAuthenticationException exception)
            {
                if (running.get())
                {
                    log.error("CLIProxyAPI Management Key 认证失败，用量订阅已停止；请修正配置后重启服务：{}",
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
                log.warn("CLIProxyAPI 用量订阅连接中断，{} 毫秒后重连：{}",
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

    private void startBacklogDrain()
    {
        if (!running.get() || !drainingBacklog.compareAndSet(false, true))
        {
            return;
        }
        backlogWorker = Thread.ofVirtual()
                .name("cliproxy-usage-backlog-drainer")
                .start(this::drainBacklog);
    }

    private void drainBacklog()
    {
        int drained = 0;
        try (CpaRedisRespClient client = CpaRedisRespClient.connect(properties))
        {
            activeBacklogClient = client;
            client.authenticate(properties.getManagementKey());
            while (running.get())
            {
                List<String> payloads = client.popUsage(properties.getBacklogBatchSize());
                if (payloads.isEmpty())
                {
                    break;
                }
                for (String payload : payloads)
                {
                    if (!running.get())
                    {
                        break;
                    }
                    try
                    {
                        processPayload(payload);
                        drained++;
                    }
                    catch (RuntimeException exception)
                    {
                        log.error("CLIProxyAPI 暂存用量记录入库失败，继续处理同批其他记录：{}",
                                exception.getMessage());
                    }
                }
            }
            if (drained > 0)
            {
                log.info("已补收 CLIProxyAPI 订阅建立前的 {} 条用量记录", drained);
            }
        }
        catch (Exception exception)
        {
            if (running.get())
            {
                log.warn("补收 CLIProxyAPI 暂存用量队列失败，实时订阅不受影响：{}", exception.getMessage());
            }
        }
        finally
        {
            activeBacklogClient = null;
            drainingBacklog.set(false);
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
                    || !"usage".equalsIgnoreCase(stringValue(values.get(1))))
            {
                throw new IOException("CLIProxyAPI 返回了无法识别的 Pub/Sub 消息");
            }
            Object rawPayload = values.get(2);
            if (!(rawPayload instanceof String payload))
            {
                throw new IOException("CLIProxyAPI usage 消息不是 JSON 字符串");
            }
            processPayload(payload);
        }
    }

    void processPayload(String payload)
    {
        String normalized = payload == null ? "" : payload.trim();
        if (normalized.isEmpty())
        {
            log.warn("忽略 CLIProxyAPI 的空 usage 消息");
            return;
        }
        if ("{\"support_refresh\":true}".equals(normalized) || "{\"refresh\":true}".equals(normalized))
        {
            return;
        }

        CpaAiLogPayload usage;
        try
        {
            usage = objectMapper.readValue(normalized, CpaAiLogPayload.class);
        }
        catch (Exception exception)
        {
            log.warn("忽略无法解析的 CLIProxyAPI usage 消息：{}", exception.getMessage());
            return;
        }
        persistWithRetry(usage);
    }

    private void persistWithRetry(CpaAiLogPayload usage)
    {
        for (int attempt = 1; attempt <= MAX_PERSIST_ATTEMPTS; attempt++)
        {
            try
            {
                aiLogService.ingest(usage);
                return;
            }
            catch (ServiceException exception)
            {
                log.warn("忽略不符合 ai_log 入库要求的 CLIProxyAPI usage 消息：{}", exception.getMessage());
                return;
            }
            catch (RuntimeException exception)
            {
                if (attempt == MAX_PERSIST_ATTEMPTS)
                {
                    throw exception;
                }
                if (!waitMillis(PERSIST_RETRY_DELAY_MILLIS))
                {
                    throw exception;
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
        long maximum = properties.maxReconnectDelayMillis();
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
            log.debug("关闭 CLIProxyAPI 用量订阅连接失败", exception);
        }
    }

    private void closeActiveBacklogClient()
    {
        CpaRedisRespClient client = activeBacklogClient;
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
            log.debug("关闭 CLIProxyAPI 暂存用量队列连接失败", exception);
        }
    }

    private String stringValue(Object value)
    {
        return value instanceof String text ? text : "";
    }
}
