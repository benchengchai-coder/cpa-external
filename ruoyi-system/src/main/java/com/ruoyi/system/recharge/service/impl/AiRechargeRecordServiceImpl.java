package com.ruoyi.system.recharge.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;
import com.ruoyi.system.recharge.mapper.AiRechargeRecordMapper;
import com.ruoyi.system.recharge.service.IAiRechargeRecordService;

/**
 * AI充值记录 服务层实现
 */
@Service
public class AiRechargeRecordServiceImpl implements IAiRechargeRecordService
{
    @Autowired
    private AiRechargeRecordMapper aiRechargeRecordMapper;

    @Override
    public List<AiRechargeRecord> selectAiRechargeRecordList(AiRechargeRecord aiRechargeRecord)
    {
        return aiRechargeRecordMapper.selectAiRechargeRecordList(aiRechargeRecord);
    }

    @Override
    public int insertAiRechargeRecord(AiRechargeRecord aiRechargeRecord)
    {
        return aiRechargeRecordMapper.insertAiRechargeRecord(aiRechargeRecord);
    }
}
