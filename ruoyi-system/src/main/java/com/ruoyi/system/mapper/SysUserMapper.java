package com.ruoyi.system.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.entity.SysUser;

/**
 * 用户表 数据层
 * 
 * @author ruoyi
 */
public interface SysUserMapper
{
    /**
     * 根据条件分页查询用户列表
     * 
     * @param sysUser 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser sysUser);

    /**
     * 根据条件分页查询已配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectAllocatedList(SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUnallocatedList(SysUser user);

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    public SysUser selectUserByUserName(String userName);

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public SysUser selectUserById(Long userId);

    public SysUser selectUserByIdForUpdate(Long userId);

    /** 查询计费预占所需的最小用户快照，不加行锁。 */
    public SysUser selectUserBillingSnapshot(Long userId);

    /**
     * 新增用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(SysUser user);

    /**
     * 修改用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(SysUser user);

    /**
     * 修改用户头像
     * 
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    public int updateUserAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    /**
     * 修改用户状态
     * 
     * @param userId 用户ID
     * @param status 状态
     * @return 结果
     */
    public int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    /** 更新用户计费倍率。 */
    public int updateUserBillingMultiplier(@Param("userId") Long userId,
                                           @Param("billingMultiplier") BigDecimal billingMultiplier);

    /**
     * 原子扣减用户余额并累计使用金额。
     *
     * @param userId 用户ID
     * @param cost 扣费金额
     * @return 结果
     */
    public int deductUserBalance(@Param("userId") Long userId, @Param("cost") BigDecimal cost);

    public int freezeUserBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    public int releaseUserFrozenBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    public int settleUserBilling(@Param("userId") Long userId,
                                 @Param("walletReserved") BigDecimal walletReserved,
                                 @Param("walletCharged") BigDecimal walletCharged,
                                 @Param("actualCost") BigDecimal actualCost,
                                 @Param("requestIncrement") int requestIncrement);

    /**
     * 记录订阅支付产生的用量，不扣减用户余额。
     *
     * @param userId 用户ID
     * @param cost 使用金额
     * @return 结果
     */
    public int recordUserSubscriptionUsage(@Param("userId") Long userId, @Param("cost") BigDecimal cost);

    /**
     * 原子增加用户余额（兑换码充值）。
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @return 结果
     */
    public int addUserBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 设置用户余额（覆盖模式）。
     *
     * @param userId 用户ID
     * @param balance 新余额
     * @return 结果
     */
    public int setUserBalance(@Param("userId") Long userId, @Param("balance") BigDecimal balance);

    /**
     * 原子减少用户余额（不影响已用金额和请求次数）。
     *
     * @param userId 用户ID
     * @param amount 减少金额
     * @return 结果
     */
    public int subUserBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 更新AI扣费偏好。
     *
     * @param userId 用户ID
     * @param billingPreference 扣费偏好
     * @return 结果
     */
    public int updateBillingPreference(@Param("userId") Long userId, @Param("billingPreference") String billingPreference);

    /**
     * 更新用户AI并发上限。
     *
     * @param userId 用户ID
     * @param aiConcurrencyLimit AI并发上限
     * @param updateBy 更新者
     * @return 结果
     */
    public int updateUserAiConcurrencyLimit(@Param("userId") Long userId,
                                            @Param("aiConcurrencyLimit") Integer aiConcurrencyLimit,
                                            @Param("updateBy") String updateBy);

    /**
     * AI在途并发计数自增（条件UPDATE完成"检查+占位"，须在预占事务内调用）。
     * 影响行数为0表示已达并发上限（或上限为0被禁用），调用方应回滚预占事务。
     *
     * @param userId 用户ID
     * @return 影响行数（1=占位成功，0=超限拒绝）
     */
    public int incrementUserActiveRequestCount(@Param("userId") Long userId);

    /**
     * AI在途并发计数递减（账单离开reserved状态的出口调用，与状态迁移同事务）。
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    public int decrementUserActiveRequestCount(@Param("userId") Long userId);

    /**
     * 更新用户登录信息（IP和登录时间）
     * 
     * @param userId 用户ID
     * @param loginIp 登录IP地址
     * @param loginDate 登录时间
     * @return 结果
     */
    public int updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp, @Param("loginDate") Date loginDate);

    /**
     * 重置用户密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId);

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 校验用户名称是否唯一
     * 
     * @param userName 用户名称
     * @return 结果
     */
    public SysUser checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    public SysUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public SysUser checkEmailUnique(String email);
}
