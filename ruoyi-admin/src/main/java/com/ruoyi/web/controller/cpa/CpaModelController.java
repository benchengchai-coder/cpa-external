package com.ruoyi.web.controller.cpa;

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
import com.ruoyi.cpaexternal.model.domain.CpaModel;
import com.ruoyi.cpaexternal.model.service.ICpaModelService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/** 模型管理。 */
@RestController
@RequestMapping("/aigate/model")
public class CpaModelController extends BaseController
{
    @Autowired
    private ICpaModelService modelService;

    @PreAuthorize("@ss.hasPermi('aigate:model:list')")
    @GetMapping("/list")
    public TableDataInfo list(CpaModel query)
    {
        startPage();
        List<CpaModel> list = modelService.selectList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aigate:model:query')")
    @GetMapping("/{modelId}")
    public AjaxResult getInfo(@PathVariable Long modelId)
    {
        return success(modelService.selectById(modelId));
    }

    @PreAuthorize("@ss.hasPermi('aigate:model:add')")
    @Log(title = "模型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CpaModel model)
    {
        model.setCreateBy(getUsername());
        return toAjax(modelService.insert(model));
    }

    @PreAuthorize("@ss.hasPermi('aigate:model:edit')")
    @Log(title = "模型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CpaModel model)
    {
        model.setUpdateBy(getUsername());
        return toAjax(modelService.update(model));
    }

    @PreAuthorize("@ss.hasPermi('aigate:model:remove')")
    @Log(title = "模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{modelIds}")
    public AjaxResult remove(@PathVariable Long[] modelIds)
    {
        return toAjax(modelService.deleteByIds(modelIds));
    }

    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        CpaModel query = new CpaModel();
        query.setStatus("0");
        return success(modelService.selectList(query));
    }
}
