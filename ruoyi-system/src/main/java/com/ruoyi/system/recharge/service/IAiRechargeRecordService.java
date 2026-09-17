package com.ruoyi.system.recharge.service;

import java.util.List;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;

/**
 * AI充值记录 服务层
 */
public interface IAiRechargeRecordService
{
    public List<AiRechargeRecord> selectAiRechargeRecordList(AiRechargeRecord aiRechargeRecord);

    public int insertAiRechargeRecord(AiRechargeRecord aiRechargeRecord);
}
