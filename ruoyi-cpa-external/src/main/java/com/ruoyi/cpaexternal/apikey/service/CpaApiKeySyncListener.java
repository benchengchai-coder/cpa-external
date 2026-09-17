package com.ruoyi.cpaexternal.apikey.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cpaexternal.apikey.event.CpaApiKeySyncEvent;

/**
 * API Key 变更推送监听器。
 *
 * <p>在本地事务提交后、原请求线程内同步执行推送，接口返回时 Key 已在
 * CLIProxyAPI 生效；推送失败不回滚本地记录（数据已落库），抛出带补偿说明的
 * 异常提示调用方，差异由镜像对账定时任务自动补推，最终一致。</p>
 */
@Component
public class CpaApiKeySyncListener
{
    private static final Logger log = LoggerFactory.getLogger(CpaApiKeySyncListener.class);

    private final CpaApiKeyPushService pushService;

    private final ICpaApiKeyService apiKeyService;

    public CpaApiKeySyncListener(CpaApiKeyPushService pushService, ICpaApiKeyService apiKeyService)
    {
        this.pushService = pushService;
        this.apiKeyService = apiKeyService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApiKeyChanged(CpaApiKeySyncEvent event)
    {
        if (!pushService.isEnabled())
        {
            return;
        }
        try
        {
            switch (event.getOperation())
            {
                case CREATE -> pushService.upsertKey(event.getNewPlainKey());
                case REPLACE -> pushService.replaceKey(event.getOldPlainKey(), event.getNewPlainKey());
                case REMOVE -> pushService.removeKey(event.getOldPlainKey());
            }
            // 推送成功后失效列表展示缓存，让 CPA 同步状态列立即反映新状态。
            apiKeyService.evictCpaKeysCache();
            log.info("API Key 变更已同步到CLIProxyAPI: operation={}, keyId={}", event.getOperation(), event.getKeyId());
        }
        catch (Exception exception)
        {
            log.error("API Key 同步到 CLIProxyAPI 失败，等待对账任务自动补推: operation={}, keyId={}",
                    event.getOperation(), event.getKeyId(), exception);
            throw new ServiceException("API Key已保存，但同步到CLIProxyAPI失败：" + exception.getMessage()
                    + "；系统将在下个对账周期自动重试同步");
        }
    }
}
