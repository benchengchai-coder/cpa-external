package com.ruoyi.cpaexternal.log.service;

import java.util.List;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogPayload;

/** CLIProxyAPI 日志服务。 */
public interface ICpaAiLogService
{
    CpaAiLog selectById(Long logId);

    List<CpaAiLog> selectList(CpaAiLog query);

    CpaAiLog ingest(CpaAiLogPayload payload);

    int deleteByIds(Long[] logIds);
}
