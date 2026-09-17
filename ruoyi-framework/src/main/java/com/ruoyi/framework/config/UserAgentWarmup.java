package com.ruoyi.framework.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.http.UserAgentUtils;

/**
 * Yauaa 预热：把 UserAgentAnalyzer 的规则加载与匹配表构建提前到应用启动阶段。
 * <p>
 * 背景：Yauaa 的 {@code UserAgentAnalyzer} 是 static final 单例，真正的规则解析
 * （加载 122 个 yaml + 构建 ~20 万条 Hash 匹配表，耗时约 1.6 秒）发生在首次
 * {@code parse()} 调用。登录流程会在 {@code TokenService.createToken} 设置
 * browser/os 时首次触发，导致重启后首次登录被阻塞约 1.6 秒。
 * <p>
 * 这里在容器启动时主动触发一次 parse，让所有一次性开销发生在启动阶段，
 * 用户首次登录即与后续登录一致（~100ms）。
 *
 * @author ruoyi
 */
@Component
@Order(0)
public class UserAgentWarmup
{
    private static final String WARMUP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @PostConstruct
    public void warmup()
    {
        // 触发 UserAgentUtils 类加载 + Yauaa 首次 parse（完成规则加载与匹配表构建）
        UserAgentUtils.getBrowser(WARMUP_UA);
        UserAgentUtils.getOperatingSystem(WARMUP_UA);
    }
}
