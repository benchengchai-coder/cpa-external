package com.ruoyi.cpaexternal.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingConstants;
import com.ruoyi.system.service.ISysConfigService;

/**
 * CLIProxyAPI 请求最低计费金额解析与应用服务。
 *
 * <p>最低计费属于一次请求的最终费用策略，在 ai_log 入库前一次性应用，
 * 不在各个模型或工具的单项计费阶段分别应用。</p>
 */
@Service
public class CpaBillingMinimumChargeResolver
{
    private static final Logger log = LoggerFactory.getLogger(CpaBillingMinimumChargeResolver.class);

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final int MONEY_SCALE = 10;

    @Autowired
    private ISysConfigService configService;

    /**
     * 读取当前系统配置中的最低计费金额。
     *
     * <p>该配置是可选的；缺失或非法时按关闭处理，不能阻断结算。</p>
     */
    public BigDecimal resolveMinimumAmount()
    {
        String value;
        try
        {
            value = configService.selectConfigByKey(CpaBillingConstants.CONFIG_MINIMUM_AMOUNT);
        }
        catch (Exception e)
        {
            log.warn("读取CLIProxyAPI请求最低计费金额失败，按0处理", e);
            return zeroAmount();
        }

        if (StrUtil.isBlank(value))
        {
            log.warn("CLIProxyAPI请求最低计费金额未配置，按0处理");
            return zeroAmount();
        }

        try
        {
            BigDecimal amount = new BigDecimal(value.trim());
            if (amount.compareTo(ZERO) < 0)
            {
                log.warn("CLIProxyAPI请求最低计费金额不能为负数，按0处理");
                return zeroAmount();
            }
            return normalize(amount);
        }
        catch (NumberFormatException e)
        {
            log.warn("CLIProxyAPI请求最低计费金额配置无效，按0处理: {}", e.getMessage());
            return zeroAmount();
        }
    }

    /**
     * 对一次请求的原始费用应用最低计费金额。
     *
     * <p>原始费用为 null（模型未定价）时按 0 处理，最低计费不生效，
     * 由结算流程按 no_charge 处理。</p>
     */
    public BigDecimal applyMinimumAmount(BigDecimal rawCost, BigDecimal minimumAmount)
    {
        if (rawCost == null || rawCost.compareTo(ZERO) <= 0)
        {
            return zeroAmount();
        }

        BigDecimal normalizedCost = normalize(rawCost);
        BigDecimal normalizedMinimum = normalize(minimumAmount);
        if (normalizedMinimum.compareTo(ZERO) <= 0)
        {
            return normalizedCost;
        }
        return normalizedCost.max(normalizedMinimum).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** 读取配置并立即应用最低计费金额，供 usage 入库和请求预检使用。 */
    public BigDecimal applyMinimumAmount(BigDecimal rawCost)
    {
        return applyMinimumAmount(rawCost, resolveMinimumAmount());
    }

    private BigDecimal normalize(BigDecimal amount)
    {
        if (amount == null || amount.compareTo(ZERO) <= 0)
        {
            return zeroAmount();
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroAmount()
    {
        return ZERO.setScale(MONEY_SCALE);
    }
}
