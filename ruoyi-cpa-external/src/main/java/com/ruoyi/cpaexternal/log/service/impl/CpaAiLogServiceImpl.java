package com.ruoyi.cpaexternal.log.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cpaexternal.apikey.domain.CpaApiKey;
import com.ruoyi.cpaexternal.apikey.service.ICpaApiKeyService;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettleCommand;
import com.ruoyi.cpaexternal.billing.service.CpaBillingMinimumChargeResolver;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingSettlementService;
import com.ruoyi.cpaexternal.log.billing.CpaAiLogCostCalculator;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogPayload;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogTokens;
import com.ruoyi.cpaexternal.log.mapper.CpaAiLogMapper;
import com.ruoyi.cpaexternal.log.service.ICpaAiLogService;
import com.ruoyi.cpaexternal.log.support.CpaLogTimestampParser;
import com.ruoyi.system.service.ISysUserService;
import tools.jackson.databind.ObjectMapper;

/** CLIProxyAPI 日志服务实现。 */
@Service
public class CpaAiLogServiceImpl implements ICpaAiLogService
{
    private static final Logger log = LoggerFactory.getLogger(CpaAiLogServiceImpl.class);

    @Autowired
    private CpaAiLogMapper aiLogMapper;

    @Autowired
    private ICpaApiKeyService apiKeyService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private CpaAiLogCostCalculator costCalculator;

    @Autowired
    private CpaBillingMinimumChargeResolver minimumChargeResolver;

    /** 与 Spring MVC 数据绑定同代的 Jackson 3 ObjectMapper，用于序列化嵌套对象，保留 CLIProxyAPI 的 snake_case 字段名。 */
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ICpaBillingSettlementService billingSettlementService;

    @Override
    public CpaAiLog selectById(Long logId)
    {
        return aiLogMapper.selectById(logId);
    }

    @Override
    public List<CpaAiLog> selectList(CpaAiLog query)
    {
        return aiLogMapper.selectList(query);
    }

    @Override
    @Transactional
    public CpaAiLog ingest(CpaAiLogPayload payload)
    {
        if (payload == null)
        {
            throw new ServiceException("日志载荷不能为空");
        }
        if (StringUtils.isEmpty(payload.getRequestId()))
        {
            throw new ServiceException("request_id不能为空");
        }
        CpaAiLog existing = aiLogMapper.selectByRequestId(payload.getRequestId());
        if (existing != null)
        {
            // CLIProxyAPI 凭据重试会对同一 request_id 产生多条 usage，首条常是失败尝试；
            // 非失败记录到达时覆盖已入库的失败记录并重算费用，保证结算读到最终用量。
            if (Boolean.TRUE.equals(existing.getFailed()) && !Boolean.TRUE.equals(payload.getFailed()))
            {
                CpaAiLog updated = buildLog(payload, existing);
                updated.setLogId(existing.getLogId());
                aiLogMapper.updateUsageByRequestId(updated);
                submitSettlementAfterCommit(updated);
                return updated;
            }
            return existing;
        }

        CpaAiLog aiLog = buildLog(payload, null);
        aiLogMapper.insert(aiLog);
        submitSettlementAfterCommit(aiLog);
        return aiLog;
    }

    /** 由已落库的 usage 触发计费结算提交。 */
    private void submitSettlementAfterCommit(CpaAiLog aiLog)
    {
        CpaBillingSettleCommand command = new CpaBillingSettleCommand();
        command.setRequestId(aiLog.getRequestId());
        command.setUserId(aiLog.getUserId());
        command.setKeyId(aiLog.getKeyId());
        command.setAmount(aiLog.getCost());
        if (!TransactionSynchronizationManager.isSynchronizationActive())
        {
            submitSettlementSafely(command);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
        {
            @Override
            public void afterCommit()
            {
                submitSettlementSafely(command);
            }
        });
    }

    private void submitSettlementSafely(CpaBillingSettleCommand command)
    {
        try
        {
            billingSettlementService.submitSettlement(command);
        }
        catch (Exception e)
        {
            log.error("usage 落库后提交计费结算失败，等待补偿任务处理: requestId={}", command.getRequestId(), e);
        }
    }

