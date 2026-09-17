package com.ruoyi.cpaexternal.apikey.event;

/**
 * API Key 变更同步事件。
 *
 * <p>由 API Key 管理服务在本地事务内发布，推送监听器在事务提交后消费，
 * 把变更写入 CLIProxyAPI 的入口 Key 列表使其生效。</p>
 */
public class CpaApiKeySyncEvent
{
    /** 同步操作类型。 */
    public enum Operation
    {
        /** 新增 Key（或重新启用后重新推送）。 */
        CREATE,
        /** 明文替换（轮换、管理员自定义密钥）。 */
        REPLACE,
        /** 移除 Key（删除、停用）。 */
        REMOVE
    }

    private final Long keyId;
    private final Operation operation;
    /** REPLACE/REMOVE 涉及的旧明文 Key。 */
    private final String oldPlainKey;
    /** CREATE/REPLACE 的新明文 Key。 */
    private final String newPlainKey;

    private CpaApiKeySyncEvent(Long keyId, Operation operation, String oldPlainKey, String newPlainKey)
    {
        this.keyId = keyId;
        this.operation = operation;
        this.oldPlainKey = oldPlainKey;
        this.newPlainKey = newPlainKey;
    }

    public static CpaApiKeySyncEvent create(Long keyId, String newPlainKey)
    {
        return new CpaApiKeySyncEvent(keyId, Operation.CREATE, null, newPlainKey);
    }

    public static CpaApiKeySyncEvent replace(Long keyId, String oldPlainKey, String newPlainKey)
    {
        return new CpaApiKeySyncEvent(keyId, Operation.REPLACE, oldPlainKey, newPlainKey);
    }

    public static CpaApiKeySyncEvent remove(Long keyId, String plainKey)
    {
        return new CpaApiKeySyncEvent(keyId, Operation.REMOVE, plainKey, null);
    }

    public Long getKeyId()
    {
        return keyId;
    }

    public Operation getOperation()
    {
        return operation;
    }

    public String getOldPlainKey()
    {
        return oldPlainKey;
    }

    public String getNewPlainKey()
    {
        return newPlainKey;
    }
}
