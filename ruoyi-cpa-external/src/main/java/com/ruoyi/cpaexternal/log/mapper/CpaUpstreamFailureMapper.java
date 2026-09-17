package com.ruoyi.cpaexternal.log.mapper;

import java.util.Date;
import java.util.List;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailure;
import org.apache.ibatis.annotations.Param;

/** CLIProxyAPI 上游失败事件数据访问层。 */
public interface CpaUpstreamFailureMapper
{
    CpaUpstreamFailure selectById(Long failureId);

    List<CpaUpstreamFailure> selectList(CpaUpstreamFailure query);

    int insert(CpaUpstreamFailure failure);

    int deleteByIds(@Param("failureIds") Long[] failureIds);

    /** 删除指定时间之前的事件，返回本批实际删除条数；供保留期清理任务分批调用。 */
    int deleteBefore(@Param("beforeTime") Date beforeTime, @Param("limit") int limit);
}
