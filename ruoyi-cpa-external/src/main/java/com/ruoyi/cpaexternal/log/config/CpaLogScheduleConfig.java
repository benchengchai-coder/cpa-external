package com.ruoyi.cpaexternal.log.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 确保 log 模块的方法级调度生效。
 *
 * <p>billing 模块已有一处 {@code @EnableScheduling}，重复标注无副作用；
 * 在此单独声明是为了让上游失败事件清理任务不隐式依赖计费模块的配置类。</p>
 */
@Configuration
@EnableScheduling
public class CpaLogScheduleConfig
{
}
