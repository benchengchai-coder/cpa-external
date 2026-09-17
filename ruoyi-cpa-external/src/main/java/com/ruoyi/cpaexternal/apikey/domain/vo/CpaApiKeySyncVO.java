package com.ruoyi.cpaexternal.apikey.domain.vo;

import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;

/**
 * API Key 列表行 VO：在表字段之上附加 CPA 同步状态标记（非表列，不入库）。
 *
 * <p>状态来自与 CLIProxyAPI api-keys 的实时对比（列表场景走短 TTL 缓存）。</p>
 */
public class CpaApiKeySyncVO extends CpaApiKey
{
    private static final long serialVersionUID = 1L;

    /** 已同步：启用 Key 存在于 CPA，或停用 Key 不在 CPA（状态正确）。 */
    public static final String STATUS_SYNCED = "synced";

    /** CPA缺失：启用 Key 不在 CPA（该 Key 无法调用 CLIProxyAPI）。 */
    public static final String STATUS_MISSING = "missing";

    /** 停用残留：平台已停用但 CPA 仍存在。 */
    public static final String STATUS_RESIDUAL = "residual";

    /** 未知：CLIProxyAPI 不可用，无法对比。 */
    public static final String STATUS_UNKNOWN = "unknown";

    private String cpaSyncStatus;

    public CpaApiKeySyncVO()
    {
    }

    public CpaApiKeySyncVO(CpaApiKey source)
    {
        setKeyId(source.getKeyId());
        setUserId(source.getUserId());
        setKeyName(source.getKeyName());
        setApiKey(source.getApiKey());
        setStatus(source.getStatus());
        setAccessedTime(source.getAccessedTime());
        setRemark(source.getRemark());
        setCreateTime(source.getCreateTime());
    }

    public String getCpaSyncStatus()
    {
        return cpaSyncStatus;
    }

    public void setCpaSyncStatus(String cpaSyncStatus)
    {
        this.cpaSyncStatus = cpaSyncStatus;
    }
}
