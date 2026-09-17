package com.ruoyi.pay.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alipay.api.AlipayResponse;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.ruoyi.pay.config.AliPayProperties;
import com.ruoyi.pay.domain.AiPayOrder;
import com.ruoyi.pay.domain.PayResult;
import com.ruoyi.pay.mapper.AiPayOrderMapper;
import com.ruoyi.pay.service.IAiPayService;
import com.ruoyi.pay.service.alipay.AlipayClientFactory;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 支付宝支付 服务层实现（纯支付原语）。
 *
 * 核心设计：
 * 1. 幂等防重 —— 异步通知与补偿查单统一走 {@link #markOrderPaid}，靠 updateOrderStatusCAS
 *    条件更新（0→1）的影响行数判断是否首次入账，天然防并发重放。
 * 2. 验签 —— 异步通知必须 AlipaySignature.rsaCheckV1 验签，防伪造回调。
 * 3. 金额校验 —— 入账前比对回调金额与订单金额一致，防篡改。
 * 4. 防掉单 —— 补偿查单由聚合层定时驱动（{@link #scanExpiredPendingOrders} + {@link #compensateOrder}）。
 *
 * 本实现不操作用户余额、不写账户流水、不触发返利；入账副作用由 PayFacade 基于
 * {@link PayResult#isSettled()} 编排。事务边界上移聚合层，内部方法不加 @Transactional。
 */
@Service
public class AiPayServiceImpl implements IAiPayService
{
    private static final Logger log = LoggerFactory.getLogger(AiPayServiceImpl.class);

    /** 订单状态：待支付 */
    private static final String STATUS_PENDING = "0";
    /** 订单状态：已支付 */
    private static final String STATUS_PAID = "1";
    /** 订单状态：已关闭 */
    private static final String STATUS_CLOSED = "2";

    /** 支付宝交易成功状态 */
    private static final String ALIPAY_TRADE_SUCCESS = "TRADE_SUCCESS";
    /** 支付宝交易结束状态（已关闭/全额退款等，不可再操作） */
    private static final String ALIPAY_TRADE_FINISHED = "TRADE_FINISHED";

    /** 单笔充值最小金额（美元） */
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.1");
    /** 单笔充值最大金额（美元） */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000");

    @Autowired
    private AliPayProperties properties;

    @Autowired
    private AlipayClientFactory clientFactory;

    @Autowired
    private AiPayOrderMapper payOrderMapper;

    @Override
    public boolean isOnlineRechargeAvailable()
    {
        return clientFactory.isReady();
    }

    /**
     * 创建支付宝订单并返回电脑网站支付表单。
     */
    @Override
    public Map<String, Object> createOrder(Long userId, String username, BigDecimal amount)
    {
        if (!clientFactory.isReady())
        {
            throw new ServiceException("在线充值支付配置未就绪");
        }

        AiPayOrder pendingOrder = findCurrentPendingOrder(userId);
        if (pendingOrder != null)
        {
            if (isOrderExpired(pendingOrder))
            {
                throw new ServiceException("待支付订单正在关闭，请稍后重试");
            }
            return buildPayFormResult(pendingOrder, false);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("充值金额必须大于0");
        }
        if (amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0)
        {
            throw new ServiceException("单笔充值金额需在 " + MIN_AMOUNT + " ~ " + MAX_AMOUNT + " 之间");
        }
        if (amount.stripTrailingZeros().scale() > 2)
        {
            throw new ServiceException("充值金额最多支持2位小数");
        }

        // 生成商户订单号：时间戳 + 随机串，配合唯一索引保证全局唯一
        String outTradeNo = System.currentTimeMillis() + IdUtils.fastSimpleUUID().substring(0, 16);

        AiPayOrder order = new AiPayOrder();
        order.setOutTradeNo(outTradeNo);
        order.setUserId(userId);
        order.setUsername(username);
        order.setTotalAmount(amount);
        order.setSubject("AI网关账户充值");
        order.setStatus(STATUS_PENDING);
        order.setCreateBy(username);
        payOrderMapper.insertAiPayOrder(order);
        order = payOrderMapper.selectAiPayOrderById(order.getOrderId());

        // 调支付宝电脑网站支付，拿到完整支付表单 HTML
        return buildPayFormResult(order, true);
    }

    /**
     * 调用 alipay.trade.page.pay 拼装支付表单。
     */
    private String buildPagePayForm(AiPayOrder order)
    {
        try
        {
            AlipayClient client = clientFactory.getClient();
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(properties.getNotifyUrl());
            request.setReturnUrl(properties.getReturnUrl());

            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(order.getOutTradeNo());
            model.setTotalAmount(formatAlipayAmount(order.getTotalAmount()));
            model.setSubject(order.getSubject());
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            request.setBizModel(model);

            AlipayTradePagePayResponse response = client.pageExecute(request);
            return response.getBody();
        }
        catch (AlipayApiException e)
        {
            log.error("调起支付宝电脑网站支付失败，订单号：{}", order.getOutTradeNo(), e);
            throw new ServiceException("调起支付失败：" + e.getErrMsg());
        }
    }

    /**
     * 查询当前用户最新一笔待支付订单。
     */
    @Override
    public Map<String, Object> getPendingOrder(Long userId)
    {
        AiPayOrder order = findCurrentPendingOrder(userId);
        return order == null ? null : buildPendingOrderInfo(order);
    }

    /**
     * 重新打开待支付订单的支付宝支付页。
     * 订单超时不再内部补偿，直接抛异常（聚合层可捕获后触发补偿）。
     */
    @Override
    public Map<String, Object> reopenOrder(Long userId, String outTradeNo)
    {
        AiPayOrder order = requireUserPendingOrder(userId, outTradeNo);
        if (isOrderExpired(order))
        {
            throw new ServiceException("订单已超时，请重新发起支付");
        }
        return buildPayFormResult(order, false);
    }

    /**
     * 用户主动取消待支付订单。
     * 若查单发现已支付，执行 markOrderPaid 并通过 PayResult.settled 告知聚合层入账。
     */
    @Override
    public PayResult cancelOrder(Long userId, String outTradeNo)
    {
        AiPayOrder order = requireUserPendingOrder(userId, outTradeNo);
        AlipayTradeQueryResponse queryResp = queryTrade(order.getOutTradeNo());
        if (queryResp == null)
        {
            throw new ServiceException("暂时无法确认订单状态，请稍后重试");
        }
        if (isPaidTrade(queryResp))
        {
            PayResult result = markOrderPaid(order, queryResp.getTradeNo(), new Date());
            throw new ServiceException("订单已支付，已为您完成入账");
        }
        if (isTradeNotExist(queryResp))
        {
            closeLocalPendingOrder(order);
            return new PayResult(false, order, null, null);
        }
        if (!queryResp.isSuccess())
        {
            throw new ServiceException("暂时无法确认订单状态，请稍后重试");
        }
        if (!closeTrade(order))
        {
            throw new ServiceException("取消订单失败，请稍后重试");
        }
        return new PayResult(false, order, null, null);
    }

    private AiPayOrder requireUserPendingOrder(Long userId, String outTradeNo)
    {
        if (StringUtils.isEmpty(outTradeNo))
        {
            throw new ServiceException("订单号不能为空");
        }
        AiPayOrder order = payOrderMapper.selectAiPayOrderByOutTradeNo(outTradeNo);
        if (order == null || !userId.equals(order.getUserId()))
        {
            throw new ServiceException("订单不存在");
        }
        if (!STATUS_PENDING.equals(order.getStatus()))
        {
            throw new ServiceException("订单不是待支付状态");
        }
        return order;
    }

    private AiPayOrder findCurrentPendingOrder(Long userId)
    {
        return payOrderMapper.selectLatestPendingOrderByUserId(userId);
    }

    private Map<String, Object> buildPayFormResult(AiPayOrder order, boolean created)
    {
        Map<String, Object> result = buildPendingOrderInfo(order);
        result.put("payForm", buildPagePayForm(order));
        result.put("created", created);
        return result;
    }

    private Map<String, Object> buildPendingOrderInfo(AiPayOrder order)
    {
        long expireAt = getOrderExpireAtMillis(order);
        long remainingSeconds = Math.max(0L, (expireAt - System.currentTimeMillis() + 999L) / 1000L);
        Map<String, Object> result = new HashMap<>();
        result.put("outTradeNo", order.getOutTradeNo());
        result.put("amount", order.getTotalAmount());
        result.put("status", order.getStatus());
        result.put("createTime", order.getCreateTime());
        result.put("expireTime", new Date(expireAt));
        result.put("expireTimestamp", expireAt);
        result.put("remainingSeconds", remainingSeconds);
        return result;
    }

    private boolean isOrderExpired(AiPayOrder order)
    {
        return System.currentTimeMillis() >= getOrderExpireAtMillis(order);
    }

    private long getOrderExpireAtMillis(AiPayOrder order)
    {
        Date createTime = order.getCreateTime();
        long startTime = createTime == null ? System.currentTimeMillis() : createTime.getTime();
        int expireMinutes = Math.max(1, properties.getOrderExpireMinutes());
        return startTime + expireMinutes * 60_000L;
    }

    private String formatAlipayAmount(BigDecimal amount)
    {
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() < 0)
        {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

    /**
     * 处理支付宝异步通知（纯：验签 + 金额校验 + markOrderPaid）。
     * 入账副作用（加余额/流水/返利）由聚合层据 result.settled 编排。
     */
    @Override
    public NotifyOutcome handleNotify(Map<String, String> params)
    {
        // 1. 验签，防伪造回调
        if (!verifySign(params))
        {
            log.warn("支付宝异步通知验签失败：{}", params);
            return new NotifyOutcome("failure", new PayResult(false, null, null, null));
        }

        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String totalAmount = params.get("total_amount");

        AiPayOrder order = payOrderMapper.selectAiPayOrderByOutTradeNo(outTradeNo);
        if (order == null)
        {
            log.warn("支付宝异步通知对应的订单不存在：{}", outTradeNo);
            return new NotifyOutcome("failure", new PayResult(false, null, null, null));
        }

        // 记录通知到达时间
        payOrderMapper.updateNotifyTime(order.getOrderId(), new Date());

        // 非成功状态不处理，但仍应答 success 避免支付宝反复重试
        if (!ALIPAY_TRADE_SUCCESS.equals(tradeStatus))
        {
            log.info("支付宝异步通知非成功状态，订单号：{}，状态：{}", outTradeNo, tradeStatus);
            return new NotifyOutcome("success", new PayResult(false, order, null, null));
        }

        // 金额一致性校验，防篡改
        if (!amountEquals(totalAmount, order.getTotalAmount()))
        {
            log.error("支付宝异步通知金额不一致，订单号：{}，回调金额：{}，订单金额：{}",
                    outTradeNo, totalAmount, order.getTotalAmount());
            return new NotifyOutcome("failure", new PayResult(false, order, null, null));
        }

        // 入账（内部含 CAS 幂等）
        PayResult result = markOrderPaid(order, tradeNo, new Date());
        return new NotifyOutcome("success", result);
    }

    /**
     * 订单入账核心：CAS 状态推进 0→1。
     * 幂等核心 —— 仅当 status 从 0→1 推进成功（影响行数=1）才返回 settled=true，
     * 重复回调/补偿重放时影响行数=0，settled=false，聚合层不重复加钱。
     */
    private PayResult markOrderPaid(AiPayOrder order, String tradeNo, Date payTime)
    {
        int rows = payOrderMapper.updateOrderStatusCAS(
                order.getOrderId(), STATUS_PENDING, STATUS_PAID, tradeNo, payTime);
        if (rows == 0)
        {
            // 已被处理过（重放回调或补偿已入账），幂等返回
            log.info("订单 {} 已处理过，跳过入账", order.getOutTradeNo());
            return new PayResult(false, order, tradeNo, payTime);
        }
        log.info("订单 {} 入账成功，金额：{}", order.getOutTradeNo(), order.getTotalAmount());
        return new PayResult(true, order, tradeNo, payTime);
    }

    /**
     * 验签。
     */
    private boolean verifySign(Map<String, String> params)
    {
        try
        {
            return AlipaySignature.rsaCheckV1(
                    params,
                    properties.getAlipayPublicKey(),
                    properties.getCharset(),
                    properties.getSignType());
        }
        catch (AlipayApiException e)
        {
            log.error("支付宝验签异常", e);
            return false;
        }
    }

    /**
     * 查询订单支付状态（前端结果页轮询用）。纯：只返回订单字段，不含余额。
     */
    @Override
    public Map<String, Object> queryOrderStatus(Long userId, String outTradeNo)
    {
        AiPayOrder order = payOrderMapper.selectAiPayOrderByOutTradeNo(outTradeNo);
        Map<String, Object> result = new HashMap<>();
        if (order == null || userId == null || !userId.equals(order.getUserId()))
        {
            result.put("status", "not_found");
            return result;
        }
        result.put("status", order.getStatus());
        if (STATUS_PAID.equals(order.getStatus()))
        {
            result.put("amount", order.getTotalAmount());
            result.put("userId", order.getUserId());
            // 余额由聚合层补充（避免此处依赖 system）
        }
        return result;
    }

    /**
     * 扫描超时未回调的待支付订单。
     */
    @Override
    public List<AiPayOrder> scanExpiredPendingOrders()
    {
        long expireMs = properties.getOrderExpireMinutes() * 60_000L;
        Date beforeTime = new Date(System.currentTimeMillis() - expireMs);
        return payOrderMapper.selectPendingOrdersBeforeTime(beforeTime);
    }

    /**
     * 补偿单笔订单：查询支付宝真实状态，已支付则 markOrderPaid（settled=true），
     * 未支付则关单。
     */
    @Override
    public PayResult compensateOrder(AiPayOrder order)
    {
        AlipayTradeQueryResponse queryResp = queryTrade(order.getOutTradeNo());
        if (queryResp == null)
        {
            return new PayResult(false, order, null, null);
        }
        if (isPaidTrade(queryResp))
        {
            // 已支付，入账（markOrderPaid 内含 CAS 幂等，重复处理安全）
            return markOrderPaid(order, queryResp.getTradeNo(), new Date());
        }
        if (!queryResp.isSuccess() && !isTradeNotExist(queryResp))
        {
            return new PayResult(false, order, null, null);
        }
        // 仍未支付或支付宝侧交易不存在，主动关单/本地关单，推进状态 0→2
        if (isTradeNotExist(queryResp))
        {
            closeLocalPendingOrder(order);
        }
        else
        {
            closeTrade(order);
        }
        return new PayResult(false, order, null, null);
    }

    /**
     * 调用 alipay.trade.query 查询交易状态。
     */
    private AlipayTradeQueryResponse queryTrade(String outTradeNo)
    {
        try
        {
            AlipayClient client = clientFactory.getClient();
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(outTradeNo);
            request.setBizModel(model);
            AlipayTradeQueryResponse response = client.execute(request);
            if (!response.isSuccess())
            {
                log.warn("支付宝查询订单失败，订单号：{}，code：{}，msg：{}",
                        outTradeNo, response.getCode(), response.getMsg());
            }
            return response;
        }
        catch (AlipayApiException e)
        {
            log.error("支付宝查询订单异常，订单号：{}", outTradeNo, e);
            return null;
        }
    }

    /**
     * 调用 alipay.trade.close 关闭交易，并推进订单状态为已关闭。
     */
    private boolean closeTrade(AiPayOrder order)
    {
        boolean shouldCloseLocal = false;
        try
        {
            AlipayClient client = clientFactory.getClient();
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            AlipayTradeCloseModel model = new AlipayTradeCloseModel();
            model.setOutTradeNo(order.getOutTradeNo());
            request.setBizModel(model);
            AlipayTradeCloseResponse response = client.execute(request);
            if (!response.isSuccess())
            {
                log.warn("支付宝关单失败，订单号：{}，code：{}，msg：{}",
                        order.getOutTradeNo(), response.getCode(), response.getMsg());
                shouldCloseLocal = isTradeNotExist(response);
            }
            else
            {
                shouldCloseLocal = true;
            }
        }
        catch (AlipayApiException e)
        {
            log.error("支付宝关单异常，订单号：{}", order.getOutTradeNo(), e);
        }
        if (shouldCloseLocal)
        {
            return closeLocalPendingOrder(order);
        }
        return false;
    }

    private boolean closeLocalPendingOrder(AiPayOrder order)
    {
        return payOrderMapper.updateOrderStatusCAS(
                order.getOrderId(), STATUS_PENDING, STATUS_CLOSED, null, null) > 0;
    }

    private boolean isPaidTrade(AlipayTradeQueryResponse response)
    {
        if (response == null)
        {
            return false;
        }
        String tradeStatus = response.getTradeStatus();
        return ALIPAY_TRADE_SUCCESS.equals(tradeStatus) || ALIPAY_TRADE_FINISHED.equals(tradeStatus);
    }

    private boolean isTradeNotExist(AlipayResponse response)
    {
        return response != null && "ACQ.TRADE_NOT_EXIST".equals(response.getSubCode());
    }

    /**
     * 金额相等比较：支付宝回调金额为字符串，订单金额为 BigDecimal，需按数值比对。
     */
    private boolean amountEquals(String callbackAmount, BigDecimal orderAmount)
    {
        if (StringUtils.isEmpty(callbackAmount))
        {
            return false;
        }
        try
        {
            return new BigDecimal(callbackAmount).compareTo(orderAmount) == 0;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
}
