package com.ruoyi.web.controller.aigate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.invite.domain.AiInviteAffiliate;
import com.ruoyi.invite.domain.AiInviteRebateRecord;
import com.ruoyi.invite.domain.InviteConfigUpdateRequest;
import com.ruoyi.invite.domain.vo.InviteInfoVO;
import com.ruoyi.invite.domain.vo.InviteOverviewVO;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.web.facade.InviteFacade;

/**
 * 邀请返利管理
 * <p>
 * 用户视角：查看自己的邀请码、邀请链接、返利额度、被邀请人列表，并领取返利。
 * 管理员视角：查询邀请关系、返利流水，设置专属比例、重置邀请码。
 */
@RestController
@RequestMapping("/aigate/invite")
public class AiInviteController extends BaseController
{
    @Autowired
    private InviteFacade inviteFacade;

    // ==================== 用户接口 ====================

    /**
     * 获取当前用户邀请返利信息
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public AjaxResult getInfo()
    {
        InviteInfoVO vo = inviteFacade.getInviteInfo(getUserId());
        return success(vo);
    }

    /**
     * 领取全部待领返利
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/claim")
    public AjaxResult claim()
    {
        BigDecimal amount = inviteFacade.claimRebate(getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("amount", amount);
        return AjaxResult.success("成功领取返利 " + amount + " 美元", result);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员 - 邀请关系列表
     */
    @PreAuthorize("@ss.hasPermi('aigate:invite:list')")
    @GetMapping("/admin/affiliate/list")
    public TableDataInfo affiliateList(AiInviteAffiliate query)
    {
        startPage();
        List<AiInviteAffiliate> list = inviteFacade.selectAffiliateList(query);
        return getDataTable(list);
    }

    /**
     * 管理员 - 返利流水列表
     */
    @PreAuthorize("@ss.hasPermi('aigate:invite:list')")
    @GetMapping("/admin/rebate/list")
    public TableDataInfo rebateList(AiInviteRebateRecord query)
    {
        startPage();
        List<AiInviteRebateRecord> list = inviteFacade.selectRecordList(query);
        return getDataTable(list);
    }

    /**
     * 管理员 - 单用户概览
     */
    @PreAuthorize("@ss.hasPermi('aigate:invite:list')")
    @GetMapping("/admin/overview/{userId}")
    public AjaxResult overview(@PathVariable Long userId)
    {
        InviteOverviewVO vo = inviteFacade.getOverview(userId);
        return success(vo);
    }

    /**
     * 管理员 - 设置专属返利比例（rebateRate 为 null 表示清除，沿用全局）
     */
    @PreAuthorize("@ss.hasPermi('aigate:invite:rate')")
    @PutMapping("/admin/rate")
    public AjaxResult setRate(@RequestBody Map<String, Object> params)
    {
        Long userId = Long.valueOf(params.get("userId").toString());
        BigDecimal rebateRate = null;
        Object rate = params.get("rebateRate");
        if (rate != null && !"".equals(rate.toString()))
        {
            rebateRate = new BigDecimal(rate.toString());
        }
        inviteFacade.setRebateRate(userId, rebateRate);
        return success();
    }

    /**
     * 管理员 - 重置用户邀请码
     */
    @PreAuthorize("@ss.hasPermi('aigate:invite:resetCode')")
    @PutMapping("/admin/resetCode/{userId}")
    public AjaxResult resetCode(@PathVariable Long userId)
    {
        String newCode = inviteFacade.resetInviteCode(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("inviteCode", newCode);
        return AjaxResult.success("邀请码已重置", result);
    }

    /**
     * 管理员 - 查询邀请返利参数配置
     */
    @PreAuthorize("@ss.hasPermi('aigate:invite:config')")
    @GetMapping("/admin/config")
    public AjaxResult getAdminConfig()
    {
        return success(inviteFacade.getAdminConfig());
    }

    /**
     * 管理员 - 更新邀请返利参数配置
     */
    @PreAuthorize("@ss.hasPermi('aigate:invite:config')")
    @Log(title = "邀请参数配置", businessType = BusinessType.UPDATE)
    @PutMapping("/admin/config")
    public AjaxResult updateAdminConfig(@Validated @RequestBody InviteConfigUpdateRequest request)
    {
        return success(inviteFacade.updateAdminConfig(request, getUsername()));
    }
}

