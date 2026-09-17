package com.ruoyi.cpaexternal.subscription.domain;

/**
 * AI订阅常量。
 */
public class AiSubscriptionConstants
{
    public static final String PLAN_STATUS_NORMAL = "0";

    public static final String PLAN_STATUS_DISABLED = "1";

    public static final String SUB_STATUS_ACTIVE = "active";

    public static final String SUB_STATUS_CANCELLED = "cancelled";

    public static final String SUB_STATUS_EXPIRED = "expired";

    public static final String SOURCE_GRANT = "grant";

    public static final String SOURCE_BALANCE_PURCHASE = "balance_purchase";

    public static final String SOURCE_REGISTER_TRIAL = "register_trial";

    public static final String RECORD_TYPE_GRANT = "grant";

    public static final String RECORD_TYPE_PURCHASE = "purchase";

    public static final String RECORD_TYPE_CANCEL = "cancel";

    public static final String RECORD_TYPE_EXPIRE = "expire";

    public static final String RECORD_TYPE_REPLACE = "replace";

    public static final String RECORD_STATUS_SUCCESS = "0";

    public static final String PREFERENCE_SUBSCRIPTION_FIRST = "subscription_first";

    public static final String PREFERENCE_WALLET_FIRST = "wallet_first";

    public static final String PREFERENCE_SUBSCRIPTION_ONLY = "subscription_only";

    public static final String PREFERENCE_WALLET_ONLY = "wallet_only";

    public static final String CONFIG_BALANCE_PURCHASE_ENABLED = "ai.subscription.balancePurchaseEnabled";

    public static final String CONFIG_REGISTER_TRIAL_PLAN_ID = "registerTrialPlanId";

    private AiSubscriptionConstants()
    {
    }
}
