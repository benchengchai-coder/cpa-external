package com.ruoyi.web.facade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.invite.domain.AiInviteAffiliate;
import com.ruoyi.invite.domain.AiInviteConstants;
import com.ruoyi.invite.domain.AiInviteRebateRecord;
import com.ruoyi.invite.domain.ClaimResult;
import com.ruoyi.invite.domain.InviteConfig;
import com.ruoyi.invite.domain.InviteConfigUpdateRequest;
import com.ruoyi.invite.domain.vo.InviteConfigVO;
import com.ruoyi.invite.domain.vo.InviteInfoVO;
import com.ruoyi.invite.domain.vo.InviteOverviewVO;
import com.ruoyi.invite.service.IAiInviteService;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.recharge.domain.AiRechargeConstants;
import com.ruoyi.system.recharge.domain.AiRechargeRecord;
import com.ruoyi.system.recharge.service.IAiRechargeRecordService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 邀请返利服务编排 Facade（聚合层）。
 * <p>
 * 串联「纯邀请域」与「系统配置 / 用户余额 / 账户流水」三类副作用：
 * <ul>
 *   <li>读取系统参数组装 {@link InviteConfig} 传入纯域；</li>
 *   <li>{@link #claimRebate} 编排：纯域领取 → 加余额 → 回填余额快照 → 写账户流水 type=6；</li>
 *   <li>{@link #accrueRebate} 供 PayFacade/RedeemFacade 在入账后触发返利。</li>
 * </ul>
 */
@Component
public class InviteFacade
{
    private static final Logger log = LoggerFactory.getLogger(InviteFacade.class);

    private static final List<String> INVITE_CONFIG_KEYS = List.of(
            AiInviteConstants.CONFIG_INVITE_ENABLED,
            AiInviteConstants.CONFIG_INVITE_REBATE_RATE,
            AiInviteConstants.CONFIG_INVITE_FREEZE_HOURS,
            AiInviteConstants.CONFIG_INVITE_DURATION_DAYS,
            AiInviteConstants.CONFIG_INVITE_PER_INVITEE_CAP);

    @Autowired
    private IAiInviteService aiInviteService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private IAiRechargeRecordService rechargeRecordService;

    // ==================== 配置读取 → InviteConfig ====================

    /**
     * 确保用户存在邀请返利关系记录（透传，供注册流程调用）。
     */
    public void ensureAffiliate(Long userId)
    {
        aiInviteService.ensureAffiliate(userId);
    }

    /**
     * 从系统参数读取邀请返利运行时配置（容错：失败回退默认值）。
     */
    public InviteConfig loadConfig()
    {
        boolean enabled = "true".equalsIgnoreCase(
                configService.selectConfigByKey(AiInviteConstants.CONFIG_INVITE_ENABLED));
        BigDecimal globalRate = readBigDecimal(AiInviteConstants.CONFIG_INVITE_REBATE_RATE,
                AiInviteConstants.DEFAULT_REBATE_RATE);
        int freezeHours = readInt(AiInviteConstants.CONFIG_INVITE_FREEZE_HOURS,
                AiInviteConstants.DEFAULT_FREEZE_HOURS, AiInviteConstants.MAX_FREEZE_HOURS);
        int durationDays = readInt(AiInviteConstants.CONFIG_INVITE_DURATION_DAYS,
                AiInviteConstants.DEFAULT_DURATION_DAYS, AiInviteConstants.MAX_DURATION_DAYS);
        BigDecimal perInviteeCap = readBigDecimal(AiInviteConstants.CONFIG_INVITE_PER_INVITEE_CAP,
                AiInviteConstants.DEFAULT_PER_INVITEE_CAP);
        return new InviteConfig(enabled, globalRate, freezeHours, durationDays, perInviteeCap);
    }

    /**
     * 严格读取管理员邀请参数配置。
     * 任一配置缺失、重复或值非法时直接报错，避免管理页面展示与运行时不一致的数据。
     */
    public InviteConfigVO getAdminConfig()
    {
        Map<String, SysConfig> configs = loadRequiredConfigRecords();
        InviteConfigUpdateRequest request = parseAdminConfig(configs);
        return toConfigVO(request);
    }

    /**
     * 原子更新管理员邀请参数配置，并在事务提交后统一刷新系统参数缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public InviteConfigVO updateAdminConfig(InviteConfigUpdateRequest request, String operator)
    {
        validateAdminConfig(request);
        Map<String, SysConfig> configs = loadRequiredConfigRecords();

        updateConfigValue(configs.get(AiInviteConstants.CONFIG_INVITE_ENABLED),
                Boolean.TRUE.equals(request.getEnabled()) ? "true" : "false", operator);
        updateConfigValue(configs.get(AiInviteConstants.CONFIG_INVITE_REBATE_RATE),
                request.getRebateRate().toPlainString(), operator);
        updateConfigValue(configs.get(AiInviteConstants.CONFIG_INVITE_FREEZE_HOURS),
                String.valueOf(request.getFreezeHours()), operator);
        updateConfigValue(configs.get(AiInviteConstants.CONFIG_INVITE_DURATION_DAYS),
                String.valueOf(request.getDurationDays()), operator);
        updateConfigValue(configs.get(AiInviteConstants.CONFIG_INVITE_PER_INVITEE_CAP),
                request.getPerInviteeCap().toPlainString(), operator);

        refreshConfigCacheAfterCommit();
        return toConfigVO(request);
    }

    // ==================== 用户侧编排 ====================

    /**
     * 注册时绑定邀请人（由 RegisterFacade 调用）。
     */
    public void bindInviter(Long userId, String inviteCode)
    {
        aiInviteService.bindInviter(userId, inviteCode, loadConfig());
    }

    /**
     * 入账后触发返利（由 PayFacade/RedeemFacade 调用）。
     * 返利失败仅记日志，不阻断主流程（与原行为一致）。
     */
    public void accrueRebate(Long inviteeId, BigDecimal baseAmount, String sourceType, Long sourceId)
    {
        try
        {
            aiInviteService.accrueRebate(inviteeId, baseAmount, sourceType, sourceId, loadConfig());
        }
        catch (Exception e)
        {
            log.error("触发邀请返利失败：inviteeId={}, sourceType={}, sourceId={}", inviteeId, sourceType, sourceId, e);
        }
    }

    /**
     * 获取当前用户邀请返利信息（用户视角）。
     */
    public InviteInfoVO getInviteInfo(Long userId)
    {
        return aiInviteService.getInviteInfo(userId, loadConfig());
    }

    /**
     * 领取全部待领返利（编排：纯域领取 → 加余额 → 回填快照 → 写账户流水）。
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal claimRebate(Long inviterId)
    {
        // 1. 纯域领取（不加余额、不写账户流水，balanceAfter 暂留空）
        ClaimResult result = aiInviteService.claimRebate(inviterId, null);
        BigDecimal amount = result.getAmount();

        // 2. 加余额
        sysUserMapper.addUserBalance(inviterId, amount);

        // 3. 读真实余额快照并回填返利账本（保留审计信息）
        SysUser user = sysUserMapper.selectUserById(inviterId);
        BigDecimal balanceAfter = user == null ? null : user.getBalance();
        String username = user == null ? null : user.getUserName();
        aiInviteService.updateClaimBalanceAfter(result.getRebateRecordId(), balanceAfter);

        // 4. 写账户流水 type=6 邀请返利
        AiRechargeRecord rechargeRecord = new AiRechargeRecord();
        rechargeRecord.setUserId(inviterId);
        rechargeRecord.setUsername(username);
        rechargeRecord.setType(AiRechargeConstants.TYPE_INVITE_REBATE);
        rechargeRecord.setAmount(amount);
        rechargeRecord.setSourceId(result.getRebateRecordId());
        rechargeRecord.setSourceName("邀请返利");
        rechargeRecord.setStatus("0");
        rechargeRecord.setCreateBy(username);
        rechargeRecord.setRemark("邀请返利领取 " + amount + " 美元");
        rechargeRecordService.insertAiRechargeRecord(rechargeRecord);

        return amount;
    }

    // ==================== 管理员侧（透传，无需编排） ====================

    public List<AiInviteAffiliate> selectAffiliateList(AiInviteAffiliate query)
    {
        return aiInviteService.selectAffiliateList(query);
    }

    public List<AiInviteRebateRecord> selectRecordList(AiInviteRebateRecord query)
    {
        return aiInviteService.selectRecordList(query);
    }

    public InviteOverviewVO getOverview(Long userId)
    {
        return aiInviteService.getOverview(userId);
    }

    public int setRebateRate(Long userId, BigDecimal rebateRate)
    {
        return aiInviteService.setRebateRate(userId, rebateRate);
    }

    public String resetInviteCode(Long userId)
    {
        return aiInviteService.resetInviteCode(userId);
    }

    // ==================== 管理员配置严格读写 ====================

    private Map<String, SysConfig> loadRequiredConfigRecords()
    {
        List<SysConfig> records = configMapper.selectConfigListByKeys(INVITE_CONFIG_KEYS);
        Map<String, List<SysConfig>> grouped = new LinkedHashMap<>();
        for (String key : INVITE_CONFIG_KEYS)
        {
            grouped.put(key, new ArrayList<>());
        }
        if (records != null)
        {
            for (SysConfig record : records)
            {
                List<SysConfig> sameKeyRecords = grouped.get(record.getConfigKey());
                if (sameKeyRecords != null)
                {
                    sameKeyRecords.add(record);
                }
            }
        }

        List<String> invalidKeys = new ArrayList<>();
        Map<String, SysConfig> result = new LinkedHashMap<>();
        for (String key : INVITE_CONFIG_KEYS)
        {
            List<SysConfig> sameKeyRecords = grouped.get(key);
            int size = sameKeyRecords == null ? 0 : sameKeyRecords.size();
            if (size == 0)
            {
                invalidKeys.add(key + "（缺失）");
            }
            else if (size > 1)
            {
                invalidKeys.add(key + "（重复" + size + "条）");
            }
            else
            {
                result.put(key, sameKeyRecords.get(0));
            }
        }
        if (!invalidKeys.isEmpty())
        {
            throw new ServiceException("邀请参数配置异常：" + String.join("、", invalidKeys)
                    + "，请先执行邀请参数升级SQL");
        }
        return result;
    }

    private InviteConfigUpdateRequest parseAdminConfig(Map<String, SysConfig> configs)
    {
        InviteConfigUpdateRequest request = new InviteConfigUpdateRequest();
        try
        {
            String enabledValue = configs.get(AiInviteConstants.CONFIG_INVITE_ENABLED).getConfigValue();
            if (!"true".equalsIgnoreCase(enabledValue) && !"false".equalsIgnoreCase(enabledValue))
            {
                throw new IllegalArgumentException("总开关必须为true或false");
            }
            request.setEnabled(Boolean.valueOf(enabledValue));
            request.setRebateRate(new BigDecimal(
                    configs.get(AiInviteConstants.CONFIG_INVITE_REBATE_RATE).getConfigValue()));
            request.setFreezeHours(Integer.valueOf(
                    configs.get(AiInviteConstants.CONFIG_INVITE_FREEZE_HOURS).getConfigValue()));
            request.setDurationDays(Integer.valueOf(
                    configs.get(AiInviteConstants.CONFIG_INVITE_DURATION_DAYS).getConfigValue()));
            request.setPerInviteeCap(new BigDecimal(
                    configs.get(AiInviteConstants.CONFIG_INVITE_PER_INVITEE_CAP).getConfigValue()));
        }
        catch (Exception e)
        {
            throw new ServiceException("邀请参数配置值格式错误，请检查sys_config中的ai.invite.*参数")
                    .setDetailMessage(e.getMessage());
        }
        validateAdminConfig(request);
        return request;
    }

    private void validateAdminConfig(InviteConfigUpdateRequest request)
    {
        if (request == null || request.getEnabled() == null)
        {
            throw new ServiceException("邀请返利总开关不能为空");
        }
        BigDecimal rebateRate = request.getRebateRate();
        if (rebateRate == null
                || rebateRate.compareTo(AiInviteConstants.REBATE_RATE_MIN) < 0
                || rebateRate.compareTo(AiInviteConstants.REBATE_RATE_MAX) > 0)
        {
            throw new ServiceException("全局返利比例必须在0-100之间");
        }
        Integer freezeHours = request.getFreezeHours();
        if (freezeHours == null || freezeHours < 0 || freezeHours > AiInviteConstants.MAX_FREEZE_HOURS)
        {
            throw new ServiceException("返利冻结期必须在0-720小时之间");
        }
        Integer durationDays = request.getDurationDays();
        if (durationDays == null || durationDays < 0 || durationDays > AiInviteConstants.MAX_DURATION_DAYS)
        {
            throw new ServiceException("返利有效期必须在0-3650天之间");
        }
        BigDecimal perInviteeCap = request.getPerInviteeCap();
        if (perInviteeCap == null || perInviteeCap.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("单人返利上限不能小于0");
        }
    }

    private void updateConfigValue(SysConfig config, String value, String operator)
    {
        config.setConfigValue(value);
        config.setUpdateBy(operator);
        int rows = configMapper.updateConfig(config);
        if (rows != 1)
        {
            throw new ServiceException("更新邀请参数[" + config.getConfigKey() + "]失败");
        }
    }

    private InviteConfigVO toConfigVO(InviteConfigUpdateRequest request)
    {
        return new InviteConfigVO(Boolean.TRUE.equals(request.getEnabled()), request.getRebateRate(),
                request.getFreezeHours(), request.getDurationDays(), request.getPerInviteeCap());
    }

    private void refreshConfigCacheAfterCommit()
    {
        if (!TransactionSynchronizationManager.isSynchronizationActive())
        {
            configService.resetConfigCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
        {
            @Override
            public void afterCommit()
            {
                try
                {
                    configService.resetConfigCache();
                }
                catch (Exception e)
                {
                    log.error("邀请参数保存成功，但刷新系统参数缓存失败", e);
                }
            }
        });
    }

    // ==================== 配置读取小工具（容错） ====================

    private BigDecimal readBigDecimal(String key, BigDecimal defaultValue)
    {
        try
        {
            String value = configService.selectConfigByKey(key);
            if (StringUtils.isNotEmpty(value))
            {
                return new BigDecimal(value);
            }
        }
        catch (Exception e)
        {
            log.warn("读取参数[{}]失败，使用默认值", key, e);
        }
        return defaultValue;
    }

    private int readInt(String key, int defaultValue, int maxLimit)
    {
        try
        {
            String value = configService.selectConfigByKey(key);
            if (StringUtils.isNotEmpty(value))
            {
                int parsed = Integer.parseInt(value);
                if (parsed < 0)
                {
                    return defaultValue;
                }
                return Math.min(parsed, maxLimit);
            }
        }
        catch (Exception e)
        {
            log.warn("读取参数[{}]失败，使用默认值", key, e);
        }
        return defaultValue;
    }
}
