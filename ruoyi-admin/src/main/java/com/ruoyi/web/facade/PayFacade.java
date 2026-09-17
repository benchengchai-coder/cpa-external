package com.ruoyi.web.facade;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.invite.domain.AiInviteConstants;
import com.ruoyi.pay.domain.AiPayOrder;
import com.ruoyi.pay.domain.AiPayConstants;
import com.ruoyi.pay.domain.PayResult;
import com.ruoyi.pay.service.IAiPayService;
import com.ruoyi.pay.service.IAiPayService.NotifyOutcome;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.recharge.domain.AiRechargeConstants;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;
import com.ruoyi.system.recharge.service.IAiRechargeRecordService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 支付结算服务编排 Facade（聚合层）。
 * <p>
 * 串联「纯支付原语」与「用户余额 / 账户流水 / 邀请返利」：
 * <ul>
 *   <li>{@link #handleNotify}：纯域处理通知 → 若首次入账(settled) → {@link #applySettleSideEffects}；</li>
 *   <li>{@link #compensate}：定时补偿超时单 → 纯域查单/入账/关单 → 若 settled 触发副作用；</li>
 *   <li>{@link #queryOrderStatus}：纯域订单状态 + system 余额补充。</li>
 * </ul>
 * 幂等保障：副作用仅在 {@link PayResult#isSettled()}（CAS 0→1 成功）时触发。
 */
@Component
public class PayFacade
{
    private static final Logger log = LoggerFactory.getLogger(PayFacade.class);

    @Autowired
    private IAiPayService payService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private IAiRechargeRecordService rechargeRecordService;

    @Autowired
    private InviteFacade inviteFacade;

    @Autowired
    private ISysConfigService configService;

    /**
     * 查询在线充值业务状态，供钱包页面展示。
     */
    public Map<String, Object> getOnlineRechargeStatus()
    {
        boolean enabled = isOnlineRechargeEnabled();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled);
        result.put("available", enabled && payService.isOnlineRechargeAvailable());
        return result;
    }

    /**
     * 创建支付宝订单（透传纯域）。
     */
    public Map<String, Object> createOrder(Long userId, String username, BigDecimal amount)
    {
        if (!isOnlineRechargeEnabled())
        {
            throw new ServiceException("在线充值服务未开放");
        }
        return payService.createOrder(userId, username, amount);
    }

    private boolean isOnlineRechargeEnabled()
    {
        String value = configService.selectConfigByKey(AiPayConstants.CONFIG_ONLINE_RECHARGE_ENABLED);
        return Boolean.parseBoolean(value == null ? "" : value.trim());
    }

    /**
     * 查询当前用户待支付订单（透传纯域）。
     */
    public Map<String, Object> getPendingOrder(Long userId)
    {
        return payService.getPendingOrder(userId);
    }

    /**
     * 重新打开待支付订单的付款页（透传纯域）。
     */
    public Map<String, Object> reopenOrder(Long userId, String outTradeNo)
    {
        return payService.reopenOrder(userId, outTradeNo);
    }

    /**
     * 处理支付宝异步通知。
     */
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(Map<String, String> params)
    {
        NotifyOutcome outcome = payService.handleNotify(params);
        if (outcome.getResult() != null && outcome.getResult().isSettled())
        {
            applySettleSideEffects(outcome.getResult());
        }
        return outcome.getResponseText();
    }

    /**
     * 取消订单（若查单发现已支付，仍会入账并触发副作用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, String outTradeNo)
    {
        PayResult result = payService.cancelOrder(userId, outTradeNo);
        if (result.isSettled())
        {
            applySettleSideEffects(result);
        }
    }

    /**
     * 查询订单支付状态（含余额）。
     */
    public Map<String, Object> queryOrderStatus(Long userId, String outTradeNo)
    {
        Map<String, Object> result = payService.queryOrderStatus(userId, outTradeNo);
        if ("1".equals(result.get("status")) && result.get("userId") != null)
        {
            // 纯域只返回订单状态与 userId，余额在此从 system 补充
            Long orderUserId = Long.valueOf(result.get("userId").toString());
            SysUser user = sysUserService.selectUserById(orderUserId);
            result.put("balance", user != null ? user.getBalance() : null);
        }
        result.remove("userId");
        return result;
    }

    /**
     * 定时补偿：扫描超时未回调的订单，主动查支付宝真实状态。
     * 默认每分钟执行一次（间隔由 cpa.pay.alipay.compensate-interval-seconds 控制）。
     */
    @Scheduled(fixedDelayString = "${cpa.pay.alipay.compensate-interval-seconds:60}000")
    public void compensate()
    {
        List<AiPayOrder> pendingOrders = payService.scanExpiredPendingOrders();
        if (pendingOrders == null || pendingOrders.isEmpty())
        {
            return;
        }
        log.info("支付补偿任务扫描到 {} 笔超时未回调订单", pendingOrders.size());
        for (AiPayOrder order : pendingOrders)
        {
            try
            {
                PayResult result = compensateOne(order);
                if (result.isSettled())
                {
                    applySettleSideEffects(result);
                }
            }
            catch (Exception e)
            {
                log.error("补偿订单 {} 异常", order.getOutTradeNo(), e);
            }
        }
    }

    /**
     * 补偿单笔订单（独立事务，避免单笔失败影响整批）。
     */
    @Transactional(rollbackFor = Exception.class)
    public PayResult compensateOne(AiPayOrder order)
    {
        return payService.compensateOrder(order);
    }

    /**
     * 入账副作用编排（仅在 markOrderPaid CAS 成功时调用）：
     * 加余额 → 写账户流水 type=2 → 触发邀请返利。
     */
    private void applySettleSideEffects(PayResult result)
    {
        AiPayOrder order = result.getOrder();

        // 1. 加余额（addUserBalance 纯加法不会失败）
        sysUserMapper.addUserBalance(order.getUserId(), order.getTotalAmount());

        // 2. 写充值记录 type=2 在线支付，source_id 指向支付订单
        AiRechargeRecord record = new AiRechargeRecord();
        record.setUserId(order.getUserId());
        record.setUsername(order.getUsername());
        record.setType(AiRechargeConstants.TYPE_ONLINE_PAY);
        record.setAmount(order.getTotalAmount());
        record.setSourceId(order.getOrderId());
        record.setSourceName("支付宝");
        record.setStatus("0");
        record.setRemark("支付宝在线充值，交易号：" + result.getTradeNo());
        rechargeRecordService.insertAiRechargeRecord(record);

        // 3. 邀请返利：在线支付入账成功后触发（幂等由流水表 uk_source + 内部总开关控制）
        inviteFacade.accrueRebate(order.getUserId(), order.getTotalAmount(),
                AiInviteConstants.SOURCE_ONLINE_PAY, record.getRecordId());
    }
}
