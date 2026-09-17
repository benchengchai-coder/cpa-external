package com.ruoyi.web.controller.aigate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.checkin.domain.vo.CheckinResultVO;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.web.facade.CheckinFacade;

/**
 * 每日签到 Controller
 */
@RestController
@RequestMapping("/aigate/checkin")
public class AiCheckinController extends BaseController
{
    @Autowired
    private CheckinFacade checkinFacade;

    /**
     * 查询签到日历。
     */
    @GetMapping("/calendar")
    public AjaxResult calendar(@RequestParam(required = false) String month)
    {
        return success(checkinFacade.getCalendar(getUserId(), parseMonth(month)));
    }

    /**
     * 今日签到。
     */
    @PostMapping("/daily")
    public AjaxResult daily(@RequestBody(required = false) Map<String, String> params)
    {
        LocalDate requestDate = null;
        if (params != null && params.get("checkinDate") != null && !params.get("checkinDate").isEmpty())
        {
            requestDate = parseDate(params.get("checkinDate"));
        }
        CheckinResultVO result = checkinFacade.checkin(getUserId(), getUsername(), requestDate);
        return AjaxResult.success("签到成功，获得 " + result.getRewardAmount() + " 美元", result);
    }

    private YearMonth parseMonth(String month)
    {
        if (month == null || month.isEmpty())
        {
            return YearMonth.now();
        }
        try
        {
            return YearMonth.parse(month);
        }
        catch (DateTimeParseException e)
        {
            throw new ServiceException("月份格式错误");
        }
    }

    private LocalDate parseDate(String date)
    {
        try
        {
            return LocalDate.parse(date);
        }
        catch (DateTimeParseException e)
        {
            throw new ServiceException("日期格式错误");
        }
    }
}

