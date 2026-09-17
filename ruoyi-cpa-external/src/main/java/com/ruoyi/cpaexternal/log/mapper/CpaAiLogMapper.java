package com.ruoyi.cpaexternal.log.mapper;

import java.util.List;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;
import org.apache.ibatis.annotations.Param;

/** CLIProxyAPI 日志数据访问层。 */
public interface CpaAiLogMapper
{
    CpaAiLog selectById(Long logId);

    CpaAiLog selectByRequestId(String requestId);

    List<CpaAiLog> selectList(CpaAiLog query);

    int insert(CpaAiLog log);

    /** 凭据重试场景下用后续 usage 记录覆盖已入库的失败记录（仅 failed=1 时生效）。 */
    int updateUsageByRequestId(CpaAiLog log);

    int deleteByIds(@Param("logIds") Long[] logIds);
}
