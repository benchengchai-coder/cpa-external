package com.ruoyi.cpaexternal.billing.service.impl;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingRecord;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingRecordDetailVO;
import com.ruoyi.cpaexternal.billing.mapper.CpaBillingRecordMapper;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingRecordQueryService;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;
import com.ruoyi.cpaexternal.log.mapper.CpaAiLogMapper;
import com.ruoyi.common.exception.ServiceException;

/** CLIProxyAPI 账单只读查询服务实现。 */
@Service
public class CpaBillingRecordQueryServiceImpl implements ICpaBillingRecordQueryService
{
    @Autowired
    private CpaAiLogMapper aiLogMapper;

    @Autowired
    private CpaBillingRecordMapper billingRecordMapper;

    @Override
    public CpaBillingRecordDetailVO selectUserBillingByLogId(Long logId, Long userId)
    {
        CpaAiLog aiLog = aiLogMapper.selectById(logId);
        if (aiLog == null || userId == null || !Objects.equals(userId, aiLog.getUserId()))
        {
            throw new ServiceException("账单记录不存在或无权查看");
        }
        CpaBillingRecord billingRecord = selectBillingRecord(aiLog);
        if (billingRecord == null || !Objects.equals(logId, billingRecord.getLogId()))
        {
            return null;
        }
        if (!Objects.equals(userId, billingRecord.getUserId())
            || !Objects.equals(aiLog.getUserId(), billingRecord.getUserId()))
        {
            throw new ServiceException("账单记录不存在或无权查看");
        }
        return CpaBillingRecordDetailVO.from(billingRecord);
    }

    @Override
    public CpaBillingRecordDetailVO selectAdminBillingByLogId(Long logId)
    {
        CpaAiLog aiLog = aiLogMapper.selectById(logId);
        if (aiLog == null)
        {
            throw new ServiceException("调用日志不存在");
        }
        CpaBillingRecord billingRecord = selectBillingRecord(aiLog);
        if (billingRecord == null || !Objects.equals(logId, billingRecord.getLogId()))
        {
            return null;
        }
        if (!Objects.equals(aiLog.getUserId(), billingRecord.getUserId()))
        {
            throw new ServiceException("账单与调用日志归属不一致");
        }
        return CpaBillingRecordDetailVO.from(billingRecord);
    }

    @Override
    public CpaBillingAmountSummaryVO selectUserAmountSummary(Long userId)
    {
        CpaBillingAmountSummaryVO summary = billingRecordMapper.selectUserAmountSummary(userId);
        if (summary == null)
        {
            summary = new CpaBillingAmountSummaryVO();
        }
        summary.setTotalChargedAmount(nvl(summary.getTotalChargedAmount()));
        summary.setTotalUncoveredAmount(nvl(summary.getTotalUncoveredAmount()));
        return summary;
    }

    private CpaBillingRecord selectBillingRecord(CpaAiLog aiLog)
    {
        if (aiLog.getRequestId() == null)
        {
            return null;
        }
        return billingRecordMapper.selectByRequestId(aiLog.getRequestId());
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }
}
