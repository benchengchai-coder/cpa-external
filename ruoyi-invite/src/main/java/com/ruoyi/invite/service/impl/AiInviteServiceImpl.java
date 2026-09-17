package com.ruoyi.invite.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.invite.domain.AiInviteAffiliate;
import com.ruoyi.invite.domain.AiInviteConstants;
import com.ruoyi.invite.domain.AiInviteRebateRecord;
import com.ruoyi.invite.domain.ClaimResult;
import com.ruoyi.invite.domain.InviteConfig;
import com.ruoyi.invite.domain.vo.InviteInfoVO;
import com.ruoyi.invite.domain.vo.InviteeVO;
import com.ruoyi.invite.domain.vo.InviteOverviewVO;
import com.ruoyi.invite.mapper.AiInviteAffiliateMapper;
import com.ruoyi.invite.mapper.AiInviteRebateRecordMapper;
import com.ruoyi.invite.service.IAiInviteService;
import com.ruoyi.common.exception.ServiceException;

/**
 * 邀请返利服务实现（纯邀请域）。
 * <p>
 * 核心业务规则：
 * <ul>
 *   <li>总开关关闭：绑定静默跳过、返利返回 0，绝不阻断注册/充值。</li>
 *   <li>绑定幂等：已绑定→静默成功；防自邀；条件 UPDATE 防并发。</li>
 *   <li>返利计算：round(base × 比例/100)，比例 clamp [0,100]，专属优先全局。</li>
 *   <li>单人上限：cap>0 时 min(rebate, cap-已累计)，已达上限返 0。</li>
 *   <li>冻结期：freezeHours>0 进 frozen，领取时统一解冻。</li>
 *   <li>订单级幂等：流水表 uk_source + INSERT IGNORE。</li>
 * </ul>
 * <p>
 * 本实现不读取系统参数、不操作用户余额、不写账户流水；
 * 运行时配置由 {@link InviteConfig} 传入，余额/流水副作用由聚合层编排。
 */
