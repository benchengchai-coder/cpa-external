package com.ruoyi.web.controller.aigate;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.cpaexternal.platform.domain.AiPlatform;
import com.ruoyi.cpaexternal.platform.service.IAiPlatformService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * AI平台管理
 */
@RestController
@RequestMapping("/aigate/platform")
public class AiPlatformController extends BaseController
{
    @Autowired
    private IAiPlatformService aiPlatformService;

    /**
     * 查询AI平台列表
     */
    @PreAuthorize("@ss.hasPermi('aigate:platform:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiPlatform aiPlatform)
    {
        startPage();
        List<AiPlatform> list = aiPlatformService.selectAiPlatformList(aiPlatform);
        return getDataTable(list);
    }

    /**
     * 获取AI平台详细信息
     */
    @PreAuthorize("@ss.hasPermi('aigate:platform:query')")
    @GetMapping("/{platformId}")
    public AjaxResult getInfo(@PathVariable Long platformId)
    {
        return success(aiPlatformService.selectAiPlatformById(platformId));
    }

    /**
     * 新增AI平台
     */
    @PreAuthorize("@ss.hasPermi('aigate:platform:add')")
    @Log(title = "AI平台", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiPlatform aiPlatform)
    {
        return toAjax(aiPlatformService.insertAiPlatform(aiPlatform));
    }

    /**
     * 修改AI平台
     */
    @PreAuthorize("@ss.hasPermi('aigate:platform:edit')")
    @Log(title = "AI平台", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiPlatform aiPlatform)
    {
        return toAjax(aiPlatformService.updateAiPlatform(aiPlatform));
    }

    /**
     * 删除AI平台
     */
    @PreAuthorize("@ss.hasPermi('aigate:platform:remove')")
    @Log(title = "AI平台", businessType = BusinessType.DELETE)
    @DeleteMapping("/{platformIds}")
    public AjaxResult remove(@PathVariable Long[] platformIds)
    {
        return toAjax(aiPlatformService.deleteAiPlatformByIds(platformIds));
    }
}
