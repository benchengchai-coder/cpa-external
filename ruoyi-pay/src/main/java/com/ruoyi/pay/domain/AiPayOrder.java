package com.ruoyi.pay.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 支付宝支付订单对象 ai_pay_order
 */
public class AiPayOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long orderId;

    /** 商户订单号（全局唯一） */
    private String outTradeNo;

    private Long userId;

    private String username;

    /** 支付金额（美元，等同人民币） */
    private BigDecimal totalAmount;

    /** 订单标题 */
    private String subject;

    /** 支付宝交易号（回调回填） */
    private String tradeNo;

    /** 状态（0待支付 1已支付 2已关闭 3已退款） */
    private String status;

    /** 支付成功时间 */
    private Date payTime;

    /** 收到异步通知时间 */
    private Date notifyTime;

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public String getOutTradeNo()
    {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo)
    {
        this.outTradeNo = outTradeNo;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getTradeNo()
    {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo)
    {
        this.tradeNo = tradeNo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getPayTime()
    {
        return payTime;
    }

    public void setPayTime(Date payTime)
    {
        this.payTime = payTime;
    }

    public Date getNotifyTime()
    {
        return notifyTime;
    }

    public void setNotifyTime(Date notifyTime)
    {
        this.notifyTime = notifyTime;
    }
}
