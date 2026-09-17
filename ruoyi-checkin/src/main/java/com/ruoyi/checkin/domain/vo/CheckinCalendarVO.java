package com.ruoyi.checkin.domain.vo;

import java.util.List;

/**
 * 签到日历视图对象。
 */
public class CheckinCalendarVO
{
    /** 签到功能是否开启 */
    private boolean enabled;

    /** 当前月份（yyyy-MM） */
    private String month;

    /** 今天（yyyy-MM-dd） */
    private String today;

    /** 当前月份已签到日期列表（yyyy-MM-dd） */
    private List<String> signedDates;

    /** 今天是否已签到 */
    private Boolean todaySigned;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getMonth()
    {
        return month;
    }

    public void setMonth(String month)
    {
        this.month = month;
    }

    public String getToday()
    {
        return today;
    }

    public void setToday(String today)
    {
        this.today = today;
    }

    public List<String> getSignedDates()
    {
        return signedDates;
    }

    public void setSignedDates(List<String> signedDates)
    {
        this.signedDates = signedDates;
    }

    public Boolean getTodaySigned()
    {
        return todaySigned;
    }

    public void setTodaySigned(Boolean todaySigned)
    {
        this.todaySigned = todaySigned;
    }
}
