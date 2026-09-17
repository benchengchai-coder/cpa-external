package com.ruoyi.cpaexternal.log.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailure;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailureEvent;

/** CLIProxyAPI 上游失败事件服务：errors 通道事件落库与管理端查询。 */
public interface ICpaUpstreamFailureService
{
    /** 把一条 errors 通道事件转换为实体并追加落库。 */
    void record(CpaUpstreamFailureEvent event);

    CpaUpstreamFailure selectById(Long failureId);

    List<CpaUpstreamFailure> selectList(CpaUpstreamFailure query);

    int deleteByIds(Long[] failureIds);

    /** 删除指定时间之前的事件，返回本批实际删除条数。 */
    int deleteBefore(Date beforeTime, int limit);
}
