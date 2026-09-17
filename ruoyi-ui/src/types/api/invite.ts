import type { BaseEntity, PageDomain } from "./common";

/** 邀请返利关系（管理员视角） */
export interface AiInviteAffiliate extends BaseEntity {
  userId?: number;
  userName?: string;
  inviteCode?: string;
  inviterId?: number;
  inviterUserName?: string;
  inviteCount?: number;
  pendingRebate?: number;
  frozenRebate?: number;
  historyRebate?: number;
  /** 专属返利比例（NULL=沿用全局） */
  rebateRate?: number | null;
}

/** 返利流水 */
export interface AiInviteRebateRecord extends BaseEntity {
  recordId?: number;
  inviterId?: number;
  inviterUserName?: string;
  inviteeId?: number;
  inviteeUserName?: string;
  /** 1产生返利 2领取返利 */
  action?: string;
  amount?: number;
  /** 1在线支付 2兑换码 */
  sourceType?: string;
  sourceId?: number;
  balanceAfter?: number;
  frozenUntil?: string;
}

/** 被邀请人（用户视角，邮箱已脱敏） */
export interface InviteeVO {
  userId?: number;
  username?: string;
  email?: string;
  totalRebate?: number;
  createTime?: string;
}

/** 邀请返利信息（用户视角聚合） */
export interface InviteInfoVO {
  inviteCode?: string;
  inviteLink?: string;
  effectiveRebateRate?: number;
  inviteCount?: number;
  pendingRebate?: number;
  frozenRebate?: number;
  historyRebate?: number;
  invitees?: InviteeVO[];
}

/** 单用户概览（管理员视角） */
export interface InviteOverviewVO {
  userId?: number;
  username?: string;
  inviteCode?: string;
  inviterId?: number;
  inviteCount?: number;
  pendingRebate?: number;
  frozenRebate?: number;
  historyRebate?: number;
  rebateRate?: number | null;
}

/** 邀请关系查询参数 */
export interface AiInviteAffiliateQuery extends PageDomain {
  inviteCode?: string;
  inviterId?: number;
}

/** 返利流水查询参数 */
export interface AiInviteRebateRecordQuery extends PageDomain {
  inviterId?: number;
  action?: string;
  sourceType?: string;
}

/** 领取返利结果 */
export interface InviteClaimResult {
  amount: number;
}

/** 邀请返利参数配置（管理员视角） */
export interface InviteAdminConfig {
  enabled: boolean;
  rebateRate: number;
  freezeHours: number;
  durationDays: number;
  perInviteeCap: number;
}
