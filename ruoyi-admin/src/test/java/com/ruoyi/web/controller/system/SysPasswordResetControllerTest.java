package com.ruoyi.web.controller.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ruoyi.common.core.domain.model.PasswordResetBody;
import com.ruoyi.common.core.domain.model.PasswordResetEmailCodeBody;
import com.ruoyi.framework.web.service.SysPasswordResetService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 密码重置接口测试。
 */
@ExtendWith(MockitoExtension.class)
class SysPasswordResetControllerTest
{
    @Mock
    private SysPasswordResetService passwordResetService;

    @InjectMocks
    private SysPasswordResetController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void statusShouldReturnPublicPasswordResetConfiguration() throws Exception
    {
        when(passwordResetService.isResetEnabled()).thenReturn(true);
        when(passwordResetService.isCaptchaEnabled()).thenReturn(true);
        when(passwordResetService.getPasswordChrtype()).thenReturn("3");

        mockMvc.perform(get("/password/reset/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.resetEnabled").value(true))
                .andExpect(jsonPath("$.captchaEnabled").value(true))
                .andExpect(jsonPath("$.pwdChrtype").value("3"));
    }

    @Test
    void sendEmailCodeShouldDeserializeRealJsonRequest() throws Exception
    {
        String json = "{\"username\":\"testUser\",\"email\":\"user@example.com\","
                + "\"code\":\"ABCD\",\"uuid\":\"captcha-uuid\"}";

        mockMvc.perform(post("/password/reset/email/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PasswordResetEmailCodeBody> captor = ArgumentCaptor.forClass(PasswordResetEmailCodeBody.class);
        verify(passwordResetService).sendEmailCode(captor.capture());
        assertEquals("testUser", captor.getValue().getUsername());
        assertEquals("user@example.com", captor.getValue().getEmail());
        assertEquals("ABCD", captor.getValue().getCode());
        assertEquals("captcha-uuid", captor.getValue().getUuid());
    }

    @Test
    void resetPasswordShouldDeserializeRealJsonRequest() throws Exception
    {
        String json = "{\"username\":\"testUser\",\"email\":\"user@example.com\","
                + "\"emailCode\":\"123456\",\"newPassword\":\"Password1\"}";

        mockMvc.perform(post("/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<PasswordResetBody> captor = ArgumentCaptor.forClass(PasswordResetBody.class);
        verify(passwordResetService).resetPassword(captor.capture());
        assertEquals("testUser", captor.getValue().getUsername());
        assertEquals("user@example.com", captor.getValue().getEmail());
        assertEquals("123456", captor.getValue().getEmailCode());
        assertEquals("Password1", captor.getValue().getNewPassword());
    }
}
