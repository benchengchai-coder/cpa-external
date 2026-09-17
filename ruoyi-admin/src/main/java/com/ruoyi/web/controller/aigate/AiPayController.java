package com.ruoyi.web.controller.aigate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.web.facade.PayFacade;

/**
 * 支付宝支付
 */
@RestController
@RequestMapping("/aigate/pay")
public class AiPayController extends BaseController
{
    @Autowired
    private PayFacade payFacade;

    /**
     * 查询在线充值业务状态。
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/online-recharge/status")
    public AjaxResult getOnlineRechargeStatus()
    {
        return success(payFacade.getOnlineRechargeStatus());
    }

    /**
     * 创建支付宝充值订单，返回电脑网站支付表单。
     * 前端拿到 payForm 后写入 document 跳转支付宝收银台。
     */
    @PreAuthorize("isAuthenticated()")
    @Log(title = "支付宝充值下单", businessType = BusinessType.INSERT)
    @PostMapping("/alipay/create")
    public AjaxResult createOrder(@RequestBody Map<String, Object> params)
    {
        Object amountValue = params.get("amount");
        if (amountValue == null)
        {
            return error("充值金额不能为空");
        }
        BigDecimal amount;
        try
        {
            amount = new BigDecimal(amountValue.toString());
        }
        catch (NumberFormatException e)
        {
            return error("充值金额格式不正确");
        }
        Map<String, Object> result = payFacade.createOrder(getUserId(), getUsername(), amount);
        return success(result);
    }

    /**
     * 查询当前用户待支付的支付宝充值订单。
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/alipay/pending")
    public AjaxResult getPendingOrder()
    {
        return success(payFacade.getPendingOrder(getUserId()));
    }

    /**
     * 重新打开待支付订单的支付宝付款页面。
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/alipay/reopen/{outTradeNo}")
    public AjaxResult reopenOrder(@PathVariable String outTradeNo)
    {
        return success(payFacade.reopenOrder(getUserId(), outTradeNo));
    }

    /**
     * 取消待支付订单。
     */
    @PreAuthorize("isAuthenticated()")
    @Log(title = "取消支付宝充值订单", businessType = BusinessType.UPDATE)
    @PostMapping("/alipay/cancel/{outTradeNo}")
    public AjaxResult cancelOrder(@PathVariable String outTradeNo)
    {
        payFacade.cancelOrder(getUserId(), outTradeNo);
        return success();
    }

    /**
     * 支付宝异步通知回调。
     * 支付宝服务器以 application/x-www-form-urlencoded 形式 POST，
     * 返回字符串 "success" 表示处理成功，支付宝不再重试；其它视为失败会重试。
     */
    @PostMapping("/alipay/notify")
    public String notify(HttpServletRequest request)
    {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet())
        {
            String[] values = entry.getValue();
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 0; i < values.length; i++)
            {
                if (i > 0)
                {
                    valueBuilder.append(",");
                }
                valueBuilder.append(values[i]);
            }
            params.put(entry.getKey(), valueBuilder.toString());
        }
        return payFacade.handleNotify(params);
    }

    /**
     * 查询订单支付状态（前端结果页轮询）。
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/order/status/{outTradeNo}")
    public AjaxResult queryOrderStatus(@PathVariable String outTradeNo)
    {
        if (outTradeNo == null || outTradeNo.isEmpty())
        {
            return error("订单号不能为空");
        }
        return success(payFacade.queryOrderStatus(getUserId(), outTradeNo));
    }
}

