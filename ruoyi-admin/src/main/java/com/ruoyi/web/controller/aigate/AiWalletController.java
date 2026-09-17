package com.ruoyi.web.controller.aigate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ruoyi.cpaexternal.billing.domain.vo.CpaBillingAmountSummaryVO;
import com.ruoyi.cpaexternal.billing.service.ICpaBillingRecordQueryService;
import com.ruoyi.redeem.domain.RedeemResult;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.recharge.domain.AiRechargeConstants;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;
import com.ruoyi.system.recharge.service.IAiRechargeRecordService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.web.facade.RedeemFacade;

/**
 * 钱包及充值记录管理。
 *
 * <p>本控制器只提供独立的钱包、充值记录和用户管理能力，不参与 AI 请求转发。</p>
 */
@RestController
@RequestMapping("/aigate/wallet")
public class AiWalletController extends BaseController
{
    @Autowired
    private RedeemFacade redeemFacade;

    @Autowired
    private IAiRechargeRecordService rechargeRecordService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ICpaBillingRecordQueryService billingRecordQueryService;

    /** 获取当前用户钱包信息。 */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public AjaxResult getWalletInfo()
    {
        SysUser user = sysUserService.selectUserById(getUserId());
        if (user == null)
        {
            return error("用户不存在");
        }
        return success(walletInfo(user));
    }

    /** 兑换充值码。 */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/redeem")
    public AjaxResult redeem(@RequestBody Map<String, String> params)
    {
        String redemptionKey = params == null ? null : params.get("redemptionKey");
        if (redemptionKey == null || redemptionKey.isBlank())
        {
            return error("兑换码不能为空");
        }
        RedeemResult result = redeemFacade.redeem(redemptionKey, getUserId(), getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("quota", result.getQuota());
        data.put("codeName", result.getCodeName());
        return AjaxResult.success("兑换成功，已充值 " + result.getQuota() + " 美元", data);
    }

    /** 查询当前用户充值记录。 */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/billing/list")
    public TableDataInfo billingList(AiRechargeRecord query)
    {
        query.setUserId(getUserId());
        startPage();
        List<AiRechargeRecord> list = rechargeRecordService.selectAiRechargeRecordList(query);
        return getDataTable(list);
    }

    /** 管理员获取指定用户钱包信息。 */
    @PreAuthorize("@ss.hasPermi('aigate:wallet:admin')")
    @GetMapping("/admin/info/{userId}")
    public AjaxResult adminGetWalletInfo(@PathVariable Long userId)
    {
        SysUser user = sysUserService.selectUserById(userId);
        if (user == null)
        {
            return error("用户不存在");
        }
        return success(walletInfo(user));
    }

    /** 管理员调整用户余额，type 支持 set、add、sub。 */
    @PreAuthorize("@ss.hasPermi('aigate:wallet:admin')")
    @Log(title = "用户余额调整", businessType = BusinessType.UPDATE)
    @PostMapping("/admin/adjust")
    public AjaxResult adminAdjustBalance(@RequestBody Map<String, Object> params)
    {
        if (params == null || params.get("userId") == null || params.get("type") == null || params.get("amount") == null)
        {
            return error("调整参数不能为空");
        }
        Long userId = Long.valueOf(params.get("userId").toString());
        String type = params.get("type").toString();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        if (amount.signum() < 0)
        {
            return error("金额不能为负数");
        }

        SysUser user = sysUserService.selectUserById(userId);
        if (user == null)
        {
            return error("用户不存在");
        }
        int rows;
        String remark;
        if ("set".equals(type))
        {
            rows = sysUserMapper.setUserBalance(userId, amount);
            remark = "管理员设置余额为 " + amount;
        }
        else if ("add".equals(type))
        {
            rows = sysUserMapper.addUserBalance(userId, amount);
            remark = "管理员添加余额 " + amount;
        }
        else if ("sub".equals(type))
        {
            rows = sysUserMapper.subUserBalance(userId, amount);
            if (rows == 0)
            {
                return error("可用余额不足");
            }
            remark = "管理员减少余额 " + amount;
        }
        else
        {
            return error("无效的操作类型");
        }
        if (rows <= 0)
        {
            return error("余额调整失败");
        }

        AiRechargeRecord record = new AiRechargeRecord();
        record.setUserId(userId);
        record.setUsername(user.getUserName());
        record.setType(AiRechargeConstants.TYPE_ADMIN_ADJUST);
        record.setAmount(amount);
        record.setSourceName(getUsername());
        record.setStatus("0");
        record.setRemark(remark);
        rechargeRecordService.insertAiRechargeRecord(record);
        return success("操作成功");
    }

    /** 管理员调整用户并发上限。 */
    @PreAuthorize("@ss.hasPermi('aigate:wallet:admin')")
    @Log(title = "用户AI并发调整", businessType = BusinessType.UPDATE)
    @PutMapping("/admin/concurrency")
    public AjaxResult adminUpdateConcurrency(@RequestBody Map<String, Object> params)
    {
        if (params == null || params.get("userId") == null || params.get("aiConcurrencyLimit") == null)
        {
            return error("并发上限参数不能为空");
        }
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer limit = Integer.valueOf(params.get("aiConcurrencyLimit").toString());
        if (limit < 0 || limit > 1000)
        {
            return error("AI并发上限必须在0到1000之间");
        }
        if (sysUserService.selectUserById(userId) == null)
        {
            return error("用户不存在");
        }
        int rows = sysUserMapper.updateUserAiConcurrencyLimit(userId, limit, getUsername());
        if (rows <= 0)
        {
            return error("更新用户AI并发上限失败");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("aiConcurrencyLimit", limit);
        return success(result);
    }

    /** 管理员查询指定用户充值记录。 */
    @PreAuthorize("@ss.hasPermi('aigate:wallet:admin')")
    @GetMapping("/admin/recharge/list/{userId}")
    public TableDataInfo adminRechargeList(@PathVariable Long userId, AiRechargeRecord query)
    {
        query.setUserId(userId);
        startPage();
        List<AiRechargeRecord> list = rechargeRecordService.selectAiRechargeRecordList(query);
        return getDataTable(list);
    }

    private Map<String, Object> walletInfo(SysUser user)
    {
        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getUserId());
        info.put("username", user.getUserName());
        info.put("nickName", user.getNickName());
        info.put("balance", user.getBalance());
        info.put("frozenBalance", nvl(user.getFrozenBalance()));
        info.put("availableBalance", nvl(user.getBalance()).subtract(nvl(user.getFrozenBalance())).max(BigDecimal.ZERO));
        info.put("usedBalance", user.getUsedBalance());
        info.put("requestCount", user.getRequestCount());
        info.put("aiConcurrencyLimit", user.getAiConcurrencyLimit());
        info.put("activeRequestCount", user.getActiveRequestCount() == null ? 0 : user.getActiveRequestCount());
        info.put("billingMultiplier", user.getBillingMultiplier());
        CpaBillingAmountSummaryVO summary = billingRecordQueryService.selectUserAmountSummary(user.getUserId());
        info.put("totalChargedAmount", summary.getTotalChargedAmount());
        info.put("totalUncoveredAmount", summary.getTotalUncoveredAmount());
        return info;
    }

    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }
}
