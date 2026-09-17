package com.ruoyi.web.controller.aigate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingRecordDetailVO;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingRecordQueryService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * CLIProxyAPI 账单详情查询。
 */
@RestController
@RequestMapping("/aigate/billing/records")
public class CpaBillingRecordController extends BaseController
{
    @Autowired
    private ICpaBillingRecordQueryService billingRecordQueryService;

    /**
     * 查询当前用户指定调用日志对应的账单。
     */
    @PreAuthorize("@ss.hasPermi('aigate:usageLog:query')")
    @GetMapping("/by-log/{logId}")
    public AjaxResult getUserBillingByLogId(@PathVariable Long logId)
    {
        CpaBillingRecordDetailVO detail = billingRecordQueryService
            .selectUserBillingByLogId(logId, getUserId());
        return AjaxResult.success(detail);
    }

    /**
     * 管理员查询指定调用日志对应的账单。
     */
    @PreAuthorize("@ss.hasPermi('aigate:log:query')")
    @GetMapping("/admin/by-log/{logId}")
    public AjaxResult getAdminBillingByLogId(@PathVariable Long logId)
    {
        CpaBillingRecordDetailVO detail = billingRecordQueryService.selectAdminBillingByLogId(logId);
        return AjaxResult.success(detail);
    }
}
