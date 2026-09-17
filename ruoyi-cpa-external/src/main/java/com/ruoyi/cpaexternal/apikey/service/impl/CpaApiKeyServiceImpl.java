package com.ruoyi.cpaexternal.apikey.service.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.cpaexternal.apikey.client.CpaManagementClient;
import com.ruoyi.cpaexternal.apikey.client.CpaManagementException;
import com.ruoyi.cpaexternal.apikey.config.CpaManagementProperties;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKeySyncStatus;
import com.ruoyi.cpaexternal.apikey.domain.vo.CpaApiKeySyncVO;
import com.ruoyi.cpaexternal.apikey.event.CpaApiKeySyncEvent;
import com.ruoyi.cpaexternal.apikey.mapper.CpaApiKeyMapper;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/** API Key 管理服务实现。 */
@Service
public class CpaApiKeyServiceImpl implements ICpaApiKeyService
{
    private static final Logger log = LoggerFactory.getLogger(CpaApiKeyServiceImpl.class);

    private static final String KEY_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String KEY_PREFIX = "sk-";
    private static final int RANDOM_LENGTH = 48;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 自定义密钥格式：sk- 前缀 + 5~125 位字母/数字/下划线/短横线。 */
    private static final java.util.regex.Pattern CUSTOM_KEY_PATTERN =
            java.util.regex.Pattern.compile("^sk-[A-Za-z0-9_-]{5,125}$");

    /** 列表展示用 CPA api-keys 缓存有效期：避免列表翻页/搜索每次都调 CLIProxyAPI。 */
    private static final long CPA_KEYS_CACHE_TTL_MILLIS = 60_000L;

    private final Object cpaKeysCacheLock = new Object();
    private volatile Set<String> cpaKeysCache;
    private volatile long cpaKeysCacheAt;

    @Autowired
    private CpaApiKeyMapper apiKeyMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private CpaManagementClient managementClient;

    @Autowired
    private CpaManagementProperties managementProperties;

    @Override
    public CpaApiKey selectById(Long keyId)
    {
        return apiKeyMapper.selectById(keyId);
    }

    @Override
    public CpaApiKey selectByPlainKey(String plainApiKey)
    {
        return apiKeyMapper.selectByPlainKey(plainApiKey);
    }

    @Override
    public CpaApiKey selectByUserId(Long userId)
    {
        return apiKeyMapper.selectByUserId(userId);
    }

    @Override
    public List<CpaApiKey> selectList(CpaApiKey query)
    {
        return apiKeyMapper.selectList(query);
    }

    @Override
    public CpaApiKeySyncStatus selectCpaSyncStatus()
    {
        Set<String> cpaKeys;
        try
        {
            cpaKeys = new HashSet<>(managementClient.listApiKeys());
        }
        catch (CpaManagementException exception)
        {
            throw new ServiceException("无法连接CLIProxyAPI获取api-keys：" + exception.getMessage());
        }
        List<CpaApiKey> platformKeys = apiKeyMapper.selectList(new CpaApiKey());

        CpaApiKeySyncStatus status = new CpaApiKeySyncStatus();
        status.setPushEnabled(managementProperties.isApiKeyPushEnabled());
        status.setCpaKeyCount(cpaKeys.size());
        status.setPlatformKeyCount(platformKeys.size());

        Set<String> platformPlainKeys = new HashSet<>();
        for (CpaApiKey apiKey : platformKeys)
        {
            String plainKey = apiKey.getApiKey();
            if (plainKey == null || plainKey.isEmpty())
            {
                continue;
            }
            platformPlainKeys.add(plainKey);
            boolean inCpa = cpaKeys.contains(plainKey);
            if ("0".equals(apiKey.getStatus()))
            {
                if (inCpa)
                {
                    status.setSyncedCount(status.getSyncedCount() + 1);
                }
                else
                {
                    status.getMissingInCpa().add(toDiffItem(apiKey));
                }
            }
            else if (inCpa)
            {
                status.getDisabledResidual().add(toDiffItem(apiKey));
            }
        }
        for (String cpaKey : cpaKeys)
        {
            if (!platformPlainKeys.contains(cpaKey))
            {
                status.getOrphanInCpa().add(maskPlainKey(cpaKey));
            }
        }
        return status;
    }

    @Override
    public List<CpaApiKeySyncVO> selectListWithCpaSync(CpaApiKey query)
    {
        List<CpaApiKey> keys = apiKeyMapper.selectList(query);
        Set<String> cpaKeys;
        try
        {
            cpaKeys = loadCpaKeysCached();
        }
        catch (CpaManagementException exception)
        {
            // 列表展示不因 CPA 故障报错，行级标记未知态。
            log.warn("获取CLIProxyAPI api-keys失败，列表CPA同步状态标记为未知: {}", exception.getMessage());
            cpaKeys = null;
        }
        List<CpaApiKeySyncVO> rows = new ArrayList<>(keys.size());
        for (CpaApiKey apiKey : keys)
        {
            CpaApiKeySyncVO row = new CpaApiKeySyncVO(apiKey);
            row.setCpaSyncStatus(resolveCpaSyncStatus(apiKey, cpaKeys));
            rows.add(row);
        }
        return rows;
    }

