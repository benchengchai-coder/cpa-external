package com.ruoyi.cpaexternal.billing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 恢复 Spring 方法级调度。
 *
 * <p>历史版本的 @EnableScheduling 随 ruoyi-aigate 模块被一并删除，
 * 导致当前代码中的 @Scheduled 任务（含支付补偿）实际未运行；
 * 计费结算 Worker、超时释放与任务清理都依赖该开关，这里一并恢复。</p>
 */
@Configuration
@EnableScheduling
public class CpaBillingScheduleConfig
{
}
