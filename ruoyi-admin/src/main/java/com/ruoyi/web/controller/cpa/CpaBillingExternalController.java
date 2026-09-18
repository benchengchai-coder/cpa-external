package com.ruoyi.web.controller.cpa;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.mapper.SysUserMapper;

/**
 * CLIProxyAPI 公开计费 API。
 *
 * <p>供 CLIProxyAPI 诊断余额/订阅状态；实际计费在 usage 落库后完成。
 * 路径在 SecurityConfig 中放行，请求方必须携带配置的 X-Billing-Token；
 * 对接契约见 docs/cliproxy-billing-integration.md。</p>
 */
@RestController
@RequestMapping("/aigate/billing")
public class CpaBillingExternalController
{
    /** 公开计费 API 的鉴权头；避开 Authorization，防止 JWT 过滤器解析报错刷屏。 */
    private static final String TOKEN_HEADER = "X-Billing-Token";

    private static final String KEY_STATUS_NORMAL = "0";

    private static final String USER_STATUS_NORMAL = "0";

    @Autowired
    private ICpaApiKeyService apiKeyService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AiUserSubscriptionMapper userSubscriptionMapper;

    @Autowired
    private CpaBillingProperties billingProperties;

    /** 余额/订阅/Key 配额只读校验，供 CLIProxyAPI 或诊断工具预检。 */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> check(@RequestBody Map<String, Object> request,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token)
    {
        ResponseEntity<Void> denied = authorize(token);
        if (denied != null)
        {
            return ResponseEntity.status(denied.getStatusCode()).build();
        }
        String apiKey = request == null ? null : (String) request.get("api_key");
        Map<String, Object> body = new LinkedHashMap<>();
        if (StringUtils.isEmpty(apiKey))
        {
            body.put("allowed", false);
            body.put("reason", "api_key不能为空");
            return ResponseEntity.ok(body);
        }

        CpaApiKey key = apiKeyService.selectByPlainKey(apiKey);
        if (key == null || !KEY_STATUS_NORMAL.equals(key.getStatus()))
        {
            body.put("allowed", false);
            body.put("reason", "API Key无效或已停用");
            return ResponseEntity.ok(body);
        }
        SysUser user = sysUserMapper.selectUserBillingSnapshot(key.getUserId());
        if (user == null || !USER_STATUS_NORMAL.equals(user.getStatus()))
        {
            body.put("allowed", false);
            body.put("reason", "API Key关联用户不存在或已停用");
            return ResponseEntity.ok(body);
        }

        body.put("allowed", true);
        body.put("user_id", user.getUserId());
        body.put("key_id", key.getKeyId());
        body.put("billing_preference", user.getBillingPreference());

        Map<String, Object> wallet = new LinkedHashMap<>();
        BigDecimal balance = nvl(user.getBalance());
        BigDecimal walletFrozen = nvl(user.getFrozenBalance());
        wallet.put("balance", balance);
        wallet.put("frozen_balance", walletFrozen);
        wallet.put("available_balance", positive(balance.subtract(walletFrozen)));
        body.put("wallet", wallet);

        AiUserSubscription subscription = userSubscriptionMapper
                .selectActiveSubscriptionSnapshotForBilling(user.getUserId());
        Map<String, Object> subscriptionInfo = new LinkedHashMap<>();
        if (subscription != null)
        {
            subscriptionInfo.put("subscription_id", subscription.getSubscriptionId());
            subscriptionInfo.put("plan_title", subscription.getPlanTitle());
            subscriptionInfo.put("amount_total", subscription.getAmountTotal());
            subscriptionInfo.put("amount_used", subscription.getAmountUsed());
            subscriptionInfo.put("frozen_balance", subscription.getFrozenBalance());
            subscriptionInfo.put("available_amount", subscription.getAvailableAmount());
        }
        body.put("subscription", subscriptionInfo);

        // AI并发信息仅作诊断参考，不参与计费判定
        Map<String, Object> concurrency = new LinkedHashMap<>();
        concurrency.put("limit", user.getAiConcurrencyLimit());
        concurrency.put("active", user.getActiveRequestCount() == null ? 0 : user.getActiveRequestCount());
        body.put("concurrency", concurrency);

        // Key 级配额已下线，契约结构保留并固定返回无限额度，保证 CLIProxyAPI 计费插件兼容。
        Map<String, Object> keyInfo = new LinkedHashMap<>();
        keyInfo.put("unlimited_balance", true);
        keyInfo.put("remain_balance", BigDecimal.ZERO);
        keyInfo.put("frozen_balance", BigDecimal.ZERO);
        keyInfo.put("available_balance", null);
        body.put("key", keyInfo);
        return ResponseEntity.ok(body);
    }

    /** 校验 X-Billing-Token；未配置 Token 时公开接口一律拒绝，防止裸奔部署。 */
    private ResponseEntity<Void> authorize(String token)
    {
        String expected = billingProperties.getApiToken();
        if (StringUtils.isEmpty(expected))
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String provided = token == null ? "" : token.trim();
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)))
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return null;
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal positive(BigDecimal value)
    {
        return value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.ZERO;
    }
}