    @Override
    public void evictCpaKeysCache()
    {
        synchronized (cpaKeysCacheLock)
        {
            cpaKeysCache = null;
            cpaKeysCacheAt = 0L;
        }
    }

    /** 行级同步状态：启用 Key 看 CPA 是否存在；停用 Key 在 CPA 残留视为差异。 */
    private static String resolveCpaSyncStatus(CpaApiKey apiKey, Set<String> cpaKeys)
    {
        if (cpaKeys == null)
        {
            return CpaApiKeySyncVO.STATUS_UNKNOWN;
        }
        boolean inCpa = apiKey.getApiKey() != null && cpaKeys.contains(apiKey.getApiKey());
        boolean enabled = "0".equals(apiKey.getStatus());
        if (enabled)
        {
            return inCpa ? CpaApiKeySyncVO.STATUS_SYNCED : CpaApiKeySyncVO.STATUS_MISSING;
        }
        return inCpa ? CpaApiKeySyncVO.STATUS_RESIDUAL : CpaApiKeySyncVO.STATUS_SYNCED;
    }

    /** 带短 TTL 的 CPA api-keys 缓存，双检锁防并发击穿。 */
    private Set<String> loadCpaKeysCached()
    {
        if (cpaKeysCache != null && System.currentTimeMillis() - cpaKeysCacheAt < CPA_KEYS_CACHE_TTL_MILLIS)
        {
            return cpaKeysCache;
        }
        synchronized (cpaKeysCacheLock)
        {
            if (cpaKeysCache != null && System.currentTimeMillis() - cpaKeysCacheAt < CPA_KEYS_CACHE_TTL_MILLIS)
            {
                return cpaKeysCache;
            }
            Set<String> fresh = new HashSet<>(managementClient.listApiKeys());
            cpaKeysCache = fresh;
            cpaKeysCacheAt = System.currentTimeMillis();
            return fresh;
        }
    }

    private static CpaApiKeySyncStatus.SyncDiffItem toDiffItem(CpaApiKey apiKey)
    {
        CpaApiKeySyncStatus.SyncDiffItem item = new CpaApiKeySyncStatus.SyncDiffItem();
        item.setKeyId(apiKey.getKeyId());
        item.setKeyName(apiKey.getKeyName());
        item.setUserId(apiKey.getUserId());
        item.setStatus(apiKey.getStatus());
        return item;
    }

    /** 脱敏明文：保留前8后4，中间以****替代；过短时只保留前缀。 */
    private static String maskPlainKey(String plainKey)
    {
        if (plainKey.length() <= 10)
        {
            return plainKey.substring(0, Math.min(4, plainKey.length())) + "****";
        }
        return plainKey.substring(0, 8) + "****" + plainKey.substring(plainKey.length() - 4);
    }

    @Override
    @Transactional
    public CpaApiKey ensureDefault(Long userId)
    {
        CpaApiKey current = apiKeyMapper.selectByUserId(userId);
        if (current != null)
        {
            return current;
        }
        CpaApiKey apiKey = new CpaApiKey();
        apiKey.setUserId(userId);
        apiKey.setKeyName("默认密钥");
        apiKey.setStatus("0");
        return create(apiKey);
    }

    @Override
    @Transactional
    public CpaApiKey rotate(Long userId)
    {
        CpaApiKey current = apiKeyMapper.selectByUserId(userId);
        if (current == null)
        {
            throw new ServiceException("当前用户尚未分配API Key");
        }
        String oldPlainKey = current.getApiKey();
        String plainApiKey = generateUniqueApiKey();
        if (apiKeyMapper.updateSecret(current.getKeyId(), plainApiKey) != 1)
        {
            throw new ServiceException("API Key更换失败，请重试");
        }
        eventPublisher.publishEvent(CpaApiKeySyncEvent.replace(current.getKeyId(), oldPlainKey, plainApiKey));
        current.setApiKey(plainApiKey);
        return current;
    }

    @Override
    @Transactional
    public CpaApiKey create(CpaApiKey apiKey)
    {
        if (apiKey == null || apiKey.getUserId() == null)
        {
            throw new ServiceException("所属用户不能为空");
        }
        if (apiKeyMapper.countByUserId(apiKey.getUserId()) > 0)
        {
            throw new ServiceException("每个用户只能拥有一个API Key");
        }
        if (StringUtils.isEmpty(apiKey.getKeyName()))
        {
            apiKey.setKeyName("默认密钥");
        }
        if (StringUtils.isEmpty(apiKey.getStatus()))
        {
            apiKey.setStatus("0");
        }
        if (StringUtils.isEmpty(apiKey.getApiKey()))
        {
            apiKey.setApiKey(generateUniqueApiKey());
        }
        else
        {
            // 管理员创建时允许携带自定义密钥。
            apiKey.setApiKey(prepareCustomKey(apiKey.getApiKey(), null));
        }
        if (apiKeyMapper.insert(apiKey) != 1)
        {
            throw new ServiceException("API Key创建失败");
        }
        eventPublisher.publishEvent(CpaApiKeySyncEvent.create(apiKey.getKeyId(), apiKey.getApiKey()));
        return apiKey;
    }

