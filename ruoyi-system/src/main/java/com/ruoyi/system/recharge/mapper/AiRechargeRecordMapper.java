package com.ruoyi.system.recharge.mapper;

import java.util.List;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;

/**
 * AI充值记录 数据层
 */
public interface AiRechargeRecordMapper
{
    public AiRechargeRecord selectAiRechargeRecordById(Long recordId);

    public List<AiRechargeRecord> selectAiRechargeRecordList(AiRechargeRecord aiRechargeRecord);

    public int insertAiRechargeRecord(AiRechargeRecord aiRechargeRecord);
}
