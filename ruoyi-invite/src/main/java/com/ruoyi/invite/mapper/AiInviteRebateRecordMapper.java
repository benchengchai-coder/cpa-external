package com.ruoyi.invite.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.invite.domain.AiInviteRebateRecord;
import com.ruoyi.invite.domain.vo.InviteeVO;

/**
 * 邀请返利流水 数据层
 */
public interface AiInviteRebateRecordMapper
{
    /**
     * 幂等插入产生返利流水：依赖唯一索引 uk_source(source_type, source_id) 防重。
     * source_type/source_id 为空时不防重（退化为普通插入）。
     * @return 影响行数，1=插入成功，0=已存在（重复返利）
     */
    public int insertAccrueIgnore(AiInviteRebateRecord record);

    /**
     * 插入领取返利流水
     */
    public int insertClaim(AiInviteRebateRecord record);

    /**
     * 回填领取流水的余额快照（聚合层加完余额后调用，保留审计信息）。
     */
    public int updateClaimBalanceAfter(@Param("recordId") Long recordId, @Param("balanceAfter") BigDecimal balanceAfter);

    /**
     * 将到期的冻结流水标记为已解冻（frozen_until 置空），并返回解冻金额合计。
     * 仅处理 frozen_until <= now 的记录。
     */
    public BigDecimal sumAndClearThawAmount(@Param("inviterId") Long inviterId);

    /**
     * 统计某邀请人从某被邀请人累计产生的返利（用于单人上限计算）
     */
    public BigDecimal sumAccruedByInvitee(@Param("inviterId") Long inviterId, @Param("inviteeId") Long inviteeId);

    /**
     * 将到期的冻结流水标记为已解冻（frozen_until 置空），防止重复汇总解冻。
     * 仅处理 frozen_until <= now 的记录。
     */
    public int clearThawFlag(@Param("inviterId") Long inviterId);

    /**
     * 查询某邀请人的被邀请人列表（含每人贡献返利合计，邮箱脱敏），最多 limit 条
     */
    public List<InviteeVO> selectInviteeList(@Param("inviterId") Long inviterId, @Param("limit") int limit);

    /**
     * 管理员按收益人分页查询流水
     */
    public List<AiInviteRebateRecord> selectRecordList(AiInviteRebateRecord query);
}
