package com.ruoyi.web.facade;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.pay.domain.AiPayConstants;
import com.ruoyi.pay.service.IAiPayService;
import com.ruoyi.system.service.ISysConfigService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayFacadeOnlineRechargeConfigTest
{
    @Test
    void shouldReadOnlineRechargeSwitchFromSysConfig()
    {
        IAiPayService payService = mock(IAiPayService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        when(configService.selectConfigByKey(AiPayConstants.CONFIG_ONLINE_RECHARGE_ENABLED))
                .thenReturn("true");
        when(payService.isOnlineRechargeAvailable()).thenReturn(true);
        PayFacade facade = createFacade(payService, configService);

        Map<String, Object> status = facade.getOnlineRechargeStatus();

        assertTrue((Boolean) status.get("enabled"));
        assertTrue((Boolean) status.get("available"));
    }

    @Test
    void shouldFailClosedWhenOnlineRechargeConfigIsMissingOrInvalid()
    {
        IAiPayService payService = mock(IAiPayService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        when(configService.selectConfigByKey(AiPayConstants.CONFIG_ONLINE_RECHARGE_ENABLED))
                .thenReturn("invalid");
        when(payService.isOnlineRechargeAvailable()).thenReturn(true);
        PayFacade facade = createFacade(payService, configService);

        Map<String, Object> status = facade.getOnlineRechargeStatus();

        assertFalse((Boolean) status.get("enabled"));
        assertFalse((Boolean) status.get("available"));
    }

    @Test
    void shouldRejectNewOrderWhenOnlineRechargeIsDisabled()
    {
        IAiPayService payService = mock(IAiPayService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        when(configService.selectConfigByKey(AiPayConstants.CONFIG_ONLINE_RECHARGE_ENABLED))
                .thenReturn("false");
        PayFacade facade = createFacade(payService, configService);

        ServiceException error = assertThrows(ServiceException.class,
                () -> facade.createOrder(1L, "user", BigDecimal.TEN));

        assertEquals("在线充值服务未开放", error.getMessage());
        verify(payService, never()).createOrder(anyLong(), anyString(), any(BigDecimal.class));
    }

    @Test
    void shouldCreateOrderImmediatelyAfterOnlineRechargeIsEnabled()
    {
        IAiPayService payService = mock(IAiPayService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        when(configService.selectConfigByKey(AiPayConstants.CONFIG_ONLINE_RECHARGE_ENABLED))
                .thenReturn("true");
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("outTradeNo", "PAY001");
        when(payService.createOrder(1L, "user", BigDecimal.TEN)).thenReturn(expected);
        PayFacade facade = createFacade(payService, configService);

        Map<String, Object> result = facade.createOrder(1L, "user", BigDecimal.TEN);

        assertSame(expected, result);
        verify(payService).createOrder(1L, "user", BigDecimal.TEN);
    }

    private PayFacade createFacade(IAiPayService payService, ISysConfigService configService)
    {
        PayFacade facade = new PayFacade();
        ReflectionTestUtils.setField(facade, "payService", payService);
        ReflectionTestUtils.setField(facade, "configService", configService);
        return facade;
    }
}
