package com.ruoyi.web.facade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.invite.domain.AiInviteConstants;
import com.ruoyi.invite.domain.InviteConfigUpdateRequest;
import com.ruoyi.invite.domain.vo.InviteConfigVO;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.ISysConfigService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InviteFacadeConfigTest
{
    @Test
    void shouldStrictlyReadInviteConfig()
    {
        SysConfigMapper configMapper = mock(SysConfigMapper.class);
        when(configMapper.selectConfigListByKeys(anyList())).thenReturn(configRecords());
        InviteFacade facade = createFacade(configMapper, mock(ISysConfigService.class));

        InviteConfigVO config = facade.getAdminConfig();

        assertTrue(config.isEnabled());
        assertEquals(new BigDecimal("5.5"), config.getRebateRate());
        assertEquals(24, config.getFreezeHours());
        assertEquals(365, config.getDurationDays());
        assertEquals(new BigDecimal("100.25"), config.getPerInviteeCap());
    }

    @Test
    void shouldRejectMissingOrDuplicateConfigBeforeUpdate()
    {
        SysConfigMapper missingMapper = mock(SysConfigMapper.class);
        List<SysConfig> missingRecords = configRecords();
        missingRecords.remove(missingRecords.size() - 1);
        when(missingMapper.selectConfigListByKeys(anyList())).thenReturn(missingRecords);
        InviteFacade missingFacade = createFacade(missingMapper, mock(ISysConfigService.class));

        ServiceException missingError = assertThrows(ServiceException.class, missingFacade::getAdminConfig);
        assertTrue(missingError.getMessage().contains(AiInviteConstants.CONFIG_INVITE_PER_INVITEE_CAP));
        assertTrue(missingError.getMessage().contains("缺失"));

        SysConfigMapper duplicateMapper = mock(SysConfigMapper.class);
        List<SysConfig> duplicateRecords = configRecords();
        duplicateRecords.add(config(99L, AiInviteConstants.CONFIG_INVITE_ENABLED, "false"));
        when(duplicateMapper.selectConfigListByKeys(anyList())).thenReturn(duplicateRecords);
        InviteFacade duplicateFacade = createFacade(duplicateMapper, mock(ISysConfigService.class));

        ServiceException duplicateError = assertThrows(ServiceException.class, duplicateFacade::getAdminConfig);
        assertTrue(duplicateError.getMessage().contains(AiInviteConstants.CONFIG_INVITE_ENABLED));
        assertTrue(duplicateError.getMessage().contains("重复2条"));
        verify(duplicateMapper, never()).updateConfig(any(SysConfig.class));
    }

    @Test
    void shouldUpdateAllConfigValuesAndRefreshCache()
    {
        SysConfigMapper configMapper = mock(SysConfigMapper.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        when(configMapper.selectConfigListByKeys(anyList())).thenReturn(configRecords());
        when(configMapper.updateConfig(any(SysConfig.class))).thenReturn(1);
        InviteFacade facade = createFacade(configMapper, configService);
        InviteConfigUpdateRequest request = request(false, "8.75", 48, 730, "250.125");

        TransactionSynchronizationManager.initSynchronization();
        InviteConfigVO result;
        try
        {
            result = facade.updateAdminConfig(request, "admin");
            verify(configService, never()).resetConfigCache();
            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());
            synchronizations.get(0).afterCommit();
        }
        finally
        {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<SysConfig> captor = ArgumentCaptor.forClass(SysConfig.class);
        verify(configMapper, org.mockito.Mockito.times(5)).updateConfig(captor.capture());
        Map<String, String> values = captor.getAllValues().stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
        assertEquals("false", values.get(AiInviteConstants.CONFIG_INVITE_ENABLED));
        assertEquals("8.75", values.get(AiInviteConstants.CONFIG_INVITE_REBATE_RATE));
        assertEquals("48", values.get(AiInviteConstants.CONFIG_INVITE_FREEZE_HOURS));
        assertEquals("730", values.get(AiInviteConstants.CONFIG_INVITE_DURATION_DAYS));
        assertEquals("250.125", values.get(AiInviteConstants.CONFIG_INVITE_PER_INVITEE_CAP));
        assertTrue(captor.getAllValues().stream().allMatch(config -> "admin".equals(config.getUpdateBy())));
        verify(configService).resetConfigCache();
        assertEquals(new BigDecimal("8.75"), result.getRebateRate());
    }

    @Test
    void shouldNotRefreshCacheWhenAnyUpdateFails()
    {
        SysConfigMapper configMapper = mock(SysConfigMapper.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        when(configMapper.selectConfigListByKeys(anyList())).thenReturn(configRecords());
        when(configMapper.updateConfig(any(SysConfig.class))).thenReturn(1, 1, 0);
        InviteFacade facade = createFacade(configMapper, configService);
        InviteConfigUpdateRequest request = request(true, "10", 12, 30, "50");

        assertThrows(ServiceException.class, () -> facade.updateAdminConfig(request, "admin"));

        verify(configMapper, org.mockito.Mockito.times(3)).updateConfig(any(SysConfig.class));
        verify(configService, never()).resetConfigCache();
    }

    @Test
    void shouldValidateRequestBeforeReadingOrUpdatingConfig()
    {
        SysConfigMapper configMapper = mock(SysConfigMapper.class);
        InviteFacade facade = createFacade(configMapper, mock(ISysConfigService.class));
        InviteConfigUpdateRequest request = request(true, "101", 0, 0, "0");

        assertThrows(ServiceException.class, () -> facade.updateAdminConfig(request, "admin"));

        verify(configMapper, never()).selectConfigListByKeys(anyList());
        verify(configMapper, never()).updateConfig(any(SysConfig.class));
    }

    private InviteFacade createFacade(SysConfigMapper configMapper, ISysConfigService configService)
    {
        InviteFacade facade = new InviteFacade();
        ReflectionTestUtils.setField(facade, "configMapper", configMapper);
        ReflectionTestUtils.setField(facade, "configService", configService);
        return facade;
    }

    private List<SysConfig> configRecords()
    {
        List<SysConfig> records = new ArrayList<>();
        records.add(config(105L, AiInviteConstants.CONFIG_INVITE_ENABLED, "true"));
        records.add(config(106L, AiInviteConstants.CONFIG_INVITE_REBATE_RATE, "5.5"));
        records.add(config(107L, AiInviteConstants.CONFIG_INVITE_FREEZE_HOURS, "24"));
        records.add(config(108L, AiInviteConstants.CONFIG_INVITE_DURATION_DAYS, "365"));
        records.add(config(109L, AiInviteConstants.CONFIG_INVITE_PER_INVITEE_CAP, "100.25"));
        return records;
    }

    private SysConfig config(Long id, String key, String value)
    {
        SysConfig config = new SysConfig();
        config.setConfigId(id);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigName(key);
        config.setConfigType("Y");
        return config;
    }

    private InviteConfigUpdateRequest request(boolean enabled, String rebateRate, int freezeHours,
                                                int durationDays, String perInviteeCap)
    {
        InviteConfigUpdateRequest request = new InviteConfigUpdateRequest();
        request.setEnabled(enabled);
        request.setRebateRate(new BigDecimal(rebateRate));
        request.setFreezeHours(freezeHours);
        request.setDurationDays(durationDays);
        request.setPerInviteeCap(new BigDecimal(perInviteeCap));
        return request;
    }
}
