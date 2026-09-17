package com.ruoyi.web.controller.cpa;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cpaexternal.log.domain.CpaUpstreamFailure;
import com.ruoyi.cpaexternal.log.service.ICpaUpstreamFailureService;

/** CLIProxyAPI 上游失败事件管理接口。 */
@RestController
@RequestMapping("/aigate/upstreamFailure")
public class CpaUpstreamFailureController extends BaseController
{
    @Autowired
    private ICpaUpstreamFailureService upstreamFailureService;

    @PreAuthorize("@ss.hasPermi('aigate:upstreamFailure:list')")
    @GetMapping("/list")
    public TableDataInfo list(CpaUpstreamFailure query)
    {
        startPage();
        List<CpaUpstreamFailure> list = upstreamFailureService.selectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aigate:upstreamFailure:query')")
    @GetMapping("/{failureId}")
    public AjaxResult getInfo(@PathVariable Long failureId)
    {
        return success(upstreamFailureService.selectById(failureId));
    }

    @PreAuthorize("@ss.hasPermi('aigate:upstreamFailure:remove')")
    @Log(title = "上游失败事件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{failureIds}")
    public AjaxResult remove(@PathVariable Long[] failureIds)
    {
        return toAjax(upstreamFailureService.deleteByIds(failureIds));
    }
}
