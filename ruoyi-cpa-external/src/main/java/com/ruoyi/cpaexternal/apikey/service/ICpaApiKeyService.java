package com.ruoyi.cpaexternal.apikey.service;

import java.util.List;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKeySyncStatus;
import com.ruoyi.cpaexternal.apikey.domain.vo.CpaApiKeySyncVO;

/** API Key 管理服务。 */
public interface ICpaApiKeyService
{
    CpaApiKey selectById(Long keyId);

    CpaApiKey selectByPlainKey(String plainApiKey);

    CpaApiKey selectByUserId(Long userId);

    List<CpaApiKey> selectList(CpaApiKey query);

    /** 实时对比平台 ai_apikey 与 CLIProxyAPI api-keys，返回分类差异明细。 */
    CpaApiKeySyncStatus selectCpaSyncStatus();

    /** 列表查询并附加每行的 CPA 同步状态（走短 TTL 缓存，CPA 不可用时标记未知）。 */
    List<CpaApiKeySyncVO> selectListWithCpaSync(CpaApiKey query);

    /** 失效 CPA api-keys 展示缓存（同步动作成功后调用，让列表立即反映新状态）。 */
    void evictCpaKeysCache();

    CpaApiKey ensureDefault(Long userId);

    CpaApiKey rotate(Long userId);

    /** 管理员自定义指定密钥的明文值，旧密钥立即失效。 */
    CpaApiKey customizeSecret(Long keyId, String plainApiKey);

    CpaApiKey create(CpaApiKey apiKey);

    int update(CpaApiKey apiKey);

    int deleteByIds(Long[] keyIds);
}
