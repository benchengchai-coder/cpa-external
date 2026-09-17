package com.ruoyi.invite.domain;

import java.math.BigDecimal;

/**
 * 邀请返利系统常量。
 * <p>
 * 包含动作/来源状态码、邀请码生成规则与默认值范围。
 * 配置参数键保留在此，由聚合层（facade）读取并组装为 {@link InviteConfig} 传入。
 */
public class AiInviteConstants
{
    /** 产生返利（流水动作） */
    public static final String ACTION_ACCRUE = "1";

    /** 领取返利（流水动作） */
    public static final String ACTION_CLAIM = "2";

    /** 返利来源：在线支付 */
    public static final String SOURCE_ONLINE_PAY = "1";

    /** 返利来源：兑换码 */
    public static final String SOURCE_REDEEM_CODE = "2";

    /** 邀请码长度（12位，字符集剔除易混淆的 I/O/0/1） */
    public static final int INVITE_CODE_LENGTH = 12;

    /** 邀请码生成冲突时的最大重试次数 */
    public static final int INVITE_CODE_MAX_ATTEMPTS = 12;

    /** 邀请码字符集（32个字符，不含 I/O/0/1） */
    public static final char[] INVITE_CODE_CHARSET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    /** 邀请码格式正则（大写字母数字下划线短横，4-32位） */
    public static final String INVITE_CODE_PATTERN = "^[A-Z0-9_-]{4,32}$";

    // ==================== 配置参数键（供 facade 读取） ====================

    /** 系统参数键：总开关 */
    public static final String CONFIG_INVITE_ENABLED = "ai.invite.enabled";

    /** 系统参数键：全局返利比例（百分比） */
    public static final String CONFIG_INVITE_REBATE_RATE = "ai.invite.rebateRate";

    /** 系统参数键：冻结期（小时） */
    public static final String CONFIG_INVITE_FREEZE_HOURS = "ai.invite.freezeHours";

    /** 系统参数键：有效期（天） */
    public static final String CONFIG_INVITE_DURATION_DAYS = "ai.invite.durationDays";

    /** 系统参数键：单人累计上限（美元） */
    public static final String CONFIG_INVITE_PER_INVITEE_CAP = "ai.invite.perInviteeCap";

    // ==================== 默认值与边界 ====================

    /** 默认：全局返利比例 20% */
    public static final BigDecimal DEFAULT_REBATE_RATE = new BigDecimal("20");

    /** 返利比例下限 */
    public static final BigDecimal REBATE_RATE_MIN = BigDecimal.ZERO;

    /** 返利比例上限 */
    public static final BigDecimal REBATE_RATE_MAX = new BigDecimal("100");

    /** 默认：冻结期 0 小时（不冻结） */
    public static final int DEFAULT_FREEZE_HOURS = 0;

    /** 冻结期上限（720小时=30天） */
    public static final int MAX_FREEZE_HOURS = 720;

    /** 默认：有效期 0 天（永久） */
    public static final int DEFAULT_DURATION_DAYS = 0;

    /** 有效期上限（3650天≈10年） */
    public static final int MAX_DURATION_DAYS = 3650;

    /** 默认：单人累计上限 0（无上限） */
    public static final BigDecimal DEFAULT_PER_INVITEE_CAP = BigDecimal.ZERO;

    /** 金额运算小数位数（与 balance/quota 一致） */
    public static final int AMOUNT_SCALE = 10;

    private AiInviteConstants()
    {
    }
}