@Service
public class AiInviteServiceImpl implements IAiInviteService
{
    private static final Logger log = LoggerFactory.getLogger(AiInviteServiceImpl.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Pattern INVITE_CODE_PATTERN =
            Pattern.compile(AiInviteConstants.INVITE_CODE_PATTERN);

    /** 被邀请人列表最多返回条数 */
    private static final int MAX_INVITEE_LIST = 100;

    @Autowired
    private AiInviteAffiliateMapper affiliateMapper;

    @Autowired
    private AiInviteRebateRecordMapper rebateRecordMapper;

    // ==================== 用户侧 ====================

    @Override
    public AiInviteAffiliate ensureAffiliate(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        AiInviteAffiliate affiliate = affiliateMapper.selectByUserId(userId);
        if (affiliate != null)
        {
            return affiliate;
        }
        // 新建并生成邀请码（冲突重试）
        affiliate = new AiInviteAffiliate();
        affiliate.setUserId(userId);
        affiliate.setInviteCode(generateUniqueInviteCode());
        affiliate.setInviteCount(0);
        affiliate.setPendingRebate(BigDecimal.ZERO);
        affiliate.setFrozenRebate(BigDecimal.ZERO);
        affiliate.setHistoryRebate(BigDecimal.ZERO);
        affiliate.setCreateTime(new Date());
        try
        {
            affiliateMapper.insertAffiliate(affiliate);
        }
        catch (Exception e)
        {
            // 并发下可能已被其他线程创建，重新查询
            log.debug("创建邀请返利关系可能冲突，重新查询：userId={}", userId);
            affiliate = affiliateMapper.selectByUserId(userId);
        }
        return affiliate;
    }

    @Override
    public void bindInviter(Long userId, String rawCode, InviteConfig config)
    {
        if (userId == null)
        {
            return;
        }
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        // 空码：不绑定，静默成功
        if (code.isEmpty())
        {
            return;
        }
        // 总开关关闭：静默跳过（不阻断注册）
        if (config != null && !config.isEnabled())
        {
            return;
        }
        // 格式校验
        if (!INVITE_CODE_PATTERN.matcher(code).matches())
        {
            log.warn("邀请码格式非法，userId={}, code={}", userId, code);
            return;
        }

        AiInviteAffiliate self = ensureAffiliate(userId);
        if (self == null)
        {
            return;
        }
        // 已绑定：幂等返回
        if (self.getInviterId() != null)
        {
            return;
        }

        AiInviteAffiliate inviter = affiliateMapper.selectByCode(code);
        if (inviter == null)
        {
            log.warn("邀请码不存在，userId={}, code={}", userId, code);
            return;
        }
        // 防自邀
        if (inviter.getUserId().equals(userId))
        {
            log.warn("禁止自邀，userId={}", userId);
            return;
        }

        // 条件绑定（仅当 inviter_id 为空时生效，并发幂等）
        int bound = affiliateMapper.bindInviter(userId, inviter.getUserId());
        if (bound > 0)
        {
            // 邀请人累计邀请人数 +1
            affiliateMapper.incrementInviteCount(inviter.getUserId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal accrueRebate(Long inviteeId, BigDecimal baseAmount, String sourceType, Long sourceId,
                                   InviteConfig config)
    {
        // 入参防御
        if (inviteeId == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0)
        {
            return BigDecimal.ZERO;
        }
        // 总开关
        if (config == null || !config.isEnabled())
        {
            return BigDecimal.ZERO;
        }

        AiInviteAffiliate invitee = affiliateMapper.selectByUserId(inviteeId);
        if (invitee == null || invitee.getInviterId() == null)
        {
            // 无邀请人，无返利
            return BigDecimal.ZERO;
        }
        Long inviterId = invitee.getInviterId();

        // 有效期检查：从被邀请人关系创建时间起算
        int durationDays = config.getDurationDays();
        if (durationDays > 0 && invitee.getCreateTime() != null)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(invitee.getCreateTime());
            cal.add(Calendar.DAY_OF_MONTH, durationDays);
            if (new Date().after(cal.getTime()))
            {
                return BigDecimal.ZERO;
            }
        }

        // 比例解析：专属优先全局，clamp [0,100]
        BigDecimal rebateRate = resolveRebateRate(inviterId, config);

        // 返利公式：round(base × 比例/100, scale)
        BigDecimal rebate = baseAmount.multiply(rebateRate)
                .divide(AiInviteConstants.REBATE_RATE_MAX, AiInviteConstants.AMOUNT_SCALE, RoundingMode.HALF_UP);
        if (rebate.compareTo(BigDecimal.ZERO) <= 0)
        {
            return BigDecimal.ZERO;
        }

        // 单人上限截断
        BigDecimal perInviteeCap = config.getPerInviteeCap();
        if (perInviteeCap != null && perInviteeCap.compareTo(BigDecimal.ZERO) > 0)
        {
            BigDecimal accrued = rebateRecordMapper.sumAccruedByInvitee(inviterId, inviteeId);
            BigDecimal remaining = perInviteeCap.subtract(accrued);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0)
            {
                return BigDecimal.ZERO;
            }
            if (rebate.compareTo(remaining) > 0)
            {
                rebate = remaining.setScale(AiInviteConstants.AMOUNT_SCALE, RoundingMode.HALF_UP);
            }
        }

        // 冻结期处理
        int freezeHours = config.getFreezeHours();
        Date frozenUntil = null;
        BigDecimal pendingDelta = rebate;
        BigDecimal frozenDelta = BigDecimal.ZERO;
        if (freezeHours > 0)
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR_OF_DAY, freezeHours);
            frozenUntil = cal.getTime();
            pendingDelta = BigDecimal.ZERO;
            frozenDelta = rebate;
        }

        // 写产生返利流水（INSERT IGNORE 幂等：source_type/source_id 防重）。
        // 先写流水，若重复则整笔回滚，避免额度误加。
        AiInviteRebateRecord record = new AiInviteRebateRecord();
        record.setInviterId(inviterId);
        record.setInviteeId(inviteeId);
        record.setAction(AiInviteConstants.ACTION_ACCRUE);
        record.setAmount(rebate);
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setFrozenUntil(frozenUntil);
        int inserted = rebateRecordMapper.insertAccrueIgnore(record);
        if (inserted == 0)
        {
            // 该来源已产生过返利（重复回调），幂等返回 0，不报错
            log.info("返利已发放过，幂等跳过：inviteeId={}, sourceType={}, sourceId={}", inviteeId, sourceType, sourceId);
            return BigDecimal.ZERO;
        }

        // 累加额度（待领取/冻结/历史同步）
        affiliateMapper.addRebate(inviterId, pendingDelta, frozenDelta);
        return rebate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClaimResult claimRebate(Long inviterId, BigDecimal balanceAfter)
    {
        if (inviterId == null)
        {
            throw new ServiceException("用户不能为空");
        }
        // 锁定推广人行，串行化领取与解冻（accrue 只新增到期时间在未来的冻结流水，不会与此处冲突）
        AiInviteAffiliate affiliate = affiliateMapper.selectByUserIdForUpdate(inviterId);
        if (affiliate == null)
        {
            throw new ServiceException("暂无可领取的返利");
        }

        // 先解冻到期冻结额（lazy thaw）：汇总到期金额 → 转入待领取 → 标记流水已解冻
        thawFrozen(inviterId);

        // 重新读取（反映解冻后的待领额）
        affiliate = affiliateMapper.selectByUserIdForUpdate(inviterId);
        BigDecimal amount = affiliate.getPendingRebate() == null
                ? BigDecimal.ZERO : affiliate.getPendingRebate();
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("暂无可领取的返利");
        }

        // 清零待领取
        affiliateMapper.clearPending(inviterId);

        // 写领取流水（余额快照由聚合层加完余额后传入）
        AiInviteRebateRecord record = new AiInviteRebateRecord();
        record.setInviterId(inviterId);
        record.setAction(AiInviteConstants.ACTION_CLAIM);
        record.setAmount(amount);
        record.setBalanceAfter(balanceAfter);
        rebateRecordMapper.insertClaim(record);

        // 把领取金额与流水ID 交给聚合层，由其完成「加余额 + 写账户流水 type=6」
        return new ClaimResult(inviterId, amount, record.getRecordId());
    }

