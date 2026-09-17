package com.ruoyi.cpaexternal.apikey.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.cpaexternal.apikey.client.CpaManagementClient;
import com.ruoyi.cpaexternal.apikey.config.CpaManagementProperties;

/**
 * API Key 推送同步服务：把平台侧 Key 变更写入 CLIProxyAPI 的入口 Key 列表。
 *
 * <p>CLIProxyAPI 管理 API 没有单条新增接口，统一采用「拉取现有列表 → 本地合并 →
 * PUT 整体改写」模式：不会误删 CPA 上平台未登记的 Key（如手工配置的 Key），
 * 写入后由 CLIProxyAPI 持久化到配置并热加载生效。进程内加锁串行化，避免并发
 * 读改写互相覆盖；所有操作幂等，实时推送与管理端手动补偿共用同一入口。</p>
 */
@Component
public class CpaApiKeyPushService
{
    private static final Logger log = LoggerFactory.getLogger(CpaApiKeyPushService.class);

    private final CpaManagementProperties properties;
    private final CpaManagementClient client;
    /** GET+PUT 读改写必须串行执行，防止并发推送互相覆盖。 */
    private final ReentrantLock pushLock = new ReentrantLock();

    public CpaApiKeyPushService(CpaManagementProperties properties, CpaManagementClient client)
    {
        this.properties = properties;
        this.client = client;
    }

    /** 推送开关是否开启；关闭时所有推送方法均为 no-op。 */
    public boolean isEnabled()
    {
        return properties.isApiKeyPushEnabled();
    }

    /** 新增（或幂等补推）一个 Key 到 CLIProxyAPI。 */
    public void upsertKey(String plainKey)
    {
        applyKeys(Set.of(), Set.of(plainKey));
    }

    /** 轮换/自定义密钥：把旧明文替换为新明文（旧 Key 立即从 CPA 失效）。 */
    public void replaceKey(String oldPlainKey, String newPlainKey)
    {
        applyKeys(Set.of(oldPlainKey), Set.of(newPlainKey));
    }

    /** 从 CLIProxyAPI 移除一个 Key（本地删除或停用时调用）。 */
    public void removeKey(String plainKey)
    {
        applyKeys(Set.of(plainKey), Set.of());
    }

    /**
     * 拉取 CPA 现有 Key 列表，应用增删后整体回写；结果与现状一致时跳过 PUT。
     *
     * @param removals 需要从 CPA 列表移除的明文 Key
     * @param additions 需要加入 CPA 列表的明文 Key
     */
    private void applyKeys(Set<String> removals, Set<String> additions)
    {
        pushLock.lock();
        try
        {
            Set<String> cpaKeys = new LinkedHashSet<>(client.listApiKeys());
            boolean changed = cpaKeys.removeAll(removals);
            changed |= cpaKeys.addAll(additions);
            if (!changed)
            {
                log.debug("CLIProxyAPI api-keys 无需变更，跳过回写: removals={}, additions={}",
                        removals.size(), additions.size());
                return;
            }
            client.putApiKeys(new ArrayList<>(cpaKeys));
            log.info("CLIProxyAPI api-keys 已回写生效: removals={}, additions={}", removals.size(), additions.size());
        }
        finally
        {
            pushLock.unlock();
        }
    }
}
