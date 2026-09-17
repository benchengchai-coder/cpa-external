package com.ruoyi.checkin.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.checkin.domain.AiCheckinRecord;
import com.ruoyi.checkin.domain.CheckinResult;
import com.ruoyi.checkin.domain.vo.CheckinCalendarVO;
import com.ruoyi.checkin.mapper.AiCheckinRecordMapper;
import com.ruoyi.checkin.service.IAiCheckinService;
import com.ruoyi.common.exception.ServiceException;

/**
 * 每日签到 服务实现（纯签到域）。
 * <p>
 * 只操作 ai_checkin_record 表；加余额、回填余额快照、写账户流水 type=5
 * 由聚合层（CheckinFacade）编排。
 */
@Service
public class AiCheckinServiceImpl implements IAiCheckinService {
    private static final BigDecimal SUBSCRIBER_REWARD = new BigDecimal("0.2000");

    private static final BigDecimal FREE_REWARD = new BigDecimal("0.1000");

    @Autowired
    private AiCheckinRecordMapper checkinRecordMapper;

    @Override
    public CheckinCalendarVO getCalendar(Long userId, YearMonth month) {
        if (userId == null) {
            throw new ServiceException("用户不能为空");
        }
        YearMonth queryMonth = month == null ? YearMonth.now() : month;
        LocalDate startDate = queryMonth.atDay(1);
        LocalDate endDate = queryMonth.plusMonths(1).atDay(1);
        List<String> signedDates = checkinRecordMapper.selectSignedDatesByMonth(userId, startDate, endDate);
        LocalDate today = LocalDate.now();

        CheckinCalendarVO vo = new CheckinCalendarVO();
        vo.setMonth(queryMonth.toString());
        vo.setToday(today.toString());
        vo.setSignedDates(signedDates);
        vo.setTodaySigned(checkinRecordMapper.selectByUserAndDate(userId, today) != null);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinResult checkin(Long userId, String username, LocalDate requestDate, boolean hasActiveSubscription) {
        if (userId == null) {
            throw new ServiceException("用户不能为空");
        }
        LocalDate today = LocalDate.now();
        if (requestDate != null && !today.equals(requestDate)) {
            throw new ServiceException("只能签到今天");
        }
        if (checkinRecordMapper.selectByUserAndDate(userId, today) != null) {
            throw new ServiceException("今天已经签到，请明天再来");
        }

        BigDecimal reward = fixedReward(hasActiveSubscription);
        AiCheckinRecord record = new AiCheckinRecord();
        record.setUserId(userId);
        record.setUsername(username);
        record.setCheckinDate(today);
        record.setRewardAmount(reward);
        record.setStatus("0");
        record.setCreateBy(username);
        record.setRemark("每日签到奖励");

        try {
            checkinRecordMapper.insertCheckinRecord(record);
        } catch (DuplicateKeyException e) {
            throw new ServiceException("今天已经签到，请明天再来");
        }

        // 余额快照与账户流水由聚合层在加余额后回填/写入
        YearMonth currentMonth = YearMonth.from(today);
        List<String> signedDates = checkinRecordMapper.selectSignedDatesByMonth(
                userId, currentMonth.atDay(1), currentMonth.plusMonths(1).atDay(1));

        return new CheckinResult(record.getCheckinId(), reward, signedDates);
    }

    @Override
    public void updateBalanceAfter(Long checkinId, BigDecimal balanceAfter)
    {
        if (checkinId == null)
        {
            return;
        }
        checkinRecordMapper.updateBalanceAfter(checkinId, balanceAfter);
    }

    private BigDecimal fixedReward(boolean hasActiveSubscription) {
        return hasActiveSubscription ? SUBSCRIBER_REWARD : FREE_REWARD;
    }
}