    @Override
    @Transactional
    public int update(CpaApiKey apiKey)
    {
        if (apiKey == null || apiKey.getKeyId() == null)
        {
            throw new ServiceException("API Key不能为空");
        }
        CpaApiKey current = apiKeyMapper.selectById(apiKey.getKeyId());
        if (current == null)
        {
            throw new ServiceException("API Key不存在");
        }
        apiKey.setApiKey(null);
        int rows = apiKeyMapper.update(apiKey);
        if (rows == 1)
        {
            publishStatusChangeEvent(current, apiKey);
        }
        return rows;
    }

    /** 状态启停变化时发布同步事件：停用从 CLIProxyAPI 移除，重新启用则推送。 */
    private void publishStatusChangeEvent(CpaApiKey current, CpaApiKey incoming)
    {
        if (incoming.getStatus() == null || incoming.getStatus().equals(current.getStatus()))
        {
            return;
        }
        String plainKey = current.getApiKey();
        if (plainKey == null || plainKey.isEmpty())
        {
            return;
        }
        if ("0".equals(incoming.getStatus()))
        {
            eventPublisher.publishEvent(CpaApiKeySyncEvent.create(current.getKeyId(), plainKey));
        }
        else
        {
            eventPublisher.publishEvent(CpaApiKeySyncEvent.remove(current.getKeyId(), plainKey));
        }
    }

    @Override
    @Transactional
    public CpaApiKey customizeSecret(Long keyId, String plainApiKey)
    {
        CpaApiKey current = apiKeyMapper.selectById(keyId);
        if (current == null)
        {
            throw new ServiceException("API Key不存在");
        }
        String oldPlainKey = current.getApiKey();
        String customKey = prepareCustomKey(plainApiKey, keyId);
        if (apiKeyMapper.updateSecret(current.getKeyId(), customKey) != 1)
        {
            throw new ServiceException("API Key自定义失败，请重试");
        }
        eventPublisher.publishEvent(CpaApiKeySyncEvent.replace(current.getKeyId(), oldPlainKey, customKey));
        current.setApiKey(customKey);
        return current;
    }

    @Override
    @Transactional
    public int deleteByIds(Long[] keyIds)
    {
        List<CpaApiKey> targets = new ArrayList<>();
        for (Long keyId : keyIds)
        {
            CpaApiKey target = apiKeyMapper.selectById(keyId);
            if (target != null)
            {
                targets.add(target);
            }
        }
        int rows = apiKeyMapper.deleteByIds(keyIds);
        if (rows > 0)
        {
            // 入参可能包含不存在的 keyId，仅对实际已删除的记录发布同步事件。
            for (CpaApiKey target : targets)
            {
                if (apiKeyMapper.selectById(target.getKeyId()) == null)
                {
                    eventPublisher.publishEvent(CpaApiKeySyncEvent.remove(target.getKeyId(), target.getApiKey()));
                }
            }
        }
        return rows;
    }

    private String generateUniqueApiKey()
    {
        for (int attempt = 0; attempt < 10; attempt++)
        {
            StringBuilder builder = new StringBuilder(KEY_PREFIX);
            for (int i = 0; i < RANDOM_LENGTH; i++)
            {
                builder.append(KEY_CHARS.charAt(RANDOM.nextInt(KEY_CHARS.length())));
            }
            String plainApiKey = builder.toString();
            if (apiKeyMapper.selectByPlainKey(plainApiKey) == null)
            {
                return plainApiKey;
            }
        }
        throw new ServiceException("API Key生成失败，请重试");
    }

    /** 校验自定义密钥格式与唯一性，excludeKeyId 用于排除当前记录自身。 */
    private String prepareCustomKey(String plainApiKey, Long excludeKeyId)
    {
        String customKey = StringUtils.trim(plainApiKey);
        if (StringUtils.isEmpty(customKey) || !CUSTOM_KEY_PATTERN.matcher(customKey).matches())
        {
            throw new ServiceException("自定义密钥必须以 sk- 开头，总长8~128位，仅含字母、数字、下划线或短横线");
        }
        CpaApiKey existing = apiKeyMapper.selectByPlainKey(customKey);
        if (existing != null && !existing.getKeyId().equals(excludeKeyId))
        {
            throw new ServiceException("该密钥已被占用，请更换其他密钥");
        }
        return customKey;
    }
}