    /**
     * 把 usage 载荷转换为 ai_log 实体，完成归属解析与费用快照。
     *
     * <p>覆盖场景传入 ownerSnapshot（已入库的首条记录），沿用其归属与倍率快照，
     * 避免重试记录因 Key 状态变化解析出不同归属。</p>
     */
    private CpaAiLog buildLog(CpaAiLogPayload payload, CpaAiLog ownerSnapshot)
    {
        CpaAiLog aiLog = new CpaAiLog();
        aiLog.setTimestamp(resolveTimestamp(payload));
        aiLog.setLatencyMs(payload.getLatencyMs());
        aiLog.setTtftMs(payload.getTtftMs());
        aiLog.setSource(payload.getSource());
        aiLog.setAuthIndex(payload.getAuthIndex());
        aiLog.setAccessTokenSha256(payload.getAccessTokenSha256());
        aiLog.setClientIp(payload.getClientIp());
        aiLog.setXForwardedFor(payload.getXForwardedFor());
        aiLog.setUserAgent(payload.getUserAgent());
        CpaAiLogTokens tokens = payload.getTokens();
        if (tokens != null)
        {
            aiLog.setInputTokens(tokens.getInputTokens());
            aiLog.setOutputTokens(tokens.getOutputTokens());
            aiLog.setReasoningTokens(tokens.getReasoningTokens());
            aiLog.setCachedTokens(tokens.getCachedTokens());
            aiLog.setCacheReadTokens(tokens.getCacheReadTokens());
            aiLog.setCacheReadTokensPresent(tokens.getCacheReadTokensPresent());
            aiLog.setCacheCreationTokens(tokens.getCacheCreationTokens());
            aiLog.setTotalTokens(tokens.getTotalTokens());
        }
        aiLog.setFailed(Boolean.TRUE.equals(payload.getFailed()));
        aiLog.setGenerate(payload.getGenerate());
        aiLog.setStream(payload.getStream());
        aiLog.setFail(toJson(payload.getFail()));
        aiLog.setAccountingVersion(payload.getAccountingVersion());
        aiLog.setTokenBreakdown(toJson(payload.getTokenBreakdown()));
        aiLog.setProvider(payload.getProvider());
        aiLog.setExecutorType(payload.getExecutorType());
        aiLog.setModel(payload.getModel());
        aiLog.setAlias(payload.getAlias());
        aiLog.setEndpoint(normalizeEndpoint(payload.getEndpoint()));
        aiLog.setAuthType(payload.getAuthType());
        aiLog.setApiKey(payload.getApiKey());
        aiLog.setRequestId(payload.getRequestId());
        aiLog.setSessionId(payload.getSessionId());
        aiLog.setParentSessionId(payload.getParentSessionId());
        aiLog.setReasoningEffort(payload.getReasoningEffort());
        aiLog.setServiceTier(payload.getServiceTier());
        aiLog.setResponseServiceTier(payload.getResponseServiceTier());
        aiLog.setResponseHeaders(JSONUtil.toJsonStr(payload.getResponseHeaders() == null
                ? Collections.emptyMap() : payload.getResponseHeaders()));
        if (ownerSnapshot != null)
        {
            aiLog.setUserId(ownerSnapshot.getUserId());
            aiLog.setKeyId(ownerSnapshot.getKeyId());
            aiLog.setKeyName(ownerSnapshot.getKeyName());
            aiLog.setUsername(ownerSnapshot.getUsername());
            aiLog.setBillingMultiplier(ownerSnapshot.getBillingMultiplier());
        }
        else
        {
            resolveApiKeyOwner(aiLog);
        }
        // 明确失败的请求没有可计费的成功用量，强制零费用，避免失败日志携带的 token 被收费。
        // 成功记录按官方定价 × 用户倍率计算，并在 ai_log 入库前应用请求级最低计费，
        // 使日志展示金额与后续实际结算金额保持一致。
        if (Boolean.TRUE.equals(aiLog.getFailed()))
        {
            aiLog.setCost(BigDecimal.ZERO);
        }
        else
        {
            BigDecimal rawCost = costCalculator.calculate(payload, aiLog.getBillingMultiplier());
            aiLog.setCost(rawCost == null ? null : minimumChargeResolver.applyMinimumAmount(rawCost));
        }
        return aiLog;
    }

    @Override
    @Transactional
    public int deleteByIds(Long[] logIds)
    {
        return aiLogMapper.deleteByIds(logIds);
    }

    /**
     * 清洗 CLIProxyAPI 上报的端点值，只保留路径部分。
     *
     * <p>CLIProxyAPI 以「HTTP方法 路径」格式上报（如 POST /v1/responses），
     * 而 AI 调用的方法恒为 POST，入库前剥掉方法前缀；不含空格的值原样保留，
     * 兼容后续上报格式变化。</p>
     */
    private String normalizeEndpoint(String endpoint)
    {
        if (endpoint == null)
        {
            return null;
        }
        int separatorIndex = endpoint.indexOf(' ');
        return separatorIndex >= 0 ? endpoint.substring(separatorIndex + 1) : endpoint;
    }

    /** 把嵌套对象序列化为 JSON 字符串入库；null 时写入空对象，避免 JSON 列出现非法值。 */
    private String toJson(Object value)
    {
        if (value == null)
        {
            return "{}";
        }
        return objectMapper.writeValueAsString(value);
    }

    /**
     * 解析 CLIProxyAPI 上报的日志时间。
     *
     * <p>上报值是 ISO-8601 字符串（秒的小数位可能到纳秒并带时区偏移），
     * 而 ai_log.timestamp 是 datetime 列，因此在这里先格式化成时间再入库。
     * 缺失或无法识别时回退为入库时间，避免因为一个时间格式问题丢掉整条用量记录。</p>
     */
    private Date resolveTimestamp(CpaAiLogPayload payload)
    {
        Date timestamp = CpaLogTimestampParser.parse(payload.getTimestamp());
        if (timestamp != null)
        {
            return timestamp;
        }
        log.warn("CLIProxyAPI usage 消息的日志时间无法解析，按入库时间记录：request_id={}, timestamp={}",
                payload.getRequestId(), payload.getTimestamp());
        return new Date();
    }

    private void resolveApiKeyOwner(CpaAiLog aiLog)
    {
        if (StringUtils.isEmpty(aiLog.getApiKey()))
        {
            return;
        }
        CpaApiKey apiKey = apiKeyService.selectByPlainKey(aiLog.getApiKey());
        if (apiKey == null)
        {
            return;
        }
        aiLog.setKeyId(apiKey.getKeyId());
        aiLog.setKeyName(apiKey.getKeyName());
        aiLog.setUserId(apiKey.getUserId());
        SysUser user = sysUserService.selectUserById(apiKey.getUserId());
        if (user != null)
        {
            aiLog.setUsername(user.getUserName());
            // 快照入库时的用户计费倍率，避免后续调整倍率影响历史记录。
            aiLog.setBillingMultiplier(user.getBillingMultiplier());
        }
    }
}
