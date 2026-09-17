package com.ruoyi.web.controller.aigate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionGrantRequest;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionPlan;
import com.ruoyi.cpaexternal.subscription.domain.AiSubscriptionRecord;
import com.ruoyi.cpaexternal.subscription.domain.AiUserSubscription;
import com.ruoyi.cpaexternal.subscription.service.IAiSubscriptionService;
import com.ruoyi.system.service.ISysUserService;

/** 订阅管理与用户自助订阅接口。 */
@RestController
@RequestMapping("/aigate/subscription")
public class AiSubscriptionController extends BaseController
{
    @Autowired
    private IAiSubscriptionService subscriptionService;

    @Autowired
    private ISysUserService sysUserService;

    @PreAuthorize("@ss.hasPermi('aigate:subscription:list')")
    @GetMapping("/plan/list")
    public TableDataInfo planList(AiSubscriptionPlan query)
    {
        startPage();
        List<AiSubscriptionPlan> list = subscriptionService.selectPlanList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:query')")
    @GetMapping("/plan/{planId}")
    public AjaxResult getPlan(@PathVariable Long planId)
    {
        return success(subscriptionService.selectPlanById(planId));
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:add')")
    @Log(title = "订阅套餐", businessType = BusinessType.INSERT)
    @PostMapping("/plan")
    public AjaxResult addPlan(@RequestBody AiSubscriptionPlan plan)
    {
        plan.setCreateBy(getUsername());
        return toAjax(subscriptionService.insertPlan(plan));
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:edit')")
    @Log(title = "订阅套餐", businessType = BusinessType.UPDATE)
    @PutMapping("/plan")
    public AjaxResult editPlan(@RequestBody AiSubscriptionPlan plan)
    {
        plan.setUpdateBy(getUsername());
        return toAjax(subscriptionService.updatePlan(plan));
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:remove')")
    @Log(title = "订阅套餐", businessType = BusinessType.DELETE)
    @DeleteMapping("/plan/{planIds}")
    public AjaxResult removePlan(@PathVariable Long[] planIds)
    {
        return toAjax(subscriptionService.deletePlanByIds(planIds));
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:edit')")
    @Log(title = "订阅套餐状态", businessType = BusinessType.UPDATE)
    @PutMapping("/plan/changeStatus")
    public AjaxResult changePlanStatus(@RequestBody AiSubscriptionPlan plan)
    {
        return toAjax(subscriptionService.changePlanStatus(plan.getPlanId(), plan.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:list')")
    @GetMapping("/admin/users/{userId}/subscriptions")
    public TableDataInfo adminUserSubscriptions(@PathVariable Long userId, AiUserSubscription query)
    {
        query.setUserId(userId);
        startPage();
        return getDataTable(subscriptionService.selectUserSubscriptionList(query));
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:grant')")
    @Log(title = "授予订阅", businessType = BusinessType.INSERT)
    @PostMapping("/admin/users/{userId}/grant")
    public AjaxResult grant(@PathVariable Long userId, @RequestBody AiSubscriptionGrantRequest request)
    {
        return success(subscriptionService.grantSubscription(userId, request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:cancel')")
    @Log(title = "取消订阅", businessType = BusinessType.UPDATE)
    @PutMapping("/admin/user-subscriptions/{subscriptionId}/cancel")
    public AjaxResult cancel(@PathVariable Long subscriptionId)
    {
        subscriptionService.cancelSubscription(subscriptionId, getUsername());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:remove')")
    @Log(title = "删除订阅", businessType = BusinessType.DELETE)
    @DeleteMapping("/admin/user-subscriptions/{subscriptionIds}")
    public AjaxResult removeUserSubscriptions(@PathVariable Long[] subscriptionIds)
    {
        return toAjax(subscriptionService.deleteUserSubscriptionByIds(subscriptionIds));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/plans")
    public AjaxResult userPlans()
    {
        return success(subscriptionService.selectUserVisiblePlans());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/self")
    public AjaxResult self()
    {
        SysUser user = sysUserService.selectUserById(getUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("billingPreference", user == null || StringUtils.isEmpty(user.getBillingPreference())
            ? "subscription_first" : user.getBillingPreference());
        data.put("subscriptions", subscriptionService.selectSelfSubscriptions(getUserId()));
        return success(data);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/self/preference")
    public AjaxResult updatePreference(@RequestBody Map<String, String> params)
    {
        subscriptionService.updateBillingPreference(getUserId(), params.get("billingPreference"));
        return success();
    }

    @PreAuthorize("isAuthenticated()")
    @Log(title = "余额购买订阅", businessType = BusinessType.INSERT)
    @PostMapping("/balance/purchase")
    public AjaxResult balancePurchase(@RequestBody Map<String, Object> params)
    {
        Object planIdValue = params.get("planId");
        if (planIdValue == null)
        {
            return error("套餐ID不能为空");
        }
        Long planId = Long.valueOf(planIdValue.toString());
        return success(subscriptionService.purchaseWithBalance(getUserId(), getUsername(), planId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/record/list")
    public TableDataInfo recordList(AiSubscriptionRecord query)
    {
        query.setUserId(getUserId());
        startPage();
        return getDataTable(subscriptionService.selectRecordList(query));
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:config')")
    @GetMapping("/config")
    public AjaxResult config()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("balancePurchaseEnabled", subscriptionService.getBalancePurchaseEnabled());
        return success(data);
    }

    @PreAuthorize("@ss.hasPermi('aigate:subscription:config')")
    @Log(title = "订阅配置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public AjaxResult updateConfig(@RequestBody Map<String, String> params)
    {
        subscriptionService.updateBalancePurchaseEnabled(params.get("balancePurchaseEnabled"), getUsername());
        return success();
    }
}
