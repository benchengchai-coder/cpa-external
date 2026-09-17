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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.redeem.domain.AiRedemptionCode;
import com.ruoyi.web.facade.RedeemFacade;

/**
 * AI兑换码管理
 */
@RestController
@RequestMapping("/aigate/redemptionCode")
public class AiRedemptionCodeController extends BaseController
{
    @Autowired
    private RedeemFacade redeemFacade;

    @PreAuthorize("@ss.hasPermi('aigate:redemptionCode:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiRedemptionCode aiRedemptionCode)
    {
        startPage();
        List<AiRedemptionCode> list = redeemFacade.selectAiRedemptionCodeList(aiRedemptionCode);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aigate:redemptionCode:query')")
    @GetMapping("/{codeId}")
    public AjaxResult getInfo(@PathVariable Long codeId)
    {
        return success(redeemFacade.selectAiRedemptionCodeById(codeId));
    }

    @PreAuthorize("@ss.hasPermi('aigate:redemptionCode:add')")
    @Log(title = "兑换码", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AiRedemptionCode aiRedemptionCode)
    {
        aiRedemptionCode.setCreateBy(getUsername());
        return toAjax(redeemFacade.insertAiRedemptionCode(aiRedemptionCode));
    }

    @PreAuthorize("@ss.hasPermi('aigate:redemptionCode:add')")
    @Log(title = "兑换码", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batchAdd(@RequestBody AiRedemptionCode aiRedemptionCode)
    {
        aiRedemptionCode.setCreateBy(getUsername());
        return toAjax(redeemFacade.batchInsertAiRedemptionCode(aiRedemptionCode));
    }

    @PreAuthorize("@ss.hasPermi('aigate:redemptionCode:edit')")
    @Log(title = "兑换码", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AiRedemptionCode aiRedemptionCode)
    {
        aiRedemptionCode.setUpdateBy(getUsername());
        return toAjax(redeemFacade.updateAiRedemptionCode(aiRedemptionCode));
    }

    @PreAuthorize("@ss.hasPermi('aigate:redemptionCode:remove')")
    @Log(title = "兑换码", businessType = BusinessType.DELETE)
    @DeleteMapping("/{codeIds}")
    public AjaxResult remove(@PathVariable Long[] codeIds)
    {
        return toAjax(redeemFacade.deleteAiRedemptionCodeByIds(codeIds));
    }
}

