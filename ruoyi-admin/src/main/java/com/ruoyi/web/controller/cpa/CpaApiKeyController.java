package com.ruoyi.web.controller.cpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.domain.vo.CpaApiKeySyncVO;
import com.ruoyi.cpaexternal.apikey.service.CpaApiKeyPushService;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * API Key 增删改查及用户密钥管理。
 *
 * <p>该 Controller 只管理 API Key 数据，不提供 AI 请求转发接口。</p>
 */
@RestController
@RequestMapping("/aigate/api-key")
public class CpaApiKeyController extends BaseController
{
    @Autowired
    private ICpaApiKeyService apiKeyService;

    @Autowired
    private CpaApiKeyPushService apiKeyPushService;

    @Autowired
    private ISysUserService sysUserService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    public TableDataInfo list(CpaApiKey query)
    {
        if (!SecurityUtils.isAdmin())
        {
            query.setUserId(getUserId());
        }
        startPage();
        List<CpaApiKeySyncVO> list = apiKeyService.selectListWithCpaSync(query);
        list.forEach(this::hideSecret);
        return getDataTable(list);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/self")
    public AjaxResult self()
    {
        return success(apiKeyService.ensureDefault(getUserId()));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/self/rotate")
    @Log(title = "API Key轮换", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult rotateSelf()
    {
        return success(apiKeyService.rotate(getUserId()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{keyId}")
    public AjaxResult getInfo(@PathVariable Long keyId)
    {
        CpaApiKey apiKey = apiKeyService.selectById(keyId);
        checkOwner(apiKey);
        hideSecret(apiKey);
        return success(apiKey);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{keyId}/secret")
    public AjaxResult getSecret(@PathVariable Long keyId)
    {
        CpaApiKey apiKey = apiKeyService.selectById(keyId);
        if (apiKey == null)
        {
            throw new ServiceException("API Key不存在");
        }
        checkOwner(apiKey);
        return success(Collections.singletonMap("apiKey", apiKey.getApiKey()));
    }

    @PreAuthorize("@ss.hasPermi('system:user:list') and @ss.hasPermi('aigate:apiKey:list') and @ss.hasPermi('aigate:apiKey:query')")
    @GetMapping("/admin/users/{userId}/secrets")
    public AjaxResult adminUserSecrets(@PathVariable Long userId)
    {
        sysUserService.checkUserDataScope(userId);
        if (sysUserService.selectUserById(userId) == null)
        {
            throw new ServiceException("用户不存在");
        }
        CpaApiKey query = new CpaApiKey();
        query.setUserId(userId);
        List<Map<String, Object>> secrets = new ArrayList<>();
        for (CpaApiKey apiKey : apiKeyService.selectList(query))
        {
            Map<String, Object> secret = new HashMap<>();
            secret.put("keyId", apiKey.getKeyId());
            secret.put("keyName", apiKey.getKeyName());
            secret.put("apiKey", apiKey.getApiKey());
            secrets.add(secret);
        }
        return success(secrets);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{keyId}/secret")
    @Log(title = "API Key自定义", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    public AjaxResult customizeSecret(@PathVariable Long keyId, @RequestBody Map<String, String> body)
    {
        checkAdminMutation();
        String plainApiKey = body == null ? null : body.get("apiKey");
        return success(apiKeyService.customizeSecret(keyId, plainApiKey));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public AjaxResult add(@RequestBody CpaApiKey apiKey)
    {
        checkAdminMutation();
        return success(apiKeyService.create(apiKey));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping
    public AjaxResult edit(@RequestBody CpaApiKey apiKey)
    {
        checkAdminMutation();
        CpaApiKey oldApiKey = apiKeyService.selectById(apiKey.getKeyId());
        if (oldApiKey == null)
        {
            throw new ServiceException("API Key不存在");
        }
        checkOwner(oldApiKey);
        apiKey.setUserId(oldApiKey.getUserId());
        return toAjax(apiKeyService.update(apiKey));
    }

    @PreAuthorize("isAuthenticated()")
    @Log(title = "API Key", businessType = BusinessType.DELETE)
    @DeleteMapping("/{keyIds}")
    public AjaxResult remove(@PathVariable Long[] keyIds)
    {
        checkAdminMutation();
        for (Long keyId : keyIds)
        {
            checkOwner(apiKeyService.selectById(keyId));
        }
        return toAjax(apiKeyService.deleteByIds(keyIds));
    }

    /** 实时对比平台与 CLIProxyAPI 的 api-keys，返回分类差异明细（仅管理员）。 */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/cpa-sync-status")
    public AjaxResult cpaSyncStatus()
    {
        checkAdminMutation();
        return success(apiKeyService.selectCpaSyncStatus());
    }

    /**
     * 手动把指定 Key 同步到 CLIProxyAPI 使其生效。
     *
     * <p>用于实时推送失败、推送开关后启用的存量 Key 等场景的人工补偿入口：
     * 启用状态推送至 CPA，停用状态则从 CPA 移除，与自动同步语义一致。</p>
     */
    @PreAuthorize("isAuthenticated()")
    @Log(title = "API Key同步CPA", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{keyId}/sync")
    public AjaxResult syncToCpa(@PathVariable Long keyId)
    {
        checkAdminMutation();
        CpaApiKey apiKey = apiKeyService.selectById(keyId);
        if (apiKey == null)
        {
            throw new ServiceException("API Key不存在");
        }
        if (StringUtils.isEmpty(apiKey.getApiKey()))
        {
            throw new ServiceException("API Key明文缺失，无法同步");
        }
        if (!apiKeyPushService.isEnabled())
        {
            throw new ServiceException("API Key推送同步未开启，请先配置 cpa.cli-proxy.management.api-key-push-enabled=true 并重启后端");
        }
        if (!"0".equals(apiKey.getStatus()))
        {
            apiKeyPushService.removeKey(apiKey.getApiKey());
            apiKeyService.evictCpaKeysCache();
            return success("该Key为停用状态，已从CLIProxyAPI移除");
        }
        apiKeyPushService.upsertKey(apiKey.getApiKey());
        apiKeyService.evictCpaKeysCache();
        return success("已推送到CLIProxyAPI并生效");
    }

    private void checkAdminMutation()
    {
        if (!SecurityUtils.isAdmin())
        {
            throw new ServiceException("普通用户只能查看或更换自己的API Key");
        }
    }

    private void checkOwner(CpaApiKey apiKey)
    {
        if (!SecurityUtils.isAdmin()
                && (apiKey == null || !getUserId().equals(apiKey.getUserId())))
        {
            throw new ServiceException("无权访问该API Key");
        }
    }

    private void hideSecret(CpaApiKey apiKey)
    {
        if (apiKey != null)
        {
            apiKey.setApiKey(null);
        }
    }
}
