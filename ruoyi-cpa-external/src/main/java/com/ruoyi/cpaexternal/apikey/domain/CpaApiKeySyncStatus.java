package com.ruoyi.cpaexternal.apikey.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 平台 API Key 与 CLIProxyAPI api-keys 的实时同步状态。
 *
 * <p>对比口径与镜像对账任务一致：仅启用状态（status=0）的 Key 应存在于 CPA；
 * CPA 上平台未登记的 Key 可能是手工配置，只以脱敏明文展示，不提供自动删除。</p>
 */
public class CpaApiKeySyncStatus
{
    /** API Key 推送同步开关是否开启。 */
    private boolean pushEnabled;

    /** CPA 侧 api-keys 总数。 */
    private int cpaKeyCount;

    /** 平台 ai_apikey 总数。 */
    private int platformKeyCount;

    /** 两边一致的启用 Key 数。 */
    private int syncedCount;

    /** 平台启用但 CPA 缺失（该 Key 无法调用 CPA，可点击同步生效）。 */
    private List<SyncDiffItem> missingInCpa = new ArrayList<>();

    /** 平台已停用但 CPA 仍残留（建议从 CPA 移除）。 */
    private List<SyncDiffItem> disabledResidual = new ArrayList<>();

    /** CPA 有但平台完全未登记的脱敏明文（可能为手工配置，系统不自动删除）。 */
    private List<String> orphanInCpa = new ArrayList<>();

    /** 差异条目：本地 Key 的定位信息，不包含明文。 */
    public static class SyncDiffItem
    {
        private Long keyId;
        private String keyName;
        private Long userId;
        private String status;

        public Long getKeyId() { return keyId; }
        public void setKeyId(Long keyId) { this.keyId = keyId; }
        public String getKeyName() { return keyName; }
        public void setKeyName(String keyName) { this.keyName = keyName; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    public int getCpaKeyCount() { return cpaKeyCount; }
    public void setCpaKeyCount(int cpaKeyCount) { this.cpaKeyCount = cpaKeyCount; }
    public int getPlatformKeyCount() { return platformKeyCount; }
    public void setPlatformKeyCount(int platformKeyCount) { this.platformKeyCount = platformKeyCount; }
    public int getSyncedCount() { return syncedCount; }
    public void setSyncedCount(int syncedCount) { this.syncedCount = syncedCount; }
    public List<SyncDiffItem> getMissingInCpa() { return missingInCpa; }
    public void setMissingInCpa(List<SyncDiffItem> missingInCpa) { this.missingInCpa = missingInCpa; }
    public List<SyncDiffItem> getDisabledResidual() { return disabledResidual; }
    public void setDisabledResidual(List<SyncDiffItem> disabledResidual) { this.disabledResidual = disabledResidual; }
    public List<String> getOrphanInCpa() { return orphanInCpa; }
    public void setOrphanInCpa(List<String> orphanInCpa) { this.orphanInCpa = orphanInCpa; }
}