    @Override
    public void updateClaimBalanceAfter(Long rebateRecordId, BigDecimal balanceAfter)
    {
        if (rebateRecordId == null)
        {
            return;
        }
        rebateRecordMapper.updateClaimBalanceAfter(rebateRecordId, balanceAfter);
    }

    @Override
    public InviteInfoVO getInviteInfo(Long userId, InviteConfig config)
    {
        // 邀请返利总开关关闭时直接返回 null，前端据此隐藏卡片
        if (config == null || !config.isEnabled())
        {
            return null;
        }
        AiInviteAffiliate affiliate = ensureAffiliate(userId);
        InviteInfoVO vo = new InviteInfoVO();
        vo.setInviteCode(affiliate.getInviteCode());
        vo.setInviteLink(buildInviteLink(affiliate.getInviteCode()));
        vo.setEffectiveRebateRate(resolveRebateRate(userId, config));
        vo.setInviteCount(affiliate.getInviteCount() == null ? 0 : affiliate.getInviteCount());
        vo.setPendingRebate(nvl(affiliate.getPendingRebate()));
        vo.setFrozenRebate(nvl(affiliate.getFrozenRebate()));
        vo.setHistoryRebate(nvl(affiliate.getHistoryRebate()));
        List<InviteeVO> invitees = rebateRecordMapper.selectInviteeList(userId, MAX_INVITEE_LIST);
        vo.setInvitees(invitees);
        return vo;
    }

    // ==================== 管理员侧 ====================

    @Override
    public List<AiInviteAffiliate> selectAffiliateList(AiInviteAffiliate query)
    {
        return affiliateMapper.selectAffiliateList(query);
    }

    @Override
    public List<AiInviteRebateRecord> selectRecordList(AiInviteRebateRecord query)
    {
        return rebateRecordMapper.selectRecordList(query);
    }

    @Override
    public InviteOverviewVO getOverview(Long userId)
    {
        AiInviteAffiliate affiliate = ensureAffiliate(userId);
        InviteOverviewVO vo = new InviteOverviewVO();
        vo.setUserId(affiliate.getUserId());
        vo.setInviteCode(affiliate.getInviteCode());
        vo.setInviterId(affiliate.getInviterId());
        vo.setInviteCount(affiliate.getInviteCount());
        vo.setPendingRebate(nvl(affiliate.getPendingRebate()));
        vo.setFrozenRebate(nvl(affiliate.getFrozenRebate()));
        vo.setHistoryRebate(nvl(affiliate.getHistoryRebate()));
        vo.setRebateRate(affiliate.getRebateRate());
        return vo;
    }

