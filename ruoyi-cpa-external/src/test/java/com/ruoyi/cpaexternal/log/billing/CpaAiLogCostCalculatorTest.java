package com.ruoyi.cpaexternal.log.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogPayload;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogTokens;
import com.ruoyi.cpaexternal.model.domain.CpaModel;
import com.ruoyi.cpaexternal.model.mapper.CpaModelMapper;

/** 调用费用计算测试：官方定价 × 用户倍率。 */
class CpaAiLogCostCalculatorTest
{
    private static final String MODEL_NAME = "gpt-5.6-luna";

    private CpaModelMapper modelMapper;
    private CpaAiLogCostCalculator calculator;

    @BeforeEach
    void setUp() throws Exception
    {
        modelMapper = mock(CpaModelMapper.class);
        calculator = new CpaAiLogCostCalculator();
        Field field = CpaAiLogCostCalculator.class.getDeclaredField("modelMapper");
        field.setAccessible(true);
        field.set(calculator, modelMapper);
    }

    @Test
    void shouldPriceEachTokenCategoryAndApplyMultiplier()
    {
        // 官方定价：输入 $1、输出 $6、缓存读取 $0.1、缓存写入 $1.25（每百万 token）。
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(model(
                "1.000000", "6.000000", "0.100000", "1.250000"));
        // CLIProxyAPI 真实样例：输入 18159 中 17152 命中缓存、1007 未缓存，输出 5。
        CpaAiLogPayload payload = payload(18159, 5, 17152, 0, 17152);

        BigDecimal cost = calculator.calculate(payload, new BigDecimal("1.50"));

        // 官方价 = (1007×1 + 17152×0.1 + 0×1.25 + 5×6) / 1e6 = 0.0027522
        // 最终价 = 0.0027522 × 1.5 = 0.0041283
        assertEquals(new BigDecimal("0.0041283000"), cost);
    }

    @Test
    void shouldPriceCacheWriteWithItsOwnPrice()
    {
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(model(
                "1.000000", "6.000000", "0.100000", "1.250000"));
        CpaAiLogPayload payload = payload(1000, 0, 0, 100, 0);

        BigDecimal cost = calculator.calculate(payload, BigDecimal.ONE);

        // 未缓存 900×1 + 缓存写入 100×1.25 = 0.001025
        assertEquals(new BigDecimal("0.0010250000"), cost);
    }

    @Test
    void shouldFallBackToInputPriceWhenCachePricesAreNull()
    {
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(model(
                "1.000000", "2.000000", null, null));
        CpaAiLogPayload payload = payload(1000, 100, 400, 100, 400);

        BigDecimal cost = calculator.calculate(payload, BigDecimal.ONE);

        // 官方价 = (500×1 + 400×1 + 100×1 + 100×2) / 1e6 = 0.0012
        assertEquals(new BigDecimal("0.0012000000"), cost);
    }

    @Test
    void shouldFallBackToCachedTokensWhenCacheReadTokensMissing()
    {
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(model(
                "1.000000", "6.000000", "0.100000", "1.250000"));
        CpaAiLogPayload payload = payload(1000, 0, null, 0, 800);

        BigDecimal cost = calculator.calculate(payload, BigDecimal.ONE);

        // 未缓存 200×1 + 缓存读取 800×0.1 = 0.00028
        assertEquals(new BigDecimal("0.0002800000"), cost);
    }

    @Test
    void shouldNotPriceNegativeUncachedTokens()
    {
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(model(
                "1.000000", "6.000000", "0.100000", "1.250000"));
        // 上游只上报缓存读取、input_tokens 未含缓存时，未缓存部分按 0 计，不出现负数金额。
        CpaAiLogPayload payload = payload(100, 0, 500, 0, 500);

        BigDecimal cost = calculator.calculate(payload, BigDecimal.ONE);

        assertEquals(new BigDecimal("0.0000500000"), cost);
    }

    @Test
    void shouldReturnNullWhenModelIsNotConfigured()
    {
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(null);

        assertNull(calculator.calculate(payload(1000, 100, 0, 0, 0), BigDecimal.ONE));
    }

    @Test
    void shouldReturnNullWhenAllPricesAreZero()
    {
        // 与 deepseek-v4-pro 一致：单价全为 0 视为未配置定价，页面显示 "-"。
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(model(
                "0.000000", "0.000000", null, null));

        assertNull(calculator.calculate(payload(1000, 100, 0, 0, 0), BigDecimal.ONE));
    }

    @Test
    void shouldPriceWithoutMultiplierWhenUserUnknown()
    {
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(model(
                "1.000000", "6.000000", "0.100000", "1.250000"));

        BigDecimal cost = calculator.calculate(payload(1000, 0, 0, 0, 0), null);

        // 倍率缺失时按 1 计，即官方价。
        assertEquals(new BigDecimal("0.0010000000"), cost);
    }

    @Test
    void shouldReturnZeroWhenModelPricedButRequestHasNoUsage()
    {
        when(modelMapper.selectPricingByName(MODEL_NAME)).thenReturn(model(
                "1.000000", "6.000000", "0.100000", "1.250000"));

        BigDecimal cost = calculator.calculate(payload(null, null, null, null, null), BigDecimal.ONE);

        assertEquals(new BigDecimal("0.0000000000"), cost);
    }

    private CpaModel model(String inputPrice, String outputPrice, String cacheReadPrice, String cacheWritePrice)
    {
        CpaModel model = new CpaModel();
        model.setModelName(MODEL_NAME);
        model.setOfficialInputPrice(new BigDecimal(inputPrice));
        model.setOfficialOutputPrice(new BigDecimal(outputPrice));
        model.setOfficialCacheReadPrice(cacheReadPrice == null ? null : new BigDecimal(cacheReadPrice));
        model.setOfficialCacheWritePrice(cacheWritePrice == null ? null : new BigDecimal(cacheWritePrice));
        return model;
    }

    private CpaAiLogPayload payload(Integer inputTokens, Integer outputTokens, Integer cacheReadTokens,
            Integer cacheCreationTokens, Integer cachedTokens)
    {
        CpaAiLogTokens tokens = new CpaAiLogTokens();
        tokens.setInputTokens(inputTokens);
        tokens.setOutputTokens(outputTokens);
        tokens.setCacheReadTokens(cacheReadTokens);
        tokens.setCacheCreationTokens(cacheCreationTokens);
        tokens.setCachedTokens(cachedTokens);
        CpaAiLogPayload payload = new CpaAiLogPayload();
        payload.setModel(MODEL_NAME);
        payload.setTokens(tokens);
        return payload;
    }
}
