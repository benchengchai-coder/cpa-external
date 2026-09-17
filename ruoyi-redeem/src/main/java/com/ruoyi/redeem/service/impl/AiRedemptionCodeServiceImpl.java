package com.ruoyi.redeem.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.redeem.domain.AiRedemptionCode;
import com.ruoyi.redeem.domain.RedeemResult;
import com.ruoyi.redeem.mapper.AiRedemptionCodeMapper;
import com.ruoyi.redeem.service.IAiRedemptionCodeService;

/**
 * AI兑换码服务实现（纯兑换码域）。
 * <p>
 * 只操作 ai_redemption_code 表；加余额、写账户流水 type=1、触发返利
 * 由聚合层（RedeemFacade）基于 {@link RedeemResult} 编排。
 */
@Service
public class AiRedemptionCodeServiceImpl implements IAiRedemptionCodeService
{
    @Autowired
    private AiRedemptionCodeMapper aiRedemptionCodeMapper;

    @Override
    public AiRedemptionCode selectAiRedemptionCodeById(Long codeId)
    {
        return aiRedemptionCodeMapper.selectAiRedemptionCodeById(codeId);
    }

    @Override
    public List<AiRedemptionCode> selectAiRedemptionCodeList(AiRedemptionCode aiRedemptionCode)
    {
        return aiRedemptionCodeMapper.selectAiRedemptionCodeList(aiRedemptionCode);
    }

    @Override
    public int insertAiRedemptionCode(AiRedemptionCode aiRedemptionCode)
    {
        aiRedemptionCode.setRedemptionKey(IdUtils.fastSimpleUUID());
        return aiRedemptionCodeMapper.insertAiRedemptionCode(aiRedemptionCode);
    }

    @Override
    public int updateAiRedemptionCode(AiRedemptionCode aiRedemptionCode)
    {
        return aiRedemptionCodeMapper.updateAiRedemptionCode(aiRedemptionCode);
    }

    @Override
    @Transactional
    public int deleteAiRedemptionCodeById(Long codeId)
    {
        return aiRedemptionCodeMapper.deleteAiRedemptionCodeById(codeId);
    }

    @Override
    @Transactional
    public int deleteAiRedemptionCodeByIds(Long[] codeIds)
    {
        int rows = 0;
        for (Long codeId : codeIds)
        {
            rows += deleteAiRedemptionCodeById(codeId);
        }
        return rows;
    }

    @Override
    @Transactional
    public int batchInsertAiRedemptionCode(AiRedemptionCode aiRedemptionCode)
    {
        int count = aiRedemptionCode.getBatchCount() != null ? aiRedemptionCode.getBatchCount() : 1;
        count = Math.min(count, 100);
        int rows = 0;
        for (int i = 0; i < count; i++)
        {
            AiRedemptionCode code = new AiRedemptionCode();
            code.setCodeName(aiRedemptionCode.getCodeName());
            code.setQuota(aiRedemptionCode.getQuota());
            code.setRedemptionKey(IdUtils.fastSimpleUUID());
            code.setStatus("0");
            code.setExpiredTime(aiRedemptionCode.getExpiredTime());
            code.setCreateBy(aiRedemptionCode.getCreateBy());
            code.setRemark(aiRedemptionCode.getRemark());
            rows += aiRedemptionCodeMapper.insertAiRedemptionCode(code);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedeemResult redeemCode(String redemptionKey, Long userId, String username)
    {
        AiRedemptionCode code = aiRedemptionCodeMapper.selectAiRedemptionCodeByKey(redemptionKey);
        if (code == null)
        {
            throw new ServiceException("兑换码不存在");
        }
        if (!"0".equals(code.getStatus()))
        {
            throw new ServiceException("兑换码已被使用或已禁用");
        }
        if (code.getExpiredTime() != null && code.getExpiredTime().before(new Date()))
        {
            throw new ServiceException("兑换码已过期");
        }
        // 标记兑换码已用
        Date redeemedTime = new Date();
        AiRedemptionCode update = new AiRedemptionCode();
        update.setCodeId(code.getCodeId());
        update.setStatus("1");
        update.setUsedUserId(userId);
        update.setUsedUsername(username);
        update.setRedeemedTime(redeemedTime);
        aiRedemptionCodeMapper.updateAiRedemptionCode(update);

        // 余额、账户流水、返利由聚合层基于 RedeemResult 编排
        return new RedeemResult(userId, username, code.getQuota(), code.getCodeId(),
                code.getCodeName(), redeemedTime);
    }
}
