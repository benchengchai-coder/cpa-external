package com.ruoyi.invite.mapper;

import java.util.List;
import com.ruoyi.invite.domain.AiInviteAffiliate;

/**
 * 邀请返利关系 数据层
 */
public interface AiInviteAffiliateMapper
{
    /**
     * 按用户ID查询（带锁）
     */
    public AiInviteAffiliate selectByUserId(Long userId);

    /**
     * 按用户ID查询并加行锁（FOR UPDATE），用于领取等需要原子读写的场景
     */
    public AiInviteAffiliate selectByUserIdForUpdate(Long userId);

    /**
     * 按邀请码查询
     */
    public AiInviteAffiliate selectByCode(String inviteCode);

    /**
     * 新建邀请返利关系记录
     */
    public int insertAffiliate(AiInviteAffiliate affiliate);

    /**
     * 条件绑定邀请人：仅当当前 inviter_id 为空时绑定（并发幂等）。
     * @return 影响行数，1=绑定成功，0=已被绑定
     */
    public int bindInviter(Long userId, Long inviterId);

    /**
     * 邀请人累计邀请人数 +1
     */
    public int incrementInviteCount(Long inviterId);

    /**
     * 累加返利额度（待领取/冻结/历史同步更新）
     */
    public int addRebate(Long userId, java.math.BigDecimal pendingDelta, java.math.BigDecimal frozenDelta);

    /**
     * 冻结额转入待领取（解冻），同时更新流水
     */
    public int thawFrozen(Long userId, java.math.BigDecimal thawDelta);

    /**
     * 条件清零待领取：仅当 pending_rebate 大于 0 时清零并返回原值（领取幂等）
     * @return 影响行数，1=领取成功
     */
    public int clearPending(Long userId);

    /**
     * 更新专属返利比例（NULL 表示清除，沿用全局）
     */
    public int updateRebateRate(Long userId, java.math.BigDecimal rebateRate);

    /**
     * 重置邀请码
     */
    public int updateInviteCode(Long userId, String inviteCode);

    /**
     * 管理员列表查询（支持按用户名/邀请码模糊、是否绑定邀请人筛选）
     */
    public List<AiInviteAffiliate> selectAffiliateList(AiInviteAffiliate query);
}
