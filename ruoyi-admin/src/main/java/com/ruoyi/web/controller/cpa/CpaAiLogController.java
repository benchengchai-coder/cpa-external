package com.ruoyi.web.controller.cpa;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingRecordQueryService;
import com.ruoyi.cpaexternal.log.domain.CpaAiLog;
import com.ruoyi.cpaexternal.log.domain.CpaAiLogPayload;
import com.ruoyi.cpaexternal.log.domain.vo.CpaLogUserSummaryVO;
import com.ruoyi.cpaexternal.log.service.ICpaAiLogService;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionConstants;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.service.IAiSubscriptionService;
import com.ruoyi.system.service.ISysUserService;

/** CLIProxyAPI 日志管理及日志接收接口。 */
@RestController
@RequestMapping("/aigate/log")
public class CpaAiLogController extends BaseController
{
    @Autowired
    private ICpaAiLogService aiLogService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IAiSubscriptionService subscriptionService;

    @Autowired
    private ICpaBillingRecordQueryService billingRecordQueryService;

    @PreAuthorize("@ss.hasPermi('aigate:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(CpaAiLog query)
    {
        startPage();
        List<CpaAiLog> list = aiLogService.selectList(query);
        list.forEach(this::maskApiKey);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aigate:log:query')")
    @GetMapping("/{logId}")
    public AjaxResult getInfo(@PathVariable Long logId)
    {
        CpaAiLog log = aiLogService.selectById(logId);
        maskApiKey(log);
        return success(log);
    }

    /** 查询调用日志关联用户的关键信息（基本信息、钱包余额、生效订阅）。 */
    @PreAuthorize("@ss.hasPermi('aigate:log:query')")
    @GetMapping("/users/{userId}/summary")
    public AjaxResult getUserSummary(@PathVariable Long userId)
    {
        SysUser user = userService.selectUserById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在");
        }

        AiUserSubscription query = new AiUserSubscription();
        query.setUserId(userId);
        query.setStatus(AiSubscriptionConstants.SUB_STATUS_ACTIVE);
        List<AiUserSubscription> subscriptions = subscriptionService.selectUserSubscriptionList(query);
        CpaLogUserSummaryVO summary = CpaLogUserSummaryVO.from(user, subscriptions);
        CpaBillingAmountSummaryVO amountSummary = billingRecordQueryService.selectUserAmountSummary(userId);
        summary.setTotalChargedAmount(amountSummary.getTotalChargedAmount());
        summary.setTotalUncoveredAmount(amountSummary.getTotalUncoveredAmount());
        return success(summary);
    }

    @PreAuthorize("@ss.hasPermi('aigate:usageLog:list')")
    @GetMapping("/usage-list")
    public TableDataInfo usageList(CpaAiLog query)
    {
        query.setUserId(getUserId());
        startPage();
        List<CpaAiLog> list = aiLogService.selectList(query);
        list.forEach(this::maskApiKey);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aigate:usageLog:query')")
    @GetMapping("/usage/{logId}")
    public AjaxResult getUsageInfo(@PathVariable Long logId)
    {
        CpaAiLog log = aiLogService.selectById(logId);
        if (log == null || !getUserId().equals(log.getUserId()))
        {
            throw new ServiceException("使用记录不存在");
        }
        maskApiKey(log);
        return success(log);
    }

    /**
     * 接收 CLIProxyAPI 产生的单条日志。
     * 该接口只落库，不转发请求；调用方使用本项目已有登录态进行认证。
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping({"/ingest", "/import"})
    public AjaxResult ingest(@RequestBody CpaAiLogPayload payload)
    {
        CpaAiLog log = aiLogService.ingest(payload);
        maskApiKey(log);
        return success(log);
    }

    @PreAuthorize("@ss.hasPermi('aigate:log:remove')")
    @Log(title = "AI调用日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(aiLogService.deleteByIds(logIds));
    }

    private void maskApiKey(CpaAiLog log)
    {
        if (log == null || log.getApiKey() == null || log.getApiKey().length() <= 10)
        {
            return;
        }
        String value = log.getApiKey();
        log.setApiKey(value.substring(0, 6) + "******" + value.substring(value.length() - 4));
    }
}
