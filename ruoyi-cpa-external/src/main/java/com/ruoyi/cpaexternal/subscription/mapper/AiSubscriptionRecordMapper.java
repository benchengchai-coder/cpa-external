package com.ruoyi.cpaexternal.subscription.mapper;

import java.util.List;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionRecord;

/**
 * AI订阅流水 数据层
 */
public interface AiSubscriptionRecordMapper
{
    public List<AiSubscriptionRecord> selectAiSubscriptionRecordList(AiSubscriptionRecord aiSubscriptionRecord);

    public int insertAiSubscriptionRecord(AiSubscriptionRecord aiSubscriptionRecord);
}