    @Override
    public int setRebateRate(Long userId, BigDecimal rebateRate)
    {
        if (rebateRate != null
                && (rebateRate.compareTo(AiInviteConstants.REBATE_RATE_MIN) < 0
                || rebateRate.compareTo(AiInviteConstants.REBATE_RATE_MAX) > 0))
        {
            throw new ServiceException("返利比例必须在 0-100 之间");
        }
        ensureAffiliate(userId);
        return affiliateMapper.updateRebateRate(userId, rebateRate);
    }

    @Override
    public String resetInviteCode(Long userId)
    {
        ensureAffiliate(userId);
        String newCode = generateUniqueInviteCode();
        affiliateMapper.updateInviteCode(userId, newCode);
        return newCode;
    }

    // ==================== 内部工具 ====================

    /**
     * 解冻到期冻结额（lazy thaw）：把 frozen_until<=now 的额度从冻结转入待领取。
     * 调用方需持有推广人行锁（selectByUserIdForUpdate）以保证串行。
     */
    private void thawFrozen(Long inviterId)
    {
        BigDecimal thawAmount = rebateRecordMapper.sumAndClearThawAmount(inviterId);
        if (thawAmount == null || thawAmount.compareTo(BigDecimal.ZERO) <= 0)
        {
            return;
        }
        // 标记流水已解冻，防止下次重复汇总
        rebateRecordMapper.clearThawFlag(inviterId);
        // 冻结额转入待领取
        affiliateMapper.thawFrozen(inviterId, thawAmount);
    }

    /**
     * 生成全局唯一的邀请码（冲突重试）。
     */
    private String generateUniqueInviteCode()
    {
        char[] charset = AiInviteConstants.INVITE_CODE_CHARSET;
        int length = AiInviteConstants.INVITE_CODE_LENGTH;
        for (int attempt = 0; attempt < AiInviteConstants.INVITE_CODE_MAX_ATTEMPTS; attempt++)
        {
            StringBuilder sb = new StringBuilder(length);
            byte[] buf = new byte[length];
            SECURE_RANDOM.nextBytes(buf);
            for (int i = 0; i < length; i++)
            {
                sb.append(charset[(buf[i] & 0xFF) % charset.length]);
            }
            String code = sb.toString();
            if (affiliateMapper.selectByCode(code) == null)
            {
                return code;
            }
        }
        // 重试耗尽，理论上概率极低；递归再试一次
        return generateUniqueInviteCode();
    }

    /**
     * 解析生效返利比例：专属优先全局，clamp [0,100]。
     */
    private BigDecimal resolveRebateRate(Long inviterId, InviteConfig config)
    {
        BigDecimal rate = null;
        if (inviterId != null)
        {
            AiInviteAffiliate inviter = affiliateMapper.selectByUserId(inviterId);
            if (inviter != null && inviter.getRebateRate() != null)
            {
                rate = inviter.getRebateRate();
            }
        }
        if (rate == null)
        {
            rate = config == null ? AiInviteConstants.DEFAULT_REBATE_RATE : config.getGlobalRebateRate();
        }
        return clampRate(rate);
    }

    private BigDecimal clampRate(BigDecimal rate)
    {
        if (rate == null)
        {
            return AiInviteConstants.DEFAULT_REBATE_RATE;
        }
        if (rate.compareTo(AiInviteConstants.REBATE_RATE_MIN) < 0)
        {
            return AiInviteConstants.REBATE_RATE_MIN;
        }
        if (rate.compareTo(AiInviteConstants.REBATE_RATE_MAX) > 0)
        {
            return AiInviteConstants.REBATE_RATE_MAX;
        }
        return rate;
    }

    // ==================== 小工具 ====================

    private BigDecimal nvl(BigDecimal v)
    {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String buildInviteLink(String code)
    {
        return "/register?invite=" + code;
    }
}
