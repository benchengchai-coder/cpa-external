package com.ruoyi.cpaexternal.log.service.impl;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cpaexternal.log.domain.CpaErrorEventAuthStatus;
import com.ruoyi.cpaexternal.log.domain.CpaErrorEventQuotaStatus;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailure;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailureEvent;
import com.ruoyi.cpaexternal.log.mapper.CpaUpstreamFailureMapper;
import com.ruoyi.cpaexternal.log.service.ICpaUpstreamFailureService;
import com.ruoyi.cpaexternal.log.support.CpaLogTimestampParser;
import tools.jackson.databind.ObjectMapper;

/** CLIProxyAPI 上游失败事件服务实现。 */
@Service
public class CpaUpstreamFailureServiceImpl implements ICpaUpstreamFailureService
{
    private static final Logger log = LoggerFactory.getLogger(CpaUpstreamFailureServiceImpl.class);

    @Autowired
    private CpaUpstreamFailureMapper upstreamFailureMapper;

    /** 与 Spring MVC 数据绑定同代的 Jackson 3 ObjectMapper，用于序列化凭证状态快照，保留 snake_case 字段名。 */
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void record(CpaUpstreamFailureEvent event)
    {
        upstreamFailureMapper.insert(buildFailure(event));
    }

    @Override
    public CpaUpstreamFailure selectById(Long failureId)
    {
        return upstreamFailureMapper.selectById(failureId);
    }

    @Override
    public List<CpaUpstreamFailure> selectList(CpaUpstreamFailure query)
    {
        return upstreamFailureMapper.selectList(query);
    }

    @Override
    public int deleteByIds(Long[] failureIds)
    {
        return upstreamFailureMapper.deleteByIds(failureIds);
    }

    @Override
    public int deleteBefore(Date beforeTime, int limit)
    {
        return upstreamFailureMapper.deleteBefore(beforeTime, limit);
    }

    private CpaUpstreamFailure buildFailure(CpaUpstreamFailureEvent event)
    {
        CpaUpstreamFailure failure = new CpaUpstreamFailure();
        failure.setEventTime(resolveEventTime(event));
        failure.setProvider(event.getProvider());
        failure.setModel(event.getModel());
        failure.setAuthId(event.getAuthId());
        failure.setAuthIndex(event.getAuthIndex());
        failure.setStatusCode(event.getStatusCode());
        failure.setBody(event.getBody());
        failure.setCode(event.getCode());
        failure.setRetryable(Boolean.TRUE.equals(event.getRetryable()));
        CpaErrorEventAuthStatus authStatus = event.getAuthStatus();
        if (authStatus != null)
        {
            failure.setAuthStatus(authStatus.getStatus());
            failure.setAuthDisabled(Boolean.TRUE.equals(authStatus.getDisabled()));
            failure.setAuthUnavailable(Boolean.TRUE.equals(authStatus.getUnavailable()));
            failure.setAuthNextRetryAt(CpaLogTimestampParser.parse(authStatus.getNextRetryAfter()));
            CpaErrorEventQuotaStatus quota = authStatus.getQuota();
            if (quota != null)
            {
                failure.setQuotaExceeded(Boolean.TRUE.equals(quota.getExceeded()));
                failure.setQuotaReason(trimToNull(quota.getReason()));
            }
            failure.setAuthStatusSnapshot(objectMapper.writeValueAsString(authStatus));
        }
        return failure;
    }

    /**
     * 解析 CLIProxyAPI 上报的事件时间。
     *
     * <p>上报值是 ISO-8601 字符串，解析失败时回退为入库时间并告警，
     * 避免因为一个时间格式问题丢掉整条失败事件。</p>
     */
    private Date resolveEventTime(CpaUpstreamFailureEvent event)
    {
        Date eventTime = CpaLogTimestampParser.parse(event.getTimestamp());
        if (eventTime != null)
        {
            return eventTime;
        }
        log.warn("CLIProxyAPI 失败事件的时间无法解析，按入库时间记录：auth_index={}, timestamp={}",
                event.getAuthIndex(), event.getTimestamp());
        return new Date();
    }

    private String trimToNull(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
