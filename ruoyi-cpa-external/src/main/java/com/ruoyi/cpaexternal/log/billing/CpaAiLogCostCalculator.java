package com.ruoyi.cpaexternal.log.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogPayload;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogTokens;
import com.ruoyi.cpaexternal.model.domain.CpaModel;
import com.ruoyi.cpaexternal.model.mapper.CpaModelMapper;

/**
 * 调用费用计算器。
 *
 * <p>按模型官方定价算出本次请求的官方价格，再乘以用户计费倍率得到最终价格。
 * 官方定价单位是美元/百万 token，缓存读取价与缓存写入价为空时按输入价计。</p>
 *
 * <p>该计算只处理 CLIProxyAPI 已上报的用量数据，不参与 AI 请求转发。</p>
 */
@Component
public class CpaAiLogCostCalculator
{
    /** 官方定价的计量单位：每百万 token。 */
    private static final BigDecimal TOKENS_PER_MILLION = new BigDecimal("1000000");

    /** 金额精度，与 sys_user 余额口径保持一致。 */
    private static final int COST_SCALE = 10;

    @Autowired
    private CpaModelMapper modelMapper;

    /**
     * 计算单次调用的最终费用：官方价格 × 用户倍率。
     *
     * @param payload    CLIProxyAPI 上报的日志载荷，提供模型标识与 token 用量
     * @param multiplier 用户计费倍率，为空时按 1 计
     * @return 最终费用；模型未配置定价或单价全为 0 时返回 null，表示本次调用不计费
     */
    public BigDecimal calculate(CpaAiLogPayload payload, BigDecimal multiplier)
    {
        if (payload == null)
        {
            return null;
        }
        CpaModel model = modelMapper.selectPricingByName(payload.getModel());
        if (model == null)
        {
            return null;
        }
        BigDecimal inputPrice = nvl(model.getOfficialInputPrice());
        BigDecimal cacheReadPrice = nvl(model.getOfficialCacheReadPrice(), inputPrice);
        BigDecimal cacheWritePrice = nvl(model.getOfficialCacheWritePrice(), inputPrice);
        BigDecimal outputPrice = nvl(model.getOfficialOutputPrice());
        if (isZero(inputPrice) && isZero(cacheReadPrice) && isZero(cacheWritePrice) && isZero(outputPrice))
        {
            return null;
        }
        CpaAiLogTokens tokens = payload.getTokens();
        int uncachedInputTokens = Math.max(
                inputTokens(tokens) - cacheReadTokens(tokens) - cacheWriteTokens(tokens), 0);
        BigDecimal officialAmount = BigDecimal.valueOf(uncachedInputTokens).multiply(inputPrice)
                .add(BigDecimal.valueOf(cacheReadTokens(tokens)).multiply(cacheReadPrice))
                .add(BigDecimal.valueOf(cacheWriteTokens(tokens)).multiply(cacheWritePrice))
                .add(BigDecimal.valueOf(outputTokens(tokens)).multiply(outputPrice))
                .divide(TOKENS_PER_MILLION, COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal multiplierValue = multiplier == null ? BigDecimal.ONE : multiplier;
        return officialAmount.multiply(multiplierValue).setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    /** 输入总量，CLIProxyAPI 的 input_tokens 已包含缓存读取与缓存写入部分。 */
    private int inputTokens(CpaAiLogTokens tokens)
    {
        return tokens == null ? 0 : intValue(tokens.getInputTokens());
    }

    /** 缓存读取 token，上游未上报 cache_read_tokens 时回退到 cached_tokens。 */
    private int cacheReadTokens(CpaAiLogTokens tokens)
    {
        if (tokens == null)
        {
            return 0;
        }
        return tokens.getCacheReadTokens() == null
                ? intValue(tokens.getCachedTokens()) : intValue(tokens.getCacheReadTokens());
    }

    private int cacheWriteTokens(CpaAiLogTokens tokens)
    {
        return tokens == null ? 0 : intValue(tokens.getCacheCreationTokens());
    }

    /** 输出总量，CLIProxyAPI 的 output_tokens 已包含推理 token。 */
    private int outputTokens(CpaAiLogTokens tokens)
    {
        return tokens == null ? 0 : intValue(tokens.getOutputTokens());
    }

    private int intValue(Integer value)
    {
        return value == null ? 0 : Math.max(value, 0);
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal nvl(BigDecimal value, BigDecimal fallback)
    {
        return value == null ? fallback : value;
    }

    private boolean isZero(BigDecimal value)
    {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
}
