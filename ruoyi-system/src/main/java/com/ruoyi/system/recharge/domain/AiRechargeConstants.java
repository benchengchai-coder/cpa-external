package com.ruoyi.system.recharge.domain;

/**
 * 充值记录类型常量（ai_recharge_record.type）。
 * <p>
 * 集中维护充值类型码，避免散落在各业务流程中的字符串字面量。
 */
public class AiRechargeConstants
{
    /** 1 兑换码 */
    public static final String TYPE_REDEEM_CODE = "1";

    /** 2 在线支付 */
    public static final String TYPE_ONLINE_PAY = "2";

    /** 3 管理员调整 */
    public static final String TYPE_ADMIN_ADJUST = "3";

    /** 4 新用户注册赠送 */
    public static final String TYPE_REGISTER_BONUS = "4";

    /** 5 每日签到奖励 */
    public static final String TYPE_DAILY_CHECKIN = "5";

    /** 6 邀请返利 */
    public static final String TYPE_INVITE_REBATE = "6";

    private AiRechargeConstants()
    {
    }
}
