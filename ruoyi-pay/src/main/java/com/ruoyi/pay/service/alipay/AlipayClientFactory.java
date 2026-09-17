package com.ruoyi.pay.service.alipay;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.pay.config.AliPayProperties;

/**
 * 支付宝客户端工厂。
 * 按 {@link AliPayProperties} 构建全局单例 {@link AlipayClient}，
 * 支持沙箱/正式环境切换。支付参数不完整时不初始化，避免启动失败。
 */
@Component
public class AlipayClientFactory
{
    private static final Logger log = LoggerFactory.getLogger(AlipayClientFactory.class);

    /** 支付宝正式网关 */
    private static final String GATEWAY_PROD = "https://openapi.alipay.com/gateway.do";

    /** 支付宝沙箱网关 */
    private static final String GATEWAY_SANDBOX = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    @Autowired
    private AliPayProperties properties;

    private volatile AlipayClient client;

    @PostConstruct
    public void init()
    {
        if (!isConfigComplete())
        {
            log.warn("支付宝支付参数不完整，跳过 AlipayClient 初始化");
            return;
        }
        try
        {
            String serverUrl = properties.isSandbox() ? GATEWAY_SANDBOX : GATEWAY_PROD;
            AlipayConfig config = new AlipayConfig();
            config.setServerUrl(serverUrl);
            config.setAppId(properties.getAppId());
            config.setPrivateKey(properties.getAppPrivateKey());
            config.setAlipayPublicKey(properties.getAlipayPublicKey());
            config.setSignType(properties.getSignType());
            config.setCharset(properties.getCharset());
            config.setFormat(properties.getFormat());
            this.client = new DefaultAlipayClient(config);
            log.info("AlipayClient 初始化成功，环境：{}", properties.isSandbox() ? "沙箱" : "正式");
        }
        catch (Exception e)
        {
            // 启动期不中断应用，真正调用时再校验 client 是否就绪
            log.error("AlipayClient 初始化失败，在线充值功能将不可用", e);
        }
    }

    /**
     * 获取已初始化的 AlipayClient。
     *
     * @return AlipayClient 实例
     * @throws IllegalStateException 当功能未启用或初始化失败时抛出
     */
    public AlipayClient getClient()
    {
        if (client == null)
        {
            throw new IllegalStateException("支付宝支付参数不完整或 AlipayClient 初始化失败");
        }
        return client;
    }

    /**
     * 功能是否就绪可用。
     */
    public boolean isReady()
    {
        return client != null;
    }

    private boolean isConfigComplete()
    {
        return StringUtils.isNotEmpty(properties.getAppId())
                && StringUtils.isNotEmpty(properties.getAppPrivateKey())
                && StringUtils.isNotEmpty(properties.getAlipayPublicKey())
                && StringUtils.isNotEmpty(properties.getNotifyUrl())
                && StringUtils.isNotEmpty(properties.getReturnUrl());
    }
}
