package com.ruoyi.pay.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.pay.domain.AiPayOrder;

/**
 * 支付宝支付订单 数据层
 */
public interface AiPayOrderMapper
{
    public AiPayOrder selectAiPayOrderById(Long orderId);

    public AiPayOrder selectAiPayOrderByOutTradeNo(String outTradeNo);

    public List<AiPayOrder> selectAiPayOrderList(AiPayOrder aiPayOrder);

    /**
     * 查询用户最新一笔待支付订单。
     */
    public AiPayOrder selectLatestPendingOrderByUserId(@Param("userId") Long userId);

    /**
     * 补偿任务：扫描超时未支付的订单（status=0 且创建时间早于指定时间）
     */
    public List<AiPayOrder> selectPendingOrdersBeforeTime(@Param("beforeTime") Date beforeTime);

    public int insertAiPayOrder(AiPayOrder aiPayOrder);

    /**
     * 条件更新状态（CAS 风格，幂等核心）。
     * 仅当当前状态 = fromStatus 时才更新为 toStatus，返回受影响行数：
     * 1 表示本次推进成功，0 表示已被其他线程/回调处理。
     */
    public int updateOrderStatusCAS(@Param("orderId") Long orderId,
                                    @Param("fromStatus") String fromStatus,
                                    @Param("toStatus") String toStatus,
                                    @Param("tradeNo") String tradeNo,
                                    @Param("payTime") Date payTime);

    /**
     * 更新异步通知到达时间
     */
    public int updateNotifyTime(@Param("orderId") Long orderId, @Param("notifyTime") Date notifyTime);
}
