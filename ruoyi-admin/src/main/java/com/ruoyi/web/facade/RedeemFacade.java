package com.ruoyi.web.facade;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.invite.domain.AiInviteConstants;
import com.ruoyi.redeem.domain.AiRedemptionCode;
import com.ruoyi.redeem.domain.RedeemResult;
import com.ruoyi.redeem.service.IAiRedemptionCodeService;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.recharge.domain.AiRechargeConstants;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;
import com.ruoyi.system.recharge.service.IAiRechargeRecordService;

/**
 * 兑换码服务编排 Facade（聚合层）。
 * <p>
 * 串联「纯兑换码域」与「用户余额 / 账户流水 / 邀请返利」：
 * {@link #redeem} 编排：纯域核销 → 加余额 → 写账户流水 type=1 → 触发返利。
 * 管理员 CRUD 方法透传纯域，无需编排。
 */
@Component
public class RedeemFacade
{
    @Autowired
    private IAiRedemptionCodeService redemptionCodeService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private IAiRechargeRecordService rechargeRecordService;

    @Autowired
    private InviteFacade inviteFacade;

    /**
     * 用户兑换码核销（编排：纯域核销 → 加余额 → 写账户流水 → 触发返利）。
     */
    @Transactional(rollbackFor = Exception.class)
    public RedeemResult redeem(String redemptionKey, Long userId, String username)
    {
        // 1. 纯域核销（只标记码已用）
        RedeemResult result = redemptionCodeService.redeemCode(redemptionKey, userId, username);

        // 2. 加余额
        sysUserMapper.addUserBalance(userId, result.getQuota());

        // 3. 写账户流水 type=1 兑换码
        AiRechargeRecord record = new AiRechargeRecord();
        record.setUserId(userId);
        record.setUsername(username);
        record.setType(AiRechargeConstants.TYPE_REDEEM_CODE);
        record.setAmount(result.getQuota());
        record.setSourceId(result.getCodeId());
        record.setSourceName(result.getCodeName());
        record.setStatus("0");
        rechargeRecordService.insertAiRechargeRecord(record);

        // 4. 邀请返利：兑换码充值成功后触发（幂等由流水表 uk_source + 兑换码一次性消费控制）
        inviteFacade.accrueRebate(userId, result.getQuota(),
                AiInviteConstants.SOURCE_REDEEM_CODE, record.getRecordId());

        return result;
    }

    // ==================== 管理员侧（透传） ====================

    public List<AiRedemptionCode> selectAiRedemptionCodeList(AiRedemptionCode query)
    {
        return redemptionCodeService.selectAiRedemptionCodeList(query);
    }

    public AiRedemptionCode selectAiRedemptionCodeById(Long codeId)
    {
        return redemptionCodeService.selectAiRedemptionCodeById(codeId);
    }

    public int insertAiRedemptionCode(AiRedemptionCode aiRedemptionCode)
    {
        return redemptionCodeService.insertAiRedemptionCode(aiRedemptionCode);
    }

    public int batchInsertAiRedemptionCode(AiRedemptionCode aiRedemptionCode)
    {
        return redemptionCodeService.batchInsertAiRedemptionCode(aiRedemptionCode);
    }

    public int updateAiRedemptionCode(AiRedemptionCode aiRedemptionCode)
    {
        return redemptionCodeService.updateAiRedemptionCode(aiRedemptionCode);
    }

    public int deleteAiRedemptionCodeByIds(Long[] codeIds)
    {
        return redemptionCodeService.deleteAiRedemptionCodeByIds(codeIds);
    }
}
