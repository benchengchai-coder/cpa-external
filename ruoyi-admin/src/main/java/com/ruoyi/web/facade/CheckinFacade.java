package com.ruoyi.web.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.checkin.domain.CheckinResult;
import com.ruoyi.checkin.domain.vo.CheckinCalendarVO;
import com.ruoyi.checkin.domain.vo.CheckinResultVO;
import com.ruoyi.checkin.service.IAiCheckinService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.recharge.domain.AiRechargeConstants;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;
import com.ruoyi.system.recharge.service.IAiRechargeRecordService;
import com.ruoyi.system.service.ISysConfigService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 每日签到服务编排 Facade（聚合层）。
 * <p>
 * 串联「纯签到域」与「用户余额 / 账户流水」：
 * {@link #checkin} 编排：纯域签到 → 加余额 → 回填余额快照 → 写账户流水 type=5。
 */
@Component
public class CheckinFacade
{
    private static final String CONFIG_CHECKIN_ENABLED = "ai.checkin.enabled";

    @Autowired
    private IAiCheckinService checkinService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private IAiRechargeRecordService rechargeRecordService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 查询签到开关与日历，关闭时不再查询签到记录。
     */
    public CheckinCalendarVO getCalendar(Long userId, YearMonth month)
    {
        boolean enabled = isCheckinEnabled();
        CheckinCalendarVO calendar;
        if (enabled)
        {
            calendar = checkinService.getCalendar(userId, month);
        }
        else
        {
            LocalDate today = LocalDate.now();
            calendar = new CheckinCalendarVO();
            calendar.setMonth((month == null ? YearMonth.from(today) : month).toString());
            calendar.setToday(today.toString());
            calendar.setSignedDates(List.of());
            calendar.setTodaySigned(false);
        }
        calendar.setEnabled(enabled);
        return calendar;
    }

    /**
     * 今日签到（编排：纯域签到 → 加余额 → 回填快照 → 写账户流水）。
     */
    @Transactional(rollbackFor = Exception.class)
    public CheckinResultVO checkin(Long userId, String username, LocalDate requestDate)
    {
        if (!isCheckinEnabled())
        {
            throw new ServiceException("签到功能未开放");
        }

        // 1. 执行纯域签到（不加余额、不写账户流水）
        CheckinResult result = checkinService.checkin(userId, username, requestDate, false);

        // 2. 加余额
        int rows = sysUserMapper.addUserBalance(userId, result.getRewardAmount());
        if (rows == 0)
        {
            throw new ServiceException("用户不存在");
        }

        // 3. 读真实余额快照并回填签到记录（保留审计信息）
        SysUser user = sysUserMapper.selectUserById(userId);
        BigDecimal balanceAfter = user == null ? null : user.getBalance();
        checkinService.updateBalanceAfter(result.getCheckinId(), balanceAfter);

        // 4. 写账户流水 type=5 每日签到
        AiRechargeRecord rechargeRecord = new AiRechargeRecord();
        rechargeRecord.setUserId(userId);
        rechargeRecord.setUsername(username);
        rechargeRecord.setType(AiRechargeConstants.TYPE_DAILY_CHECKIN);
        rechargeRecord.setAmount(result.getRewardAmount());
        rechargeRecord.setSourceId(result.getCheckinId());
        rechargeRecord.setSourceName("每日签到");
        rechargeRecord.setStatus("0");
        rechargeRecord.setCreateBy(username);
        rechargeRecord.setRemark("每日签到奖励 " + result.getRewardAmount() + " 美元");
        rechargeRecordService.insertAiRechargeRecord(rechargeRecord);

        // 5. 组装前端 VO
        CheckinResultVO vo = new CheckinResultVO();
        vo.setCheckinDate(LocalDate.now().toString());
        vo.setRewardAmount(result.getRewardAmount());
        vo.setBalance(balanceAfter);
        vo.setSignedDates(result.getSignedDates());
        vo.setTodaySigned(true);
        return vo;
    }

    private boolean isCheckinEnabled()
    {
        String value = configService.selectConfigByKey(CONFIG_CHECKIN_ENABLED);
        // 未配置时保持原有签到行为，已有部署可在参数设置中直接启停。
        return StringUtils.isBlank(value) || Boolean.parseBoolean(value.trim());
    }
}
