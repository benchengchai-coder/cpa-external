package com.ruoyi.web.controller.cpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.cpaexternal.billing.config.CpaBillingProperties;
import com.ruoyi.cpaexternal.billing.service.CpaBillingMinimumChargeResolver;
import com.ruoyi.cpaexternal.model.domain.CpaModel;
import com.ruoyi.cpaexternal.model.mapper.CpaModelMapper;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.mapper.AiUserSubscriptionMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;

/** 公开计费 /check 接口测试：Key/用户有效性校验与余额预检。 */
@ExtendWith(MockitoExtension.class)
class CpaBillingExternalControllerTest
{
    private static final Long USER_ID = 100L;

    private static final Long KEY_ID = 5L;

    private static final String API_KEY = "sk-client-123";

    @Mock
    private ICpaApiKeyService apiKeyService;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private AiUserSubscriptionMapper userSubscriptionMapper;

    @Mock
    private CpaBillingProperties billingProperties;

    @Mock
    private CpaModelMapper modelMapper;

    @Mock
    private CpaBillingMinimumChargeResolver minimumChargeResolver;

    private CpaBillingExternalController controller;

    @BeforeEach
    void setUp()
    {
        controller = new CpaBillingExternalController();
        ReflectionTestUtils.setField(controller, "apiKeyService", apiKeyService);
        ReflectionTestUtils.setField(controller, "sysUserMapper", sysUserMapper);
        ReflectionTestUtils.setField(controller, "userSubscriptionMapper", userSubscriptionMapper);
        ReflectionTestUtils.setField(controller, "billingProperties", billingProperties);
        ReflectionTestUtils.setField(controller, "modelMapper", modelMapper);
        ReflectionTestUtils.setField(controller, "minimumChargeResolver", minimumChargeResolver);
        lenient().when(billingProperties.getApiToken()).thenReturn("tok");
        lenient().when(billingProperties.isBalanceCheckEnabled()).thenReturn(true);
        lenient().when(minimumChargeResolver.applyMinimumAmount(org.mockito.ArgumentMatchers.any(BigDecimal.class)))
                .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(0)).max(new BigDecimal("0.2")));
    }

    private Map<String, Object> callCheck()
    {
        Map<String, Object> request = new HashMap<>();
        request.put("api_key", API_KEY);
        request.put("model", "gpt-4o");
        ResponseEntity<Map<String, Object>> response = controller.check(request, "tok");
        assertEquals(200, response.getStatusCode().value());
        return response.getBody();
    }

    private void mockValidKeyAndUser(String preference, String balance, String frozenBalance)
    {
        CpaApiKey key = new CpaApiKey();
        key.setKeyId(KEY_ID);
        key.setUserId(USER_ID);
        key.setStatus("0");
        when(apiKeyService.selectByPlainKey(API_KEY)).thenReturn(key);

        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        user.setStatus("0");
        user.setBillingPreference(preference);
        user.setBalance(new BigDecimal(balance));
        user.setFrozenBalance(new BigDecimal(frozenBalance));
        user.setBillingMultiplier(BigDecimal.ONE);
        when(sysUserMapper.selectUserBillingSnapshot(USER_ID)).thenReturn(user);
        when(userSubscriptionMapper.selectActiveSubscriptionSnapshotForBilling(USER_ID)).thenReturn(null);
        CpaModel model = new CpaModel();
        model.setModelName("gpt-4o");
        model.setOfficialInputPrice(new BigDecimal("1"));
        when(modelMapper.selectPricingByName("gpt-4o")).thenReturn(model);
    }

    private AiUserSubscription subscription(String total, String used)
    {
        AiUserSubscription subscription = new AiUserSubscription();
        subscription.setSubscriptionId(12L);
        subscription.setPlanTitle("月度套餐");
        subscription.setAmountTotal(new BigDecimal(total));
        subscription.setAmountUsed(new BigDecimal(used));
        subscription.setFrozenBalance(BigDecimal.ZERO);
        return subscription;
    }

    @Test
    void zeroBalanceWithoutSubscriptionShouldBeDenied()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_FIRST, "0", "0");

        Map<String, Object> body = callCheck();

        assertEquals(false, body.get("allowed"));
        assertTrue(((String) body.get("reason")).contains("余额"));
    }

    @Test
    void positiveWalletBalanceShouldBeAllowed()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_FIRST, "1", "0");

        Map<String, Object> body = callCheck();

        assertEquals(true, body.get("allowed"));
        assertEquals(USER_ID, body.get("user_id"));
        assertEquals(new BigDecimal("1.0000000000"), body.get("estimated_cost"));
    }

    @Test
    void billingMultiplierShouldIncreaseEstimatedCost()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_WALLET_ONLY, "2", "0");
        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        user.setStatus("0");
        user.setBillingPreference(AiSubscriptionConstants.PREFERENCE_WALLET_ONLY);
        user.setBalance(new BigDecimal("2"));
        user.setFrozenBalance(BigDecimal.ZERO);
        user.setBillingMultiplier(new BigDecimal("2"));
        when(sysUserMapper.selectUserBillingSnapshot(USER_ID)).thenReturn(user);

        Map<String, Object> body = callCheck();

        assertEquals(true, body.get("allowed"));
        assertEquals(new BigDecimal("2.0000000000"), body.get("estimated_cost"));
    }

    @Test
    void frozenBalanceShouldReduceUsableWallet()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_FIRST, "0.02", "0.02");

        Map<String, Object> body = callCheck();

        assertEquals(false, body.get("allowed"));
    }

    @Test
    void walletBalanceBelowEstimatedCostShouldBeDenied()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_WALLET_ONLY, "0.99", "0");

        Map<String, Object> body = callCheck();

        assertEquals(false, body.get("allowed"));
        assertEquals(new BigDecimal("1.0000000000"), body.get("estimated_cost"));
        assertEquals("钱包余额不足，请充值后再试", body.get("reason"));
    }

    @Test
    void activeSubscriptionShouldCoverZeroWallet()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_FIRST, "0", "0");
        when(userSubscriptionMapper.selectActiveSubscriptionSnapshotForBilling(USER_ID))
                .thenReturn(subscription("10", "3"));

        Map<String, Object> body = callCheck();

        assertEquals(true, body.get("allowed"));
    }

    @Test
    void unlimitedSubscriptionShouldBeAllowed()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_FIRST, "0", "0");
        // amount_total=0 表示不限量，getAvailableAmount() 返回 null
        when(userSubscriptionMapper.selectActiveSubscriptionSnapshotForBilling(USER_ID))
                .thenReturn(subscription("0", "0"));

        Map<String, Object> body = callCheck();

        assertEquals(true, body.get("allowed"));
    }

    @Test
    void exhaustedSubscriptionWithZeroWalletShouldBeDenied()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_FIRST, "0", "0");
        when(userSubscriptionMapper.selectActiveSubscriptionSnapshotForBilling(USER_ID))
                .thenReturn(subscription("10", "10"));

        Map<String, Object> body = callCheck();

        assertEquals(false, body.get("allowed"));
    }

    @Test
    void walletOnlyUserShouldIgnoreSubscriptionFunds()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_WALLET_ONLY, "0", "0");
        when(userSubscriptionMapper.selectActiveSubscriptionSnapshotForBilling(USER_ID))
                .thenReturn(subscription("10", "0"));

        Map<String, Object> body = callCheck();

        assertEquals(false, body.get("allowed"));
        assertEquals("钱包余额不足，请充值后再试", body.get("reason"));
    }

    @Test
    void subscriptionOnlyUserWithoutSubscriptionShouldBeDenied()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_SUBSCRIPTION_ONLY, "5", "0");

        Map<String, Object> body = callCheck();

        assertEquals(false, body.get("allowed"));
        assertEquals("订阅可用额度不足或已过期", body.get("reason"));
    }

    @Test
    void disabledBalanceCheckShouldAllowZeroBalance()
    {
        mockValidKeyAndUser(AiSubscriptionConstants.PREFERENCE_WALLET_ONLY, "0", "0");
        when(billingProperties.isBalanceCheckEnabled()).thenReturn(false);

        Map<String, Object> body = callCheck();

        assertEquals(true, body.get("allowed"));
    }

    @Test
    void stoppedKeyShouldBeDeniedBeforeBalanceCheck()
    {
        CpaApiKey key = new CpaApiKey();
        key.setKeyId(KEY_ID);
        key.setUserId(USER_ID);
        key.setStatus("1");
        when(apiKeyService.selectByPlainKey(API_KEY)).thenReturn(key);

        Map<String, Object> body = callCheck();

        assertEquals(false, body.get("allowed"));
        assertEquals("API Key无效或已停用", body.get("reason"));
    }

    @Test
    void wrongTokenShouldBeRejected()
    {
        Map<String, Object> request = new HashMap<>();
        request.put("api_key", API_KEY);
        ResponseEntity<Map<String, Object>> response = controller.check(request, "wrong");

        assertEquals(401, response.getStatusCode().value());
        assertFalse(response.hasBody());
    }
}
