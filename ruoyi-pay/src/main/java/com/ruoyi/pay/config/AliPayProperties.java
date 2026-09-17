package com.ruoyi.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝在线充值相关配置。
 * 对应 application.yml 中 cpa.pay.alipay 前缀。
 */
@Component
@ConfigurationProperties(prefix = "cpa.pay.alipay")
public class AliPayProperties
{
    /**
     * 是否沙箱环境。
     * true 时使用支付宝沙箱网关（openapi-sandbox.dl.alipaydev.com）。
     */
    private boolean sandbox = false;

    /**
     * 支付宝应用 ID（appId）。
     */
    private String appId = "";

    /**
     * 应用私钥（PKCS8 格式，去掉头尾与换行）。
     * 生产环境必须通过环境变量 ALIPAY_APP_PRIVATE_KEY 覆盖。
     */
    private String appPrivateKey = "";

    /**
     * 支付宝公钥（用于异步通知验签）。
     * 生产环境必须通过环境变量 ALIPAY_PUBLIC_KEY 覆盖。
     */
    private String alipayPublicKey = "";

    /**
     * 签名算法类型，默认 RSA2。
     */
    private String signType = "RSA2";

    /**
     * 字符集，默认 UTF-8。
     */
    private String charset = "UTF-8";

    /**
     * 数据格式，默认 json。
     */
    private String format = "json";

    /**
     * 异步通知地址（notify_url）。
     * 必须为外网可访问的完整 URL，支付宝服务器将向该地址 POST 支付结果。
     */
    private String notifyUrl = "";

    /**
     * 同步回跳地址（return_url）。
     * 用户支付完成后浏览器将跳转到该地址（前端支付结果页）。
     */
    private String returnUrl = "";

    /**
     * 订单超时关闭时间（分钟）。
     * 超过该时长仍未支付，定时补偿任务会主动查询并关闭订单。
     */
    private int orderExpireMinutes = 10;

    /**
     * 订单补偿扫描间隔（秒），定时任务据此扫描超时未回调的订单。
     */
    private int compensateIntervalSeconds = 60;

    public boolean isSandbox()
    {
        return sandbox;
    }

    public void setSandbox(boolean sandbox)
    {
        this.sandbox = sandbox;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getAppPrivateKey()
    {
        return appPrivateKey;
    }

    public void setAppPrivateKey(String appPrivateKey)
    {
        this.appPrivateKey = appPrivateKey;
    }

    public String getAlipayPublicKey()
    {
        return alipayPublicKey;
    }

    public void setAlipayPublicKey(String alipayPublicKey)
    {
        this.alipayPublicKey = alipayPublicKey;
    }

    public String getSignType()
    {
        return signType;
    }

    public void setSignType(String signType)
    {
        this.signType = signType;
    }

    public String getCharset()
    {
        return charset;
    }

    public void setCharset(String charset)
    {
        this.charset = charset;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    public String getNotifyUrl()
    {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl)
    {
        this.notifyUrl = notifyUrl;
    }

    public String getReturnUrl()
    {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl)
    {
        this.returnUrl = returnUrl;
    }

    public int getOrderExpireMinutes()
    {
        return orderExpireMinutes;
    }

    public void setOrderExpireMinutes(int orderExpireMinutes)
    {
        this.orderExpireMinutes = orderExpireMinutes;
    }

    public int getCompensateIntervalSeconds()
    {
        return compensateIntervalSeconds;
    }

    public void setCompensateIntervalSeconds(int compensateIntervalSeconds)
    {
        this.compensateIntervalSeconds = compensateIntervalSeconds;
    }
}
