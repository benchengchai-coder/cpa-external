package com.ruoyi.cpaexternal.dashboard.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardTrendVO;
import com.ruoyi.cpaexternal.dashboard.domain.vo.DashboardUserRankVO;

/**
 * 仪表盘统计 Mapper。
 */
public interface CpaDashboardMapper
{
    /** 概览：平台总数 */
    int countPlatforms();

    /** 概览：模型总数 */
    int countModels();

    /** 概览：用户总数 */
    int countUsers();

    /** 概览：今日新增用户数 */
    int countTodayNewUsers();

    /** 概览：API Key 总数 */
    int countApiKeys();

    /** 概览：今日请求数 */
    int countTodayRequests();

    /** 概览：最近一分钟请求数（RPM） */
    int countRequestsPerMinute();

    /** 概览：今日 Token 消耗总量 */
    long sumTodayTokens();

    /** 概览：历史 Token 消耗总量 */
    long sumTotalTokens();

    /** 概览：今日计算费用 */
    BigDecimal sumTodayCost();

    /** 概览：今日钱包与订阅实扣金额 */
    BigDecimal sumTodayChargedAmount();

    /** 概览：最近7天成功调用平均响应时间（毫秒） */
    BigDecimal avgRecentSuccessDuration();

    /** 概览：最近7天成功调用平均首 Token 响应时间（毫秒） */
    BigDecimal avgRecentSuccessFirstTokenTime();

    /**
     * 调用趋势：today 按小时聚合，其余按天聚合。
     *
     * @param mode 聚合模式：today / daily
     * @param startDate 起始日期（含），daily 模式使用，today 模式忽略
     * @param userId 用户ID，为空时统计全站
     */
    List<DashboardTrendVO> selectTrend(@Param("mode") String mode,
            @Param("startDate") String startDate,
            @Param("userId") Long userId);

    /** 用户实扣排行：有调用或实扣记录的用户总数（startDate 为空表示全部） */
    long countUserRank(@Param("startDate") String startDate);

    /** 用户实扣排行：按实扣金额降序分页（startDate 为空表示全部） */
    List<DashboardUserRankVO> selectUserRank(@Param("startDate") String startDate,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /** 用户：今日请求数 */
    int countUserTodayRequests(@Param("userId") Long userId);

    /** 用户：Token 消耗总量 */
    long sumUserTokens(@Param("userId") Long userId);

    /** 用户：API Key 数量 */
    int countUserApiKeys(@Param("userId") Long userId);

    /** 用户：最近7天成功调用平均响应时间（毫秒） */
    BigDecimal avgRecentUserSuccessDuration(@Param("userId") Long userId);

    /** 用户：最近7天成功调用平均首 Token 响应时间（毫秒） */
    BigDecimal avgRecentUserSuccessFirstTokenTime(@Param("userId") Long userId);
}
