package com.ruoyi.redeem.mapper;

import java.util.List;
import com.ruoyi.redeem.domain.AiRedemptionCode;

/**
 * AI兑换码 数据层
 */
public interface AiRedemptionCodeMapper
{
    public AiRedemptionCode selectAiRedemptionCodeById(Long codeId);

    public AiRedemptionCode selectAiRedemptionCodeByKey(String redemptionKey);

    public List<AiRedemptionCode> selectAiRedemptionCodeList(AiRedemptionCode aiRedemptionCode);

    public int insertAiRedemptionCode(AiRedemptionCode aiRedemptionCode);

    public int updateAiRedemptionCode(AiRedemptionCode aiRedemptionCode);

    public int deleteAiRedemptionCodeById(Long codeId);

    public int deleteAiRedemptionCodeByIds(Long[] codeIds);
}
