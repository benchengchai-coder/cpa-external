package com.ruoyi.checkin.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.checkin.domain.CheckinResult;
import com.ruoyi.checkin.mapper.AiCheckinRecordMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiCheckinServiceImplTest
{
    private AiCheckinRecordMapper checkinRecordMapper;

    @BeforeEach
    void setUp()
    {
        checkinRecordMapper = mock(AiCheckinRecordMapper.class);
        when(checkinRecordMapper.selectByUserAndDate(any(Long.class), any(LocalDate.class))).thenReturn(null);
        when(checkinRecordMapper.selectSignedDatesByMonth(any(Long.class), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
    }

    @Test
    void shouldUseFixedSubscriberReward()
    {
        assertEquals(new BigDecimal("0.2000"), checkin(true).getRewardAmount());
    }

    @Test
    void shouldUseFixedFreeReward()
    {
        assertEquals(new BigDecimal("0.1000"), checkin(false).getRewardAmount());
    }

    private CheckinResult checkin(boolean hasActiveSubscription)
    {
        AiCheckinServiceImpl checkinService = new AiCheckinServiceImpl();
        ReflectionTestUtils.setField(checkinService, "checkinRecordMapper", checkinRecordMapper);
        return checkinService.checkin(1L, "tester", LocalDate.now(), hasActiveSubscription);
    }
}
