package com.ruoyi.cpaexternal.billing.domain;

/** CLIProxyAPI 外部计费常量。 */
public final class CpaBillingConstants
{
    public static final String SOURCE_WALLET = "wallet";
    public static final String SOURCE_SUBSCRIPTION = "subscription";
    public static final String SOURCE_MIXED = "mixed";
    public static final String SOURCE_NO_CHARGE = "no_charge";
    public static final String SOURCE_WRITE_OFF = "write_off";

    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_RESERVED = "reserved";
    public static final String STATUS_PENDING_SETTLEMENT = "pending_settlement";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_PARTIAL = "partial";
    public static final String STATUS_RELEASED = "released";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_WRITTEN_OFF = "written_off";

    public static final String TASK_PENDING = "pending";
    public static final String TASK_CLAIMED = "claimed";
    public static final String TASK_DONE = "done";
    public static final String TASK_FAILED = "failed";
    public static final String TASK_RESOLVED = "resolved";


    /** AI 请求最低计费金额配置键，在结算边界应用。 */
    public static final String CONFIG_MINIMUM_AMOUNT = "ai.billing.minimumAmount";

    private CpaBillingConstants()
    {
    }
}
