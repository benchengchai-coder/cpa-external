package com.ruoyi.cpaexternal.log.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CLIProxyAPI errors 通道订阅与上游失败事件清理配置。
 *
 * <p>连接参数（host/port/management-key/tls/超时/载荷上限）复用
 * {@code cpa.cli-proxy.usage-subscription} 配置段，两者连的是同一台 CLIProxyAPI。
 * errors 通道只做实时推送、没有积压暂存，订阅断线期间的事件无法补收。</p>
 */
@Component
@ConfigurationProperties(prefix = "cpa.cli-proxy.errors-subscription")
public class CpaErrorsSubscriptionProperties
{
    private boolean enabled;
    /** 失败事件保留天数，超过后由清理任务分批删除。 */
    private int retentionDays = 3;
    private String cleanupCron = "0 30 1 * * *";
    private String cleanupZone = "Asia/Shanghai";
    private int cleanupBatchSize = 2000;
    private int cleanupMaxBatches = 50;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public int getRetentionDays()
    {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays)
    {
        this.retentionDays = retentionDays;
    }

    public String getCleanupCron()
    {
        return cleanupCron;
    }

    public void setCleanupCron(String cleanupCron)
    {
        this.cleanupCron = cleanupCron;
    }

    public String getCleanupZone()
    {
        return cleanupZone;
    }

    public void setCleanupZone(String cleanupZone)
    {
        this.cleanupZone = cleanupZone;
    }

    public int getCleanupBatchSize()
    {
        return cleanupBatchSize;
    }

    public void setCleanupBatchSize(int cleanupBatchSize)
    {
        this.cleanupBatchSize = cleanupBatchSize;
    }

    public int getCleanupMaxBatches()
    {
        return cleanupMaxBatches;
    }

    public void setCleanupMaxBatches(int cleanupMaxBatches)
    {
        this.cleanupMaxBatches = cleanupMaxBatches;
    }

    /** 返回配置错误；返回 {@code null} 表示配置有效。连接参数由 usage-subscription 配置负责校验。 */
    public String validate()
    {
        if (retentionDays <= 0)
        {
            return "retention-days 必须大于 0";
        }
        if (cleanupCron == null || cleanupCron.isBlank())
        {
            return "cleanup-cron 不能为空";
        }
        if (cleanupZone == null || cleanupZone.isBlank())
        {
            return "cleanup-zone 不能为空";
        }
        if (cleanupBatchSize <= 0)
        {
            return "cleanup-batch-size 必须大于 0";
        }
        if (cleanupMaxBatches <= 0)
        {
            return "cleanup-max-batches 必须大于 0";
        }
        return null;
    }
}
