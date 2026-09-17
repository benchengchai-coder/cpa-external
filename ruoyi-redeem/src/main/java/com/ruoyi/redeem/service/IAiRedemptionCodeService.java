package com.ruoyi.redeem.service;

import java.util.List;
import com.ruoyi.redeem.domain.AiRedemptionCode;
import com.ruoyi.redeem.domain.RedeemResult;

/**
 * AI兑换码 服务层（纯兑换码域）。
 * <p>
 * 本接口仅依赖 ruoyi-common，只操作 ai_redemption_code 表；
 * {@link #redeemCode} 只核销兑换码，加余额/写账户流水/触发返利由 RedeemFacade 编排。
 */
public interface IAiRedemptionCodeService
{
    public AiRedemptionCode selectAiRedemptionCodeById(Long codeId);

    public List<AiRedemptionCode> selectAiRedemptionCodeList(AiRedemptionCode aiRedemptionCode);

    public int insertAiRedemptionCode(AiRedemptionCode aiRedemptionCode);

    public int updateAiRedemptionCode(AiRedemptionCode aiRedemptionCode);

    public int deleteAiRedemptionCodeById(Long codeId);

    public int deleteAiRedemptionCodeByIds(Long[] codeIds);

    /**
     * 批量生成兑换码。
     */
    public int batchInsertAiRedemptionCode(AiRedemptionCode aiRedemptionCode);

    /**
     * 用户兑换码核销（纯域：只标记码已用，返回结果给聚合层）。
     * <p>
     * 不在此加余额、不写账户流水、不触发返利 —— 由聚合层基于 {@link RedeemResult} 编排。
     */
    public RedeemResult redeemCode(String redemptionKey, Long userId, String username);
}
