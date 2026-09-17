package com.ruoyi.web.controller.aigate;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementFailedQuery;
import com.ruoyi.cpaexternal.billing.domain.CpaBillingSettlementWriteOffRequest;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingSettlementFailedVO;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingSettlementService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * CLIProxyAPI 异步结算管理。
 */
@RestController
@RequestMapping("/aigate/billing/settlement")
public class CpaBillingSettlementController extends BaseController
{
    @Autowired
    private ICpaBillingSettlementService billingSettlementService;

    /**
     * 查询尚未人工处置的结算失败任务。
     */
    @PreAuthorize("@ss.hasPermi('aigate:billingSettlement:list')")
    @GetMapping("/failed/list")
    public TableDataInfo failedList(CpaBillingSettlementFailedQuery query)
    {
        startPage();
        List<CpaBillingSettlementFailedVO> list = billingSettlementService.selectFailedTaskList(query);
        return getDataTable(list);
    }

    /**
     * 查询尚未人工处置的结算失败任务数量。
     */
    @PreAuthorize("@ss.hasPermi('aigate:billingSettlement:list')")
    @GetMapping("/failed/count")
    public AjaxResult failedCount()
    {
        return success(billingSettlementService.countFailedTasks());
    }

    /**
     * 将已超过自动重试上限的结算记录重新放回队列。
     */
    @PreAuthorize("@ss.hasPermi('aigate:billingSettlement:retry')")
    @Log(title = "CLIProxyAPI计费结算重试", businessType = BusinessType.UPDATE)
    @PostMapping("/{requestId}/retry")
    public AjaxResult retry(@PathVariable String requestId)
    {
        int rows = billingSettlementService.retryFailedTask(requestId);
        if (rows != 1)
        {
            return error("未找到可重试的结算失败记录");
        }
        return success("结算任务已重新进入重试队列");
    }

    /**
     * 管理员明确放弃追收并释放该请求的全部预占额度。
     */
    @PreAuthorize("@ss.hasPermi('aigate:billingSettlement:writeOff')")
    @Log(title = "CLIProxyAPI计费结算核销", businessType = BusinessType.UPDATE)
    @PostMapping("/{requestId}/write-off")
    public AjaxResult writeOff(@PathVariable String requestId,
                               @Validated @RequestBody CpaBillingSettlementWriteOffRequest request)
    {
        int rows = billingSettlementService.writeOffFailedTask(
            requestId, request.getReason(), getUsername());
        if (rows != 1)
        {
            return error("未找到可核销的结算失败记录");
        }
        return success("结算失败记录已核销，相关冻结额度已释放");
    }
}
