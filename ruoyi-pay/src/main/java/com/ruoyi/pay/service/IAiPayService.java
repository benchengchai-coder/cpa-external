package com.ruoyi.pay.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.ruoyi.pay.domain.AiPayOrder;
import com.ruoyi.pay.domain.PayResult;

/**
 * 支付宝支付 服务层（纯支付原语）。
 * <p>
 * 本接口仅依赖 ruoyi-common + alipay-sdk，只操作 ai_pay_order 表与支付宝 API；
 * 不操作用户余额、不写账户流水、不触发返利 —— 这些副作用由 PayFacade 编排。
 * <p>
 * 幂等核心：所有入账路径统一走 {@link #markOrderPaid} 的 CAS（0→1），
 * 仅当 CAS 成功（首次入账）时 {@link PayResult#isSettled()} 为 true，
 * PayFacade 据此决定是否触发加余额/流水/返利。
 */
public interface IAiPayService
{
    /**
     * 在线充值当前是否具备下单条件。
     *
     * @return true 表示支付客户端已就绪
     */
    boolean isOnlineRechargeAvailable();

    /**
     * 创建支付宝订单并返回电脑网站支付表单。
     */
    Map<String, Object> createOrder(Long userId, String username, BigDecimal amount);

    /**
     * 查询当前用户最新一笔待支付订单。
     */
    Map<String, Object> getPendingOrder(Long userId);

    /**
     * 重新生成待支付订单的支付宝支付表单。
     * 订单超时时抛 ServiceException（不再内部补偿），由聚合层决定是否触发补偿。
     */
    Map<String, Object> reopenOrder(Long userId, String outTradeNo);

    /**
     * 取消当前用户的待支付订单。
     * 若取消时查到已支付，会执行 markOrderPaid，返回的 PayResult.settled=true 时
     * 由聚合层完成入账副作用。
     */
    PayResult cancelOrder(Long userId, String outTradeNo);

    /**
     * 处理支付宝异步通知（纯：验签 + 金额校验 + markOrderPaid）。
     *
     * @return [0]=应答支付宝的字符串（success/failure），[1]=入账结果（settled=true 时聚合层触发副作用）
     */
    NotifyOutcome handleNotify(Map<String, String> params);

    /**
     * 查询订单支付状态（纯：只返回订单字段，不含余额；余额由聚合层补充）。
     */
    Map<String, Object> queryOrderStatus(Long userId, String outTradeNo);

    /**
     * 扫描超时未回调的待支付订单（供聚合层定时补偿调用）。
     */
    List<AiPayOrder> scanExpiredPendingOrders();

    /**
     * 补偿单笔订单：查支付宝真实状态，已支付则 markOrderPaid（settled=true），
     * 否则关单。
     */
    PayResult compensateOrder(AiPayOrder order);

    /**
     * 异步通知处理结果。
     */
    class NotifyOutcome
    {
        /** 应答支付宝的字符串 */
        private final String responseText;
        /** 入账结果（可能 settled=false） */
        private final PayResult result;

        public NotifyOutcome(String responseText, PayResult result)
        {
            this.responseText = responseText;
            this.result = result;
        }

        public String getResponseText()
        {
            return responseText;
        }

        public PayResult getResult()
        {
            return result;
        }
    }
}
