package com.ruoyi.web.controller.system;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeReadService;
import com.ruoyi.system.service.ISysNoticeService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通知公告接口测试。
 */
@ExtendWith(MockitoExtension.class)
class SysNoticeControllerTest
{
    private static final Long USER_ID = 100L;

    @Mock
    private ISysNoticeService noticeService;

    @Mock
    private ISysNoticeReadService noticeReadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        SysNoticeController controller = new TestSysNoticeController();
        ReflectionTestUtils.setField(controller, "noticeService", noticeService);
        ReflectionTestUtils.setField(controller, "noticeReadService", noticeReadService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void unreadCountShouldReturnCurrentUserUnreadCount() throws Exception
    {
        when(noticeReadService.selectUnreadCount(USER_ID)).thenReturn(3);

        mockMvc.perform(get("/system/notice/unreadCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));

        verify(noticeReadService).selectUnreadCount(USER_ID);
    }

    @Test
    void unreadCountShouldReturnZeroWhenAllNoticesAreRead() throws Exception
    {
        when(noticeReadService.selectUnreadCount(USER_ID)).thenReturn(0);

        mockMvc.perform(get("/system/notice/unreadCount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void unreadListShouldReturnCurrentUserUnreadNoticesInMapperOrder() throws Exception
    {
        SysNotice latestNotice = new SysNotice();
        latestNotice.setNoticeId(12L);
        latestNotice.setNoticeTitle("最新通知");
        latestNotice.setNoticeContent("最新通知内容");
        SysNotice earlierNotice = new SysNotice();
        earlierNotice.setNoticeId(11L);
        earlierNotice.setNoticeTitle("较早通知");
        earlierNotice.setNoticeContent("较早通知内容");
        when(noticeReadService.selectUnreadNoticeList(USER_ID))
                .thenReturn(List.of(latestNotice, earlierNotice));

        mockMvc.perform(get("/system/notice/unreadList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].noticeId").value(12))
                .andExpect(jsonPath("$.data[0].noticeTitle").value("最新通知"))
                .andExpect(jsonPath("$.data[0].noticeContent").value("最新通知内容"))
                .andExpect(jsonPath("$.data[1].noticeId").value(11));

        verify(noticeReadService).selectUnreadNoticeList(USER_ID);
    }

    @Test
    void listTopShouldReturnTotalUnreadCountInsteadOfOnlyTopFiveUnreadCount() throws Exception
    {
        when(noticeReadService.selectNoticeListWithReadStatus(USER_ID, 5)).thenReturn(List.of());
        when(noticeReadService.selectUnreadCount(USER_ID)).thenReturn(8);

        mockMvc.perform(get("/system/notice/listTop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.unreadCount").value(8));

        verify(noticeReadService).selectUnreadCount(USER_ID);
    }

    private static class TestSysNoticeController extends SysNoticeController
    {
        @Override
        public Long getUserId()
        {
            return USER_ID;
        }
    }
}
