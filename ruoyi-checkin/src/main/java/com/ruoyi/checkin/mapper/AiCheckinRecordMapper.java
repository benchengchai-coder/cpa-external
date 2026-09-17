package com.ruoyi.checkin.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.checkin.domain.AiCheckinRecord;

/**
 * 每日签到记录 数据层
 */
public interface AiCheckinRecordMapper
{
    /**
     * 按用户和日期查询签到记录。
     */
    public AiCheckinRecord selectByUserAndDate(@Param("userId") Long userId, @Param("checkinDate") LocalDate checkinDate);

    /**
     * 查询用户指定月份已签到日期。
     */
    public List<String> selectSignedDatesByMonth(@Param("userId") Long userId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 新增签到记录。
     */
    public int insertCheckinRecord(AiCheckinRecord record);

    /**
     * 更新签到后的余额快照。
     */
    public int updateBalanceAfter(@Param("checkinId") Long checkinId, @Param("balanceAfter") BigDecimal balanceAfter);
}
