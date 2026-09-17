package com.ruoyi.invite.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.invite.domain.AiInviteAffiliate;
import com.ruoyi.invite.domain.AiInviteRebateRecord;
import com.ruoyi.invite.domain.ClaimResult;
import com.ruoyi.invite.domain.InviteConfig;
import com.ruoyi.invite.domain.vo.InviteInfoVO;
import com.ruoyi.invite.domain.vo.InviteOverviewVO;

/**
 * 邀请返利 服务层（纯邀请域）。
 * <p>
 * 本接口仅依赖 ruoyi-common，不读取系统参数、不操作用户余额、不写账户流水；
 * 凡涉及配置/余额/流水的副作用，由聚合层（facade）传入 {@link InviteConfig} 或基于
 * {@link ClaimResult} 完成编排。
 */
public interface IAiInviteService
{
    /**
     * 确保用户存在邀请返利关系记录（无则创建并生成邀请码）。
     */
    public AiInviteAffiliate ensureAffiliate(Long userId);

    /**
     * 按邀请码绑定邀请人。
     * 空码/总开关关闭/已绑定 → 静默成功；防自邀、防重复绑定。
     * @param config 运行时配置（控制总开关）
     */
    public void bindInviter(Long userId, String inviteCode, InviteConfig config);

    /**
     * 触发返利（被邀请人充值/兑换后调用）。
     * 总开关关/无邀请人/金额<=0/超期/达上限 → 返回 0。
     * @param inviteeId  被邀请人ID
     * @param baseAmount 充值基数
     * @param sourceType 来源类型（1在线支付 2兑换码）
     * @param sourceId   来源ID（充值记录ID/订单ID），用于幂等
     * @param config     运行时配置
     * @return 实际产生的返利金额，0 表示未产生
     */
    public BigDecimal accrueRebate(Long inviteeId, BigDecimal baseAmount, String sourceType, Long sourceId,
                                   InviteConfig config);

    /**
     * 领取全部待领返利（纯域：只更新邀请关系与返利账本，返回结果给聚合层）。
     * <p>
     * 不在此加余额、不写账户流水 —— 由聚合层基于 {@link ClaimResult} 编排。
     * @param balanceAfter 领取后余额快照（由聚合层加完余额后传入，写入返利账本；首次调用可传 null）
     * @return 领取结果
     */
    public ClaimResult claimRebate(Long inviterId, BigDecimal balanceAfter);

    /**
     * 回填领取返利流水的余额快照（聚合层加完余额后调用，保留审计信息）。
     */
    public void updateClaimBalanceAfter(Long rebateRecordId, BigDecimal balanceAfter);

    /**
     * 获取当前用户的邀请返利信息（用户视角）。
     * @param config 运行时配置（总开关关闭时返回 null）
     */
    public InviteInfoVO getInviteInfo(Long userId, InviteConfig config);

    /**
     * 管理员：邀请关系分页列表。
     */
    public List<AiInviteAffiliate> selectAffiliateList(AiInviteAffiliate query);

    /**
     * 管理员：返利流水分页列表。
     */
    public List<AiInviteRebateRecord> selectRecordList(AiInviteRebateRecord query);

    /**
     * 管理员：单用户概览。
     */
    public InviteOverviewVO getOverview(Long userId);

    /**
     * 管理员：设置专属返利比例（null 表示清除，沿用全局）。
     */
    public int setRebateRate(Long userId, BigDecimal rebateRate);

    /**
     * 管理员：重置用户邀请码。
     */
    public String resetInviteCode(Long userId);
}
