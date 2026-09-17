/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80405 (8.4.5)
 Source Host           : localhost:3306
 Source Schema         : aigate

 Target Server Type    : MySQL
 Target Server Version : 80405 (8.4.5)
 File Encoding         : 65001

 Date: 09/09/2026 20:41:17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_apikey
-- ----------------------------
DROP TABLE IF EXISTS `ai_apikey`;
CREATE TABLE `ai_apikey`  (
  `key_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Key ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `key_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `api_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'API Key明文（敏感，与CPA api-keys一致）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `accessed_time` datetime NULL DEFAULT NULL COMMENT '最后访问时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`key_id`) USING BTREE,
  UNIQUE INDEX `uk_api_key`(`api_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI API Key表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_apikey
-- ----------------------------
INSERT INTO `ai_apikey` VALUES (2, 1, 'root', 'xveLQGXBpFC2++hyQAvtjG46t/QSSZZex+Zd3SM2lQ7q8MfxSu59Xd7iLIaBJIgjIjZz2TM0W0mJLm+sMKhpuqRKZMGn4uwuzmlmTb2yXw==', '0', '2026-09-06 16:53:09', NULL);
INSERT INTO `ai_apikey` VALUES (10, 108, '默认密钥', 'MdHtGKZXV6YCIr87Km+iHYYm2yFa5LHZXbcCxr1CB31Lb+jPkskxBARLFiiMSktxc9T/XCLTdbFnd/mX22q8zWDTaNthf7Nr8439k9Xphw==', '0', NULL, NULL);

-- ----------------------------
-- Table structure for ai_balance_reminder_state
-- ----------------------------
DROP TABLE IF EXISTS `ai_balance_reminder_state`;
CREATE TABLE `ai_balance_reminder_state`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `last_sent_time` datetime NULL DEFAULT NULL COMMENT '上次成功发送时间',
  `last_source` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提醒来源 subscription/wallet',
  `last_subscription_id` bigint NULL DEFAULT NULL COMMENT '上次提醒关联订阅ID',
  `last_available_amount` decimal(18, 10) NULL DEFAULT NULL COMMENT '发送时可用额度快照（美元）',
  `last_attempt_time` datetime NULL DEFAULT NULL COMMENT '最近尝试时间',
  `last_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'idle' COMMENT '状态 idle/processing/success/failed/skipped',
  `last_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近失败或跳过原因',
  `claim_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送占用令牌',
  `claim_expire_time` datetime NULL DEFAULT NULL COMMENT '发送占用过期时间',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  INDEX `idx_balance_reminder_last_sent`(`last_sent_time` ASC) USING BTREE,
  INDEX `idx_balance_reminder_claim_expire`(`claim_expire_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI额度不足邮件提醒状态' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_balance_reminder_state
-- ----------------------------
INSERT INTO `ai_balance_reminder_state` VALUES (1, '2026-08-12 14:03:47', 'subscription', 11, 0.0000000000, '2026-08-12 14:03:45', 'success', NULL, NULL, NULL, '2026-07-28 23:59:22', '2026-08-12 14:03:46');
INSERT INTO `ai_balance_reminder_state` VALUES (107, '2026-07-28 23:59:26', 'wallet', NULL, 4.0000000000, '2026-07-28 23:59:25', 'success', NULL, NULL, NULL, '2026-07-28 23:59:24', '2026-07-28 23:59:26');
INSERT INTO `ai_balance_reminder_state` VALUES (108, '2026-08-02 17:34:14', 'subscription', 17, 1.0000000000, '2026-08-02 17:34:12', 'success', NULL, NULL, NULL, '2026-08-02 17:34:12', '2026-08-02 17:34:13');

-- ----------------------------
-- Table structure for ai_billing_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_billing_record`;
CREATE TABLE `ai_billing_record`  (
  `billing_id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算ID',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请求ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `key_id` bigint NULL DEFAULT NULL COMMENT 'API Key ID',
  `subscription_id` bigint NULL DEFAULT NULL COMMENT '用户订阅ID',
  `subscription_plan_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结算时订阅套餐标题快照',
  `billing_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '扣费来源 wallet/subscription/mixed/no_charge/write_off',
  `billing_preference` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预占时扣费偏好快照',
  `amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '应计金额（正常结算时sys_user.used_balance按此累计）',
  `reserved_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '用户资金总预占额',
  `wallet_reserved_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '钱包预占额',
  `subscription_reserved_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '订阅预占额',
  `key_reserved_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT 'API Key预占额',
  `wallet_charged_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '钱包实扣额',
  `subscription_charged_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '订阅实扣额',
  `key_charged_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT 'API Key实扣额',
  `uncovered_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '用户资金未覆盖金额（不形成欠费）',
  `reserve_expire_time` datetime NULL DEFAULT NULL COMMENT '预占过期时间',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'processing' COMMENT '状态 processing/reserved/pending_settlement/success/partial/released/expired/failed/written_off',
  `log_id` bigint NULL DEFAULT NULL COMMENT '调用日志ID',
  `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `resolution_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人工核销原因',
  `resolved_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '人工处置操作人',
  `resolved_time` datetime(3) NULL DEFAULT NULL COMMENT '人工处置时间',
  `settle_retry_count` int NOT NULL DEFAULT 0 COMMENT '异步结算重试次数',
  `settle_next_retry_time` datetime(3) NULL DEFAULT NULL COMMENT '下次结算重试时间',
  `settle_claim_time` datetime(3) NULL DEFAULT NULL COMMENT 'Worker领取时间',
  `settle_claim_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Worker领取令牌',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`billing_id`) USING BTREE,
  UNIQUE INDEX `uk_ai_billing_request_id`(`request_id` ASC) USING BTREE,
  INDEX `idx_ai_billing_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_ai_billing_subscription`(`subscription_id` ASC) USING BTREE,
  INDEX `idx_ai_billing_status`(`status` ASC) USING BTREE,
  INDEX `idx_ai_billing_reserve_expire`(`status` ASC, `reserve_expire_time` ASC) USING BTREE,
  INDEX `idx_ai_billing_status_create`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_ai_billing_user_status_create`(`user_id` ASC, `status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_ai_billing_log_status`(`log_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1144 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI幂等结算记录' ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for ai_billing_settlement_task
-- ----------------------------
DROP TABLE IF EXISTS `ai_billing_settlement_task`;
CREATE TABLE `ai_billing_settlement_task`  (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `billing_id` bigint NOT NULL COMMENT '账单ID',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请求ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `key_id` bigint NULL DEFAULT NULL COMMENT 'API Key ID',
  `actual_cost` decimal(18, 10) NOT NULL COMMENT '实际费用',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/claimed/done/failed/resolved',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '业务重试次数',
  `available_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下次可领取时间',
  `claim_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '领取凭证',
  `claim_expire_time` datetime(3) NULL DEFAULT NULL COMMENT '领取租约到期时间',
  `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后错误信息',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`task_id`) USING BTREE,
  UNIQUE INDEX `uk_billing_task_billing`(`billing_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_billing_task_request`(`request_id` ASC) USING BTREE,
  INDEX `idx_billing_task_dispatch`(`status` ASC, `available_time` ASC, `task_id` ASC) USING BTREE,
  INDEX `idx_billing_task_reclaim`(`status` ASC, `claim_expire_time` ASC, `task_id` ASC) USING BTREE,
  INDEX `idx_billing_task_failed`(`status` ASC, `update_time` DESC, `task_id` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 348 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI计费异步结算任务' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_checkin_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_checkin_record`;
CREATE TABLE `ai_checkin_record`  (
  `checkin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '签到记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '用户名',
  `checkin_date` date NOT NULL COMMENT '签到日期',
  `reward_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '奖励金额（美元）',
  `balance_after` decimal(18, 10) NULL DEFAULT NULL COMMENT '签到后余额快照',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0成功）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`checkin_id`) USING BTREE,
  UNIQUE INDEX `uk_user_checkin_date`(`user_id` ASC, `checkin_date` ASC) USING BTREE,
  INDEX `idx_checkin_date`(`checkin_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '每日签到记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_dashboard_daily_state
-- ----------------------------
DROP TABLE IF EXISTS `ai_dashboard_daily_state`;
CREATE TABLE `ai_dashboard_daily_state`  (
  `state_id` tinyint NOT NULL COMMENT '固定为1',
  `initialized` tinyint NOT NULL DEFAULT 0 COMMENT '历史回填是否完成（0否 1是）',
  `backfill_next_date` date NULL DEFAULT NULL COMMENT '下一次待回填日期',
  `last_success_time` datetime(3) NULL DEFAULT NULL COMMENT '最近成功刷新时间',
  `last_error` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近一次错误',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`state_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员仪表盘日汇任务状态' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_dashboard_daily_state
-- ----------------------------
INSERT INTO `ai_dashboard_daily_state` VALUES (1, 1, NULL, '2026-09-09 15:16:33.866', NULL, '2026-08-14 00:58:20.737', '2026-09-09 15:16:33.866');

-- ----------------------------
-- Table structure for ai_invite_affiliate
-- ----------------------------
DROP TABLE IF EXISTS `ai_invite_affiliate`;
CREATE TABLE `ai_invite_affiliate`  (
  `user_id` bigint NOT NULL COMMENT '用户ID（关联sys_user.user_id）',
  `invite_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '专属邀请码（12位）',
  `inviter_id` bigint NULL DEFAULT NULL COMMENT '邀请人用户ID（绑定一次不可改）',
  `invite_count` int NOT NULL DEFAULT 0 COMMENT '累计邀请人数',
  `pending_rebate` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '待领取返利额度',
  `frozen_rebate` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '冻结中返利额度',
  `history_rebate` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '历史累计返利（只增不减）',
  `rebate_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '专属返利比例（百分比，NULL=沿用全局）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_invite_code`(`invite_code` ASC) USING BTREE,
  INDEX `idx_inviter_id`(`inviter_id` ASC) USING BTREE,
  INDEX `idx_pending_rebate`(`pending_rebate` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '邀请返利关系表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for ai_invite_rebate_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_invite_rebate_record`;
CREATE TABLE `ai_invite_rebate_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '返利流水ID',
  `inviter_id` bigint NOT NULL COMMENT '收益人（邀请人）用户ID',
  `invitee_id` bigint NULL DEFAULT NULL COMMENT '被邀请人用户ID（产生返利时记录）',
  `action` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '动作（1产生返利 2领取返利）',
  `amount` decimal(18, 10) NOT NULL COMMENT '金额',
  `source_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '返利来源（1在线支付 2兑换码）',
  `source_id` bigint NULL DEFAULT NULL COMMENT '来源ID（充值记录ID或订单ID）',
  `balance_after` decimal(18, 10) NULL DEFAULT NULL COMMENT '领取后余额快照',
  `frozen_until` datetime NULL DEFAULT NULL COMMENT '冻结到期时间（NULL=已解冻或从未冻结）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`) USING BTREE,
  UNIQUE INDEX `uk_source`(`source_type` ASC, `source_id` ASC) USING BTREE,
  INDEX `idx_inviter_id`(`inviter_id` ASC) USING BTREE,
  INDEX `idx_invitee_id`(`invitee_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '邀请返利流水表' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for ai_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_log`;
CREATE TABLE `ai_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `timestamp` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'CLIProxyAPI日志时间（+08:00）',
  `latency_ms` int NOT NULL DEFAULT 0 COMMENT '请求耗时（毫秒）',
  `ttft_ms` int NULL DEFAULT 0 COMMENT '首Token延迟（毫秒）',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '调用来源',
  `auth_index` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '认证索引',
  `access_token_sha256` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '访问令牌SHA256哈希',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `x_forwarded_for` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'X-Forwarded-For代理链',
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '客户端User-Agent',
  `input_tokens` int NOT NULL DEFAULT 0 COMMENT '输入Token数',
  `output_tokens` int NOT NULL DEFAULT 0 COMMENT '输出Token数',
  `reasoning_tokens` int NOT NULL DEFAULT 0 COMMENT '推理Token数',
  `cached_tokens` int NOT NULL DEFAULT 0 COMMENT '缓存Token数',
  `cache_read_tokens` int NULL DEFAULT 0 COMMENT '缓存读取Token数',
  `cache_read_tokens_present` tinyint(1) NULL DEFAULT 0 COMMENT '上游是否返回缓存读取字段（0否 1是）',
  `cache_creation_tokens` int NULL DEFAULT 0 COMMENT '缓存写入Token数',
  `total_tokens` int NOT NULL DEFAULT 0 COMMENT '总Token数',
  `failed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否失败（0否 1是）',
  `generate` tinyint(1) NULL DEFAULT 1 COMMENT '是否生成请求（0否 1是）',
  `stream` tinyint(1) NULL DEFAULT 0 COMMENT '是否流式请求（0否 1是）',
  `fail` json NULL COMMENT '失败详情JSON对象（status_code/body）',
  `accounting_version` int NULL DEFAULT NULL COMMENT 'Token计费结构版本',
  `token_breakdown` json NULL COMMENT 'Token分类明细JSON对象',
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '供应商',
  `executor_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行器类型',
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型',
  `alias` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型别名',
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求端点',
  `auth_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '认证类型',
  `api_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'CLIProxyAPI传入的API Key值，用于归属用户解析',
  `request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '请求ID',
  `session_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '会话ID',
  `parent_session_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '父会话ID',
  `reasoning_effort` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '推理等级',
  `service_tier` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求服务层级',
  `response_service_tier` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '响应服务层级',
  `response_headers` json NULL COMMENT '响应头JSON对象',
  `user_id` bigint NULL DEFAULT NULL COMMENT '根据api_key解析出的用户ID',
  `key_id` bigint NULL DEFAULT NULL COMMENT '匹配到的API Key ID',
  `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户账号快照',
  `key_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'API Key名称快照',
  `billing_multiplier` decimal(10, 2) NULL DEFAULT NULL COMMENT '入库时用户计费倍率快照',
  `cost` decimal(18, 10) NULL DEFAULT NULL COMMENT '计算费用（官方定价×用户倍率，美元）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  UNIQUE INDEX `uk_request_id`(`request_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_key_time`(`key_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_provider_model_time`(`provider` ASC, `model` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_failed_time`(`failed` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'CLIProxyAPI调用日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_model
-- ----------------------------
DROP TABLE IF EXISTS `ai_model`;
CREATE TABLE `ai_model`  (
  `model_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型ID',
  `model_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型标识（如 gpt-4o, claude-3-opus）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '模型描述',
  `icon` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '图标',
  `platform` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'openai' COMMENT '平台（openai/anthropic/deepseek 等）',
  `official_input_price` decimal(10, 6) NOT NULL DEFAULT 0.000000 COMMENT '输入价格（$/百万token）',
  `official_output_price` decimal(10, 6) NOT NULL DEFAULT 0.000000 COMMENT '输出价格（$/百万token）',
  `official_cache_read_price` decimal(10, 6) NULL DEFAULT NULL COMMENT '缓存读取价格（$/百万token，NULL=不区分，按input_price计）',
  `official_cache_write_price` decimal(10, 6) NULL DEFAULT NULL COMMENT '缓存写入价格（$/百万token，NULL=不区分，按input_price计）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`model_id`) USING BTREE,
  UNIQUE INDEX `uk_model_name`(`model_name` ASC) USING BTREE,
  INDEX `idx_platform`(`platform` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 116 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI模型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_model
-- ----------------------------
INSERT INTO `ai_model` VALUES (110, 'gpt-5.6-sol', NULL, '', 'openai', 5.000000, 30.000000, 0.500000, 6.250000, '0', 'admin', '2026-07-09 23:08:23', 'admin', '2026-07-10 09:41:56', NULL);
INSERT INTO `ai_model` VALUES (111, 'gpt-5.6-terra', NULL, '', 'openai', 2.500000, 15.000000, 0.250000, 3.125000, '0', 'admin', '2026-07-09 23:09:03', 'admin', '2026-07-10 09:41:57', NULL);
INSERT INTO `ai_model` VALUES (112, 'gpt-5.6-luna', NULL, '', 'openai', 1.000000, 6.000000, 0.100000, 1.250000, '0', 'admin', '2026-07-09 23:09:31', 'admin', '2026-08-08 14:41:13', NULL);
INSERT INTO `ai_model` VALUES (113, 'deepseek-v4-flash', NULL, '', 'deepseek', 1.000000, 2.000000, 0.020000, NULL, '0', 'admin', '2026-07-31 16:11:49', 'admin', '2026-07-31 16:13:07', NULL);
INSERT INTO `ai_model` VALUES (114, 'deepseek-v4-pro', NULL, '', 'deepseek', 0.000000, 0.000000, NULL, NULL, '0', 'admin', '2026-08-26 21:50:10', 'admin', '2026-08-26 21:50:30', NULL);
INSERT INTO `ai_model` VALUES (115, 'gpt-6-astra', NULL, '', 'openai', 10.000000, 50.000000, 5.000000, NULL, '0', 'admin', '2026-09-06 01:47:21', '', NULL, NULL);

-- ----------------------------
-- Table structure for ai_oauth_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_oauth_log`;
CREATE TABLE `ai_oauth_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `client_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'OAuth Client ID',
  `proxy_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '代理URL',
  `grant_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'refresh_token' COMMENT '授权类型',
  `access_token` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '访问令牌（加密）',
  `refresh_token` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '刷新令牌（加密）',
  `id_token` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'ID Token（加密）',
  `token_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '令牌类型（Bearer）',
  `expires_in` bigint NULL DEFAULT NULL COMMENT '过期时间（秒）',
  `expires_at` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `scope` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '授权范围',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户邮箱',
  `chatgpt_account_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'ChatGPT账号ID',
  `chatgpt_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'ChatGPT用户ID',
  `organization_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组织ID',
  `plan_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订阅计划类型（plus/pro/free）',
  `subscription_expires_at` datetime NULL DEFAULT NULL COMMENT '订阅过期时间',
  `request_id` char(31) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内部请求ID',
  `request_time` datetime NULL DEFAULT NULL COMMENT '请求时间',
  `response_time` datetime NULL DEFAULT NULL COMMENT '响应时间',
  `duration` int NULL DEFAULT NULL COMMENT '耗时（毫秒）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0成功 1失败）',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误信息',
  `other` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '其他信息（JSON格式）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  INDEX `idx_request_id`(`request_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_status_time`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_email`(`email` ASC) USING BTREE,
  INDEX `idx_account_id`(`chatgpt_account_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'OAuth认证审计日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_pay_order
-- ----------------------------
DROP TABLE IF EXISTS `ai_pay_order`;
CREATE TABLE `ai_pay_order`  (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `out_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商户订单号（全局唯一）',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '下单用户名',
  `total_amount` decimal(18, 10) NOT NULL COMMENT '支付金额（USD，等同人民币）',
  `subject` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '订单标题',
  `trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付宝交易号（回调回填）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0待支付 1已支付 2已关闭 3已退款）',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付成功时间',
  `notify_time` datetime NULL DEFAULT NULL COMMENT '收到异步通知时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`order_id`) USING BTREE,
  UNIQUE INDEX `uk_out_trade_no`(`out_trade_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status_create_time`(`status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 54 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '支付宝支付订单表' ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for ai_platform
-- ----------------------------
DROP TABLE IF EXISTS `ai_platform`;
CREATE TABLE `ai_platform`  (
  `platform_id` bigint NOT NULL AUTO_INCREMENT COMMENT '平台ID',
  `platform_val` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '平台字典值（对应 ai_platform_name.dict_value）',
  `platform_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '平台名称（对应 ai_platform_name.dict_label）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`platform_id`) USING BTREE,
  UNIQUE INDEX `uk_platform_val`(`platform_val` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 108 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI平台表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_platform
-- ----------------------------
INSERT INTO `ai_platform` VALUES (100, 'openai', 'OpenAI', '0', NULL);
INSERT INTO `ai_platform` VALUES (107, 'deepseek', 'DeepSeek', '0', NULL);

-- ----------------------------
-- Table structure for ai_recharge_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_recharge_record`;
CREATE TABLE `ai_recharge_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '用户名',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '充值类型（1兑换码 2在线支付 3管理员调整 4新用户注册 5每日签到 6邀请返利）',
  `amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '充值金额（美元）',
  `source_id` bigint NULL DEFAULT NULL COMMENT '来源ID（兑换码ID、支付订单ID或业务流水ID）',
  `source_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '来源名称（兑换码名称、支付方式或业务来源）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0成功 1失败）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 78 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI充值记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_redemption_code
-- ----------------------------
DROP TABLE IF EXISTS `ai_redemption_code`;
CREATE TABLE `ai_redemption_code`  (
  `code_id` bigint NOT NULL AUTO_INCREMENT COMMENT '兑换码ID',
  `code_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `quota` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '额度（美元）',
  `redemption_key` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '兑换码（32位唯一码）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态（0未使用 1已使用 2已禁用）',
  `expired_time` datetime NULL DEFAULT NULL COMMENT '过期时间（NULL=永不过期）',
  `used_user_id` bigint NULL DEFAULT NULL COMMENT '使用人用户ID',
  `used_username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '使用人用户名',
  `redeemed_time` datetime NULL DEFAULT NULL COMMENT '兑换时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`code_id`) USING BTREE,
  UNIQUE INDEX `uk_redemption_key`(`redemption_key` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_used_user_id`(`used_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI兑换码表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_redemption_code
-- ----------------------------
INSERT INTO `ai_redemption_code` VALUES (1, '1', 1.0000000000, 'e322bab524934a17a5cace0a04d232eb', '1', NULL, 1, 'admin', '2026-06-27 00:34:51', 'admin', '2026-06-27 00:34:35', '', '2026-06-27 00:34:51', NULL);
INSERT INTO `ai_redemption_code` VALUES (2, '99', 100.0000000000, 'bdbefd6a7e4c4d1cb487f0b6d01c821f', '1', NULL, 105, 'b15902193', '2026-07-05 17:35:14', 'admin', '2026-07-05 17:33:13', '', '2026-07-05 17:35:13', NULL);
INSERT INTO `ai_redemption_code` VALUES (3, '11', 100.0000000000, '0855d738ae8f4065a178587eaedbcf83', '1', NULL, 106, 'a15902193', '2026-07-05 19:01:32', 'admin', '2026-07-05 19:01:00', '', '2026-07-05 19:01:31', NULL);

-- ----------------------------
-- Table structure for ai_subscription_plan
-- ----------------------------
DROP TABLE IF EXISTS `ai_subscription_plan`;
CREATE TABLE `ai_subscription_plan`  (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '套餐标题',
  `sub_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '套餐副标题',
  `price_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '套餐价格（USD）',
  `duration_unit` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'month' COMMENT '有效期单位 day/week/month/year/custom',
  `duration_value` int NOT NULL DEFAULT 1 COMMENT '有效期数值',
  `custom_seconds` bigint NULL DEFAULT NULL COMMENT '自定义有效期秒数',
  `amount_total` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '订阅总额度（0为不限）',
  `quota_reset_period` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'none' COMMENT '额度重置周期 none/day/week/month/custom',
  `quota_reset_custom_seconds` bigint NULL DEFAULT NULL COMMENT '自定义重置周期秒数',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `max_purchase_per_user` int NULL DEFAULT NULL COMMENT '每用户购买上限',
  `allow_balance_purchase` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否允许余额购买',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`plan_id`) USING BTREE,
  INDEX `idx_ai_subscription_plan_status`(`status` ASC) USING BTREE,
  INDEX `idx_ai_subscription_plan_sort`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI订阅套餐' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_subscription_plan
-- ----------------------------
INSERT INTO `ai_subscription_plan` VALUES (1, 'PLUS', 'PLUS', 50.0000000000, 'month', 1, NULL, 50.0000000000, 'none', NULL, '1', 0, NULL, 1, 'admin', '2026-06-07 02:03:14', 'admin', '2026-08-07 21:13:53', '');
INSERT INTO `ai_subscription_plan` VALUES (4, 'PRO', 'PRO', 100.0000000000, 'month', 1, NULL, 100.0000000000, 'none', NULL, '1', 4, NULL, 1, 'admin', '2026-06-07 02:04:28', 'admin', '2026-08-07 21:13:53', '');
INSERT INTO `ai_subscription_plan` VALUES (6, '新人试用(有效期3天)', '新人试用(有效期3天)', 1.0000000000, 'day', 3, NULL, 1.0000000000, 'none', NULL, '0', 0, NULL, 1, 'admin', '2026-08-02 16:32:30', 'admin', '2026-08-04 22:32:45', '');

-- ----------------------------
-- Table structure for ai_subscription_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_subscription_record`;
CREATE TABLE `ai_subscription_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户账号快照',
  `plan_id` bigint NULL DEFAULT NULL COMMENT '套餐ID',
  `plan_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '套餐标题快照',
  `user_subscription_id` bigint NULL DEFAULT NULL COMMENT '用户订阅ID',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型 grant/purchase/cancel/expire',
  `amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '金额',
  `source_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源名称',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作者',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '状态（0成功 1失败）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `idx_ai_subscription_record_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_ai_subscription_record_subscription`(`user_subscription_id` ASC) USING BTREE,
  INDEX `idx_ai_subscription_record_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI订阅流水' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_upstream_failure
-- ----------------------------
DROP TABLE IF EXISTS `ai_upstream_failure`;
CREATE TABLE `ai_upstream_failure`  (
  `failure_id` bigint NOT NULL AUTO_INCREMENT COMMENT '失败事件ID',
  `event_time` datetime(3) NULL DEFAULT NULL COMMENT '事件发生时间（CLIProxyAPI上报）',
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '供应商',
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型',
  `auth_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '凭证ID',
  `auth_index` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '认证索引',
  `status_code` int NULL DEFAULT NULL COMMENT '上游失败状态码',
  `body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '上游错误消息或响应体',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'CLIProxyAPI错误分类码',
  `retryable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否可重试（0否 1是）',
  `auth_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '凭证状态',
  `auth_disabled` tinyint(1) NULL DEFAULT 0 COMMENT '凭证是否被禁用（0否 1是）',
  `auth_unavailable` tinyint(1) NULL DEFAULT 0 COMMENT '凭证是否处于不可用冷却（0否 1是）',
  `auth_next_retry_at` datetime(3) NULL DEFAULT NULL COMMENT '凭证预计恢复时间',
  `quota_exceeded` tinyint(1) NULL DEFAULT 0 COMMENT '配额是否超限（0否 1是）',
  `quota_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配额超限原因',
  `auth_status_snapshot` json NULL COMMENT '完整凭证状态快照JSON对象（含模型级状态）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`failure_id`) USING BTREE,
  INDEX `idx_provider_time`(`provider` ASC, `event_time` ASC) USING BTREE,
  INDEX `idx_auth_time`(`auth_index` ASC, `event_time` ASC) USING BTREE,
  INDEX `idx_event_time`(`event_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'CLIProxyAPI上游失败事件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_usage_daily
-- ----------------------------
DROP TABLE IF EXISTS `ai_usage_daily`;
CREATE TABLE `ai_usage_daily`  (
  `stat_date` date NOT NULL COMMENT '业务统计日期（Asia/Shanghai）',
  `request_count` bigint NOT NULL DEFAULT 0 COMMENT '调用日志条数',
  `prompt_tokens` bigint NOT NULL DEFAULT 0 COMMENT '输入Token总量',
  `completion_tokens` bigint NOT NULL DEFAULT 0 COMMENT '输出Token总量',
  `total_tokens` bigint NOT NULL DEFAULT 0 COMMENT 'Token总量',
  `total_cost` decimal(28, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '计算费用总额',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`stat_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '全站每日用量与费用汇总' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_user_subscription
-- ----------------------------
DROP TABLE IF EXISTS `ai_user_subscription`;
CREATE TABLE `ai_user_subscription`  (
  `subscription_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户订阅ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户账号快照',
  `plan_id` bigint NOT NULL COMMENT '套餐ID',
  `plan_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '套餐标题快照',
  `plan_sub_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '套餐副标题快照',
  `price_amount` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '套餐价格快照',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'active' COMMENT '状态 active/cancelled/expired',
  `amount_total` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '订阅总额度（0为不限）',
  `amount_used` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '已用额度',
  `frozen_balance` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '冻结额度',
  `quota_reset_period` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'none' COMMENT '额度重置周期快照 none/day/week/month/custom',
  `quota_reset_custom_seconds` bigint NULL DEFAULT NULL COMMENT '自定义重置周期秒数快照',
  `last_reset_time` datetime NULL DEFAULT NULL COMMENT '上次重置时间',
  `next_reset_time` datetime NULL DEFAULT NULL COMMENT '下次重置时间',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'grant' COMMENT '来源 grant/balance_purchase',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`subscription_id`) USING BTREE,
  INDEX `idx_ai_user_subscription_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_ai_user_subscription_plan`(`plan_id` ASC) USING BTREE,
  INDEX `idx_ai_user_subscription_status_end`(`status` ASC, `end_time` ASC) USING BTREE,
  INDEX `idx_ai_user_subscription_billing`(`user_id` ASC, `status` ASC, `end_time` ASC, `subscription_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI用户订阅' ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for ai_user_usage_daily
-- ----------------------------
DROP TABLE IF EXISTS `ai_user_usage_daily`;
CREATE TABLE `ai_user_usage_daily`  (
  `stat_date` date NOT NULL COMMENT '业务统计日期（Asia/Shanghai）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `request_count` bigint NOT NULL DEFAULT 0 COMMENT '调用日志条数',
  `prompt_tokens` bigint NOT NULL DEFAULT 0 COMMENT '输入Token总量',
  `completion_tokens` bigint NOT NULL DEFAULT 0 COMMENT '输出Token总量',
  `total_tokens` bigint NOT NULL DEFAULT 0 COMMENT 'Token总量',
  `wallet_charged_amount` decimal(28, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '钱包实扣总额',
  `subscription_charged_amount` decimal(28, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '订阅实扣总额',
  `total_charged_amount` decimal(28, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '钱包与订阅实扣总额',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`stat_date`, `user_id`) USING BTREE,
  INDEX `idx_ai_user_usage_daily_user_date`(`user_id` ASC, `stat_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户每日用量与实扣汇总' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成功能作者',
  `form_col_num` int NULL DEFAULT 1 COMMENT '表单布局（单列 双列 三列）',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码生成业务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table
-- ----------------------------


-- ----------------------------
-- Records of gen_table_column
-- ----------------------------

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 118 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2026-06-01 01:27:16', '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2026-06-01 01:27:16', '', NULL, '初始化密码 123456');
INSERT INTO `sys_config` VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2026-06-01 01:27:16', '', NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` VALUES (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'false', 'Y', 'admin', '2026-06-01 01:27:16', 'admin', '2026-06-01 12:59:22', '是否开启验证码功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'true', 'Y', 'admin', '2026-06-01 01:27:16', 'admin', '2026-07-18 11:01:19', '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y', 'admin', '2026-06-01 01:27:16', '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
INSERT INTO `sys_config` VALUES (7, '用户管理-初始密码修改策略', 'sys.account.initPasswordModify', '1', 'Y', 'admin', '2026-06-01 01:27:16', '', NULL, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
INSERT INTO `sys_config` VALUES (8, '用户管理-账号密码更新周期', 'sys.account.passwordValidateDays', '0', 'Y', 'admin', '2026-06-01 01:27:16', '', NULL, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');
INSERT INTO `sys_config` VALUES (9, '用户管理-密码字符范围', 'sys.account.chrtype', '0', 'Y', 'admin', '2026-06-01 01:27:16', '', NULL, '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）');
INSERT INTO `sys_config` VALUES (101, 'AI订阅余额购买开关', 'ai.subscription.balancePurchaseEnabled', 'true', 'Y', 'admin', '2026-06-07 01:30:20', 'admin', '2026-06-09 18:27:13', '控制用户是否可以使用钱包余额购买订阅');
INSERT INTO `sys_config` VALUES (104, '用户管理-注册用户默认角色', 'sys.user.registerRole', 'common', 'Y', 'admin', '2026-06-27 00:00:00', '', NULL, '新注册用户的默认角色键（role_key，如 common），为空则不分配角色');
INSERT INTO `sys_config` VALUES (105, '邀请返利总开关', 'ai.invite.enabled', 'false', 'Y', 'admin', '2026-06-29 00:56:49', 'admin', '2026-08-14 11:38:56', '是否开启邀请返利功能（true开启，false关闭），关闭后注册不再绑定邀请人，充值不再产生返利');
INSERT INTO `sys_config` VALUES (106, '邀请返利比例', 'ai.invite.rebateRate', '5', 'Y', 'admin', '2026-06-29 00:56:49', 'admin', '2026-08-14 11:38:56', '邀请返利全局比例（百分比，0-100），被邀请人充值时邀请人按此比例获得返利');
INSERT INTO `sys_config` VALUES (107, '邀请返利冻结期', 'ai.invite.freezeHours', '0', 'Y', 'admin', '2026-06-29 00:56:49', 'admin', '2026-08-14 11:38:56', '返利冻结时长（小时，0-720），产生的返利先冻结，到期后才能领取');
INSERT INTO `sys_config` VALUES (108, '邀请返利有效期', 'ai.invite.durationDays', '0', 'Y', 'admin', '2026-06-29 00:56:49', 'admin', '2026-08-14 11:38:56', '返利有效期（天，0-3650，从被邀请人注册起算，0=永久有效）');
INSERT INTO `sys_config` VALUES (109, '邀请返利单人上限', 'ai.invite.perInviteeCap', '0', 'Y', 'admin', '2026-06-29 00:56:49', 'admin', '2026-08-14 11:38:56', '单个被邀请人累计返利上限（美元，0=无上限）');
INSERT INTO `sys_config` VALUES (111, 'AI计费-priority倍率', 'ai.cost.priorityMultiplier', '2', 'Y', 'admin', '2026-07-02 23:32:50', '', NULL, '上游 service_tier 为 priority 时模型计费的乘数（默认2）；其他 tier 不受影响');
INSERT INTO `sys_config` VALUES (112, 'AI计费-长上下文规则', 'ai.cost.longContextRules', '{\n  \"gpt-5.6-sol\": {\n    \"thresholdTokens\": 272000,\n    \"inputMultiplier\": 2,\n    \"outputMultiplier\": 1.5\n  },\n  \"gpt-5.6-terra\": {\n    \"thresholdTokens\": 272000,\n    \"inputMultiplier\": 2,\n    \"outputMultiplier\": 1.5\n  },\n  \"gpt-5.6-luna\": {\n    \"thresholdTokens\": 272000,\n    \"inputMultiplier\": 2,\n    \"outputMultiplier\": 1.5\n  }\n}', 'Y', 'admin', '2026-07-14 01:25:29', 'admin', '2026-07-14 23:59:47', '按模型配置长上下文计费规则，JSON字段为 thresholdTokens、inputMultiplier、outputMultiplier；空对象表示关闭');
INSERT INTO `sys_config` VALUES (113, 'AI请求预占金额', 'ai.billing.reserveAmount', '0.01', 'Y', 'admin', '2026-07-16 23:20:59', 'admin', '2026-07-16 23:52:03', 'AI请求发送上游前必须足额冻结的金额（美元），配置非法时拒绝请求');
INSERT INTO `sys_config` VALUES (114, 'AI在线充值业务开关', 'ai.pay.onlineRechargeEnabled', 'false', 'Y', 'admin', '2026-07-30 09:36:23', 'admin', '2026-07-30 09:36:49', '控制用户是否可以创建新的在线充值订单；关闭后已有订单的回调、查询和补偿仍继续处理');
INSERT INTO `sys_config` VALUES (115, '用户管理-新人试用套餐', 'registerTrialPlanId', '6', 'Y', 'admin', '2026-08-02 16:33:13', 'admin', '2026-08-02 16:33:37', '新用户注册后自动发放的订阅套餐ID；留空或非正整数时不发放');
INSERT INTO `sys_config` VALUES (116, 'AI请求最低计费金额', 'ai.billing.minimumAmount', '0.001', 'Y', 'admin', '2026-08-27 13:06:23', 'admin', '2026-09-04 11:43:31', '单次AI请求最终计费的最低金额（美元），仅对实际费用大于0的请求生效，0表示关闭');
INSERT INTO `sys_config` VALUES (117, '每日签到开关', 'ai.checkin.enabled', 'false', 'Y', 'admin', '2026-09-04 11:41:50', 'admin', '2026-09-04 11:42:08', '是否开启每日签到（true开启，false关闭）；关闭后隐藏签到卡片并拒绝签到，未配置时默认开启');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 200 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (100, 0, '0', 'Default API', 0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-06-01 01:27:16', 'admin', '2026-06-10 14:04:08');
INSERT INTO `sys_dept` VALUES (101, 100, '0,100', 'CHAI总公司', 1, 'CHAI', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-06-01 01:27:16', 'admin', '2026-06-10 14:03:07');
INSERT INTO `sys_dept` VALUES (102, 100, '0,100', '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '2', 'admin', '2026-06-01 01:27:16', '', NULL);
INSERT INTO `sys_dept` VALUES (103, 101, '0,100,101', '研发部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-06-01 01:27:16', '', NULL);
INSERT INTO `sys_dept` VALUES (104, 101, '0,100,101', '市场部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-06-01 01:27:16', '', NULL);
INSERT INTO `sys_dept` VALUES (105, 101, '0,100,101', '测试部门', 3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-06-01 01:27:16', '', NULL);
INSERT INTO `sys_dept` VALUES (106, 101, '0,100,101', '财务部门', 4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-06-01 01:27:16', '', NULL);
INSERT INTO `sys_dept` VALUES (107, 101, '0,100,101', '运维部门', 5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2026-06-01 01:27:16', '', NULL);
INSERT INTO `sys_dept` VALUES (108, 102, '0,100,102', '市场部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '2', 'admin', '2026-06-01 01:27:16', '', NULL);
INSERT INTO `sys_dept` VALUES (109, 102, '0,100,102', '财务部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '2', 'admin', '2026-06-01 01:27:16', '', NULL);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 121 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '性别男');
INSERT INTO `sys_dict_data` VALUES (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '性别女');
INSERT INTO `sys_dict_data` VALUES (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '性别未知');
INSERT INTO `sys_dict_data` VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '显示菜单');
INSERT INTO `sys_dict_data` VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (8, 1, '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (9, 2, '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '默认分组');
INSERT INTO `sys_dict_data` VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '系统分组');
INSERT INTO `sys_dict_data` VALUES (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '系统默认是');
INSERT INTO `sys_dict_data` VALUES (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '系统默认否');
INSERT INTO `sys_dict_data` VALUES (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '通知');
INSERT INTO `sys_dict_data` VALUES (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '公告');
INSERT INTO `sys_dict_data` VALUES (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '关闭状态');
INSERT INTO `sys_dict_data` VALUES (18, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '其他操作');
INSERT INTO `sys_dict_data` VALUES (19, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '新增操作');
INSERT INTO `sys_dict_data` VALUES (20, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '修改操作');
INSERT INTO `sys_dict_data` VALUES (21, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '删除操作');
INSERT INTO `sys_dict_data` VALUES (22, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '授权操作');
INSERT INTO `sys_dict_data` VALUES (23, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '导出操作');
INSERT INTO `sys_dict_data` VALUES (24, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '导入操作');
INSERT INTO `sys_dict_data` VALUES (25, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '强退操作');
INSERT INTO `sys_dict_data` VALUES (26, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '生成操作');
INSERT INTO `sys_dict_data` VALUES (27, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '清空操作');
INSERT INTO `sys_dict_data` VALUES (28, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (29, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (100, 1, '兑换码', '1', 'ai_recharge_type', '', 'primary', 'Y', '0', 'admin', '2026-06-07 02:58:45', '', NULL, '兑换码充值');
INSERT INTO `sys_dict_data` VALUES (101, 2, '在线支付', '2', 'ai_recharge_type', '', 'success', 'N', '0', 'admin', '2026-06-07 02:58:45', '', NULL, '在线支付充值');
INSERT INTO `sys_dict_data` VALUES (102, 3, '管理员调整', '3', 'ai_recharge_type', '', 'warning', 'N', '0', 'admin', '2026-06-07 02:58:45', '', NULL, '管理员手动调整余额');
INSERT INTO `sys_dict_data` VALUES (110, 1, 'OpenAI', 'openai', 'ai_platform_name', '', 'primary', 'Y', '0', 'admin', '2026-06-27 19:53:19', '', NULL, 'AI平台：OpenAI');
INSERT INTO `sys_dict_data` VALUES (111, 2, '智谱', 'zhipu', 'ai_platform_name', '', 'success', 'N', '1', 'admin', '2026-06-27 19:53:19', 'admin', '2026-07-31 16:12:43', 'AI平台：智谱');
INSERT INTO `sys_dict_data` VALUES (112, 3, 'DeepSeek', 'deepseek', 'ai_platform_name', '', 'warning', 'N', '0', 'admin', '2026-06-27 19:53:19', 'admin', '2026-07-31 16:12:47', 'AI平台：DeepSeek');
INSERT INTO `sys_dict_data` VALUES (113, 4, 'Anthropic', 'anthropic', 'ai_platform_name', '', 'info', 'N', '1', 'admin', '2026-06-27 19:53:19', 'admin', '2026-06-27 20:45:51', 'AI平台：Anthropic');
INSERT INTO `sys_dict_data` VALUES (119, 5, '每日签到', '5', 'ai_recharge_type', '', 'success', 'N', '0', 'admin', '2026-07-05 01:10:28', '', NULL, '每日签到奖励');
INSERT INTO `sys_dict_data` VALUES (120, 6, '邀请返利', '6', 'ai_recharge_type', '', 'success', 'N', '0', 'admin', '2026-07-05 19:02:56', '', NULL, '邀请返利领取');

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 103 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (4, '任务状态', 'sys_job_status', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '任务状态列表');
INSERT INTO `sys_dict_type` VALUES (5, '任务分组', 'sys_job_group', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '任务分组列表');
INSERT INTO `sys_dict_type` VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (10, '系统状态', 'sys_common_status', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (100, '充值类型', 'ai_recharge_type', '0', 'admin', '2026-06-07 02:58:45', '', NULL, '充值记录类型列表');
INSERT INTO `sys_dict_type` VALUES (102, 'AI平台名称', 'ai_platform_name', '0', 'admin', '2026-06-27 19:53:19', 'admin', '2026-06-27 19:53:19', 'AI平台名称列表');

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`  (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`, `job_name`, `job_group`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务调度表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_job
-- ----------------------------
INSERT INTO `sys_job` VALUES (1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_job` VALUES (2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_job` VALUES (3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?', '3', '1', '1', 'admin', '2026-06-01 01:27:16', '', NULL, '');

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`  (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '异常信息',
  `start_time` datetime NULL DEFAULT NULL COMMENT '执行开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '执行结束时间',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务调度日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_job_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor`  (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '提示消息',
  `login_time` datetime NULL DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE,
  INDEX `idx_sys_logininfor_s`(`status` ASC) USING BTREE,
  INDEX `idx_sys_logininfor_lt`(`login_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1419 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统访问记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_mail_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_mail_config`;
CREATE TABLE `sys_mail_config`  (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT 'SMTP服务器',
  `port` int NULL DEFAULT 25 COMMENT 'SMTP端口',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT 'SMTP用户名',
  `encrypted_password` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '加密后的SMTP密码',
  `from_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '发件邮箱',
  `from_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '发件人名称',
  `ssl_enable` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否启用SSL（Y是 N否）',
  `starttls_enable` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'Y' COMMENT '是否启用STARTTLS（Y是 N否）',
  `auth_enable` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'Y' COMMENT '是否启用认证（Y是 N否）',
  `enabled` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否启用配置（Y是 N否）',
  `timeout` int NULL DEFAULT 10000 COMMENT '超时时间（毫秒）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `email_verify_enable` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'Y' COMMENT '是否开启邮箱验证（Y开启 N关闭）',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '邮件配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_mail_config
-- ----------------------------
INSERT INTO `sys_mail_config` VALUES (2, 'smtp.qq.com', 465, 'cbcicu@qq.com', '/Ux+FJokGPk2Z0jp32/vV8fIne/4lIXuwZn0zTetI7RAXIoCM1imd+ZA4ek=', 'cbcicu@qq.com', 'DEFAULT API', 'Y', 'N', 'Y', 'Y', 10000, 'admin', '2026-06-27 13:48:12', 'admin', '2026-06-27 14:20:47', '', 'Y');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '路由名称',
  `is_frame` int NULL DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3164 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 1, 'system', NULL, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', '2026-06-01 01:27:16', '', NULL, '系统管理目录');
INSERT INTO `sys_menu` VALUES (2, '系统监控', 0, 2, 'monitor', NULL, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', '2026-06-01 01:27:16', '', NULL, '系统监控目录');
INSERT INTO `sys_menu` VALUES (3, '系统工具', 0, 3, 'tool', NULL, '', '', 1, 0, 'M', '1', '1', '', 'tool', 'admin', '2026-06-01 01:27:16', 'admin', '2026-06-11 11:27:41', '系统工具目录');
INSERT INTO `sys_menu` VALUES (4, '官网', 0, 5, '/home?redirect', NULL, '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', '2026-06-01 01:27:16', 'admin', '2026-09-04 00:32:36', '若依官网地址');
INSERT INTO `sys_menu` VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', '2026-06-01 01:27:16', '', NULL, '用户管理菜单');
INSERT INTO `sys_menu` VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', '2026-06-01 01:27:16', '', NULL, '角色管理菜单');
INSERT INTO `sys_menu` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', '2026-06-01 01:27:16', '', NULL, '菜单管理菜单');
INSERT INTO `sys_menu` VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', '2026-06-01 01:27:16', '', NULL, '部门管理菜单');
INSERT INTO `sys_menu` VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', '2026-06-01 01:27:16', '', NULL, '岗位管理菜单');
INSERT INTO `sys_menu` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', '2026-06-01 01:27:16', '', NULL, '字典管理菜单');
INSERT INTO `sys_menu` VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', '2026-06-01 01:27:16', '', NULL, '参数设置菜单');
INSERT INTO `sys_menu` VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 'admin', '2026-06-01 01:27:16', '', NULL, '通知公告菜单');
INSERT INTO `sys_menu` VALUES (108, '日志管理', 1, 9, 'log', '', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', '2026-06-01 01:27:16', '', NULL, '日志管理菜单');
INSERT INTO `sys_menu` VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', '2026-06-01 01:27:16', '', NULL, '在线用户菜单');
INSERT INTO `sys_menu` VALUES (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job', 'admin', '2026-06-01 01:27:16', '', NULL, '定时任务菜单');
INSERT INTO `sys_menu` VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list', 'druid', 'admin', '2026-06-01 01:27:16', '', NULL, '数据监控菜单');
INSERT INTO `sys_menu` VALUES (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', '', 1, 0, 'C', '0', '0', 'monitor:server:list', 'server', 'admin', '2026-06-01 01:27:16', '', NULL, '服务监控菜单');
INSERT INTO `sys_menu` VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 'admin', '2026-06-01 01:27:16', '', NULL, '缓存监控菜单');
INSERT INTO `sys_menu` VALUES (114, '缓存列表', 2, 6, 'cacheList', 'monitor/cache/list', '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis-list', 'admin', '2026-06-01 01:27:16', '', NULL, '缓存列表菜单');
INSERT INTO `sys_menu` VALUES (115, '表单构建', 3, 1, 'build', 'tool/build/index', '', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 'admin', '2026-06-01 01:27:16', '', NULL, '表单构建菜单');
INSERT INTO `sys_menu` VALUES (116, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin', '2026-06-01 01:27:16', '', NULL, '代码生成菜单');
INSERT INTO `sys_menu` VALUES (117, '系统接口', 3, 3, 'swagger', 'tool/swagger/index', '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list', 'swagger', 'admin', '2026-06-01 01:27:16', '', NULL, '系统接口菜单');
INSERT INTO `sys_menu` VALUES (118, '邮箱管理', 1, 9, 'mail', 'system/mail/config', '', '', 1, 0, 'C', '0', '0', 'system:mail:query', 'email', 'admin', '2026-06-01 11:17:34', '', NULL, '邮箱配置菜单');
INSERT INTO `sys_menu` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', '2026-06-01 01:27:16', '', NULL, '操作日志菜单');
INSERT INTO `sys_menu` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', '2026-06-01 01:27:16', '', NULL, '登录日志菜单');
INSERT INTO `sys_menu` VALUES (1000, '用户查询', 100, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1001, '用户新增', 100, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1002, '用户修改', 100, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1003, '用户删除', 100, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1004, '用户导出', 100, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1005, '用户导入', 100, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1006, '重置密码', 100, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1007, '角色查询', 101, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1008, '角色新增', 101, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1009, '角色修改', 101, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1010, '角色删除', 101, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1011, '角色导出', 101, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1012, '菜单查询', 102, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1013, '菜单新增', 102, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1014, '菜单修改', 102, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1015, '菜单删除', 102, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1016, '部门查询', 103, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1017, '部门新增', 103, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1018, '部门修改', 103, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1019, '部门删除', 103, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1020, '岗位查询', 104, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1021, '岗位新增', 104, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1022, '岗位修改', 104, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1023, '岗位删除', 104, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1024, '岗位导出', 104, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1025, '字典查询', 105, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1026, '字典新增', 105, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1027, '字典修改', 105, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1028, '字典删除', 105, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1029, '字典导出', 105, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1030, '参数查询', 106, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1031, '参数新增', 106, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1032, '参数修改', 106, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1033, '参数删除', 106, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1034, '参数导出', 106, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1035, '公告查询', 107, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1036, '公告新增', 107, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1037, '公告修改', 107, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1038, '公告删除', 107, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1039, '操作查询', 500, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1040, '操作删除', 500, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1041, '日志导出', 500, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1042, '登录查询', 501, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1043, '登录删除', 501, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1044, '日志导出', 501, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1045, '账户解锁', 501, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1046, '在线查询', 109, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1047, '批量强退', 109, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1048, '单条强退', 109, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1049, '任务查询', 110, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1050, '任务新增', 110, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1051, '任务修改', 110, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1052, '任务删除', 110, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1053, '状态修改', 110, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1054, '任务导出', 110, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1055, '生成查询', 116, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1056, '生成修改', 116, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1057, '生成删除', 116, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1058, '导入代码', 116, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1059, '预览代码', 116, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1060, '生成代码', 116, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1061, '邮箱查询', 118, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:mail:query', '#', 'admin', '2026-06-01 11:17:34', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1062, '邮箱修改', 118, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:mail:edit', '#', 'admin', '2026-06-01 11:17:34', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1063, '邮箱测试', 118, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'system:mail:test', '#', 'admin', '2026-06-01 11:17:34', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3000, 'AI网关', 0, 0, 'aigate', NULL, '', '', 1, 0, 'M', '0', '0', '', 'skill', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-02 09:27:33', 'AI网关目录');
INSERT INTO `sys_menu` VALUES (3003, '平台管理', 3000, 7, 'platform', 'aigate/platform/index', '', '', 1, 0, 'C', '0', '0', 'aigate:platform:list', 'link', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', 'AI平台管理菜单');
INSERT INTO `sys_menu` VALUES (3004, '模型管理', 3000, 2, 'model', 'aigate/model/index', '', '', 1, 0, 'C', '0', '0', 'aigate:model:list', 'code', 'admin', '2026-06-01 21:14:16', '', NULL, 'AI模型管理菜单');
INSERT INTO `sys_menu` VALUES (3008, 'API密钥', 3100, 1, 'api-key', 'aigate/api-key/index', '', '', 1, 0, 'C', '0', '0', 'aigate:apiKey:list', 'validCode', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-02 15:15:26', 'AI API Key管理菜单');
INSERT INTO `sys_menu` VALUES (3010, '全局使用记录', 3000, 7, 'log', 'aigate/log/index', '', '', 1, 0, 'C', '0', '0', 'aigate:log:list', 'log', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-11 12:15:58', '全局使用记录菜单');
INSERT INTO `sys_menu` VALUES (3011, '兑换码管理', 3000, 8, 'redemption-code', 'aigate/redemption-code/index', '', '', 1, 0, 'C', '0', '0', 'aigate:redemptionCode:list', 'button', 'admin', '2026-06-02 11:23:04', 'admin', '2026-06-02 11:26:25', 'AI兑换码管理菜单');
INSERT INTO `sys_menu` VALUES (3012, '钱包', 3100, 2, 'wallet', 'aigate/wallet/index', '', '', 1, 0, 'C', '0', '0', 'aigate:wallet:list', 'money', 'admin', '2026-06-02 14:06:21', 'admin', '2026-06-02 15:14:51', 'AI钱包管理菜单');
INSERT INTO `sys_menu` VALUES (3014, '订阅管理', 3000, 9, 'subscription', 'aigate/subscription/index', '', '', 1, 0, 'C', '0', '0', 'aigate:subscription:list', 'money', 'admin', '2026-06-07 01:30:20', '', NULL, 'AI订阅管理菜单');
INSERT INTO `sys_menu` VALUES (3058, '平台查询', 3003, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:platform:query', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', '');
INSERT INTO `sys_menu` VALUES (3059, '平台新增', 3003, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:platform:add', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', '');
INSERT INTO `sys_menu` VALUES (3060, '平台修改', 3003, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:platform:edit', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', '');
INSERT INTO `sys_menu` VALUES (3061, '平台删除', 3003, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:platform:remove', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', '');
INSERT INTO `sys_menu` VALUES (3062, '模型查询', 3004, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:model:query', '#', 'admin', '2026-06-01 21:14:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3063, '模型新增', 3004, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:model:add', '#', 'admin', '2026-06-01 21:14:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3064, '模型修改', 3004, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:model:edit', '#', 'admin', '2026-06-01 21:14:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3065, '模型删除', 3004, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:model:remove', '#', 'admin', '2026-06-01 21:14:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3079, 'API Key查询', 3008, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:apiKey:query', '#', 'admin', '2026-06-01 21:14:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3080, 'API Key新增', 3008, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:apiKey:add', '#', 'admin', '2026-06-01 21:14:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3081, 'API Key修改', 3008, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:apiKey:edit', '#', 'admin', '2026-06-01 21:14:16', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3086, '全局使用记录查询', 3010, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:log:query', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-11 12:15:58', '');
INSERT INTO `sys_menu` VALUES (3087, '全局使用记录删除', 3010, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:log:remove', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-11 12:15:58', '');
INSERT INTO `sys_menu` VALUES (3088, '兑换码查询', 3011, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:redemptionCode:query', '#', 'admin', '2026-06-02 11:23:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3089, '兑换码新增', 3011, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:redemptionCode:add', '#', 'admin', '2026-06-02 11:23:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3090, '兑换码修改', 3011, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:redemptionCode:edit', '#', 'admin', '2026-06-02 11:23:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3091, '兑换码删除', 3011, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:redemptionCode:remove', '#', 'admin', '2026-06-02 11:23:04', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3092, '钱包查询', 3012, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:wallet:query', '#', 'admin', '2026-06-02 14:06:21', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3093, '兑换充值', 3012, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:wallet:redeem', '#', 'admin', '2026-06-02 14:06:21', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3100, '常规服务', 0, 4, 'general-service', NULL, '', '', 1, 0, 'M', '0', '0', '', 'peoples', 'admin', '2026-06-02 15:00:58', '', NULL, '常规服务目录');
INSERT INTO `sys_menu` VALUES (3103, '个人使用记录', 3100, 4, 'usage-log', 'aigate/usage-log/index', '', '', 1, 0, 'C', '0', '0', 'aigate:usageLog:list', 'log', 'admin', '2026-06-02 15:00:58', 'admin', '2026-06-11 12:15:58', '个人使用记录菜单');
INSERT INTO `sys_menu` VALUES (3104, '我的订阅', 3100, 3, 'user-subscription', 'aigate/user-subscription/index', '', '', 1, 0, 'C', '0', '0', 'aigate:subscription:self', 'money', 'admin', '2026-09-10 00:00:00', '', NULL, '个人订阅管理菜单');
INSERT INTO `sys_menu` VALUES (3116, '个人使用记录查询', 3103, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:usageLog:query', '#', 'admin', '2026-06-02 15:00:58', 'admin', '2026-06-11 12:15:58', '');
INSERT INTO `sys_menu` VALUES (3117, 'AI聊天', 3100, 4, 'chat', 'aigate/chat/index', '', '', 1, 0, 'C', '0', '0', 'aigate:chat:list', 'message', 'admin', '2026-06-02 15:52:49', '', NULL, 'AI聊天对话');
INSERT INTO `sys_menu` VALUES (3118, '聊天发送', 3117, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:chat:send', '#', 'admin', '2026-06-02 15:52:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3131, '订阅查询', 3014, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:subscription:query', '#', 'admin', '2026-06-07 01:30:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3132, '套餐新增', 3014, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:subscription:add', '#', 'admin', '2026-06-07 01:30:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3133, '套餐修改', 3014, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:subscription:edit', '#', 'admin', '2026-06-07 01:30:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3134, '套餐删除', 3014, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:subscription:remove', '#', 'admin', '2026-06-07 01:30:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3135, '授予订阅', 3014, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:subscription:grant', '#', 'admin', '2026-06-07 01:30:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3136, '取消订阅', 3014, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:subscription:cancel', '#', 'admin', '2026-06-07 01:30:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3137, '订阅配置', 3014, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:subscription:config', '#', 'admin', '2026-06-07 01:30:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3138, '购买订阅', 3104, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:subscription:purchase', '#', 'admin', '2026-06-07 01:30:20', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3150, '邀请管理', 3000, 10, 'invite', 'aigate/invite/index', '', '', 1, 0, 'C', '0', '0', 'aigate:invite:list', 'people', 'admin', '2026-06-29 00:56:49', '', NULL, '邀请返利管理菜单');
INSERT INTO `sys_menu` VALUES (3151, '邀请查询', 3150, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:invite:query', '#', 'admin', '2026-06-29 00:56:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3152, '返利明细查询', 3150, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:invite:rebateQuery', '#', 'admin', '2026-06-29 00:56:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3153, '设置专属比例', 3150, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:invite:rate', '#', 'admin', '2026-06-29 00:56:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3154, '重置邀请码', 3150, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:invite:resetCode', '#', 'admin', '2026-06-29 00:56:49', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3155, '邀请参数配置', 3150, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:invite:config', '#', 'admin', '2026-07-29 19:22:13', '', NULL, '邀请返利全局参数配置');
INSERT INTO `sys_menu` VALUES (3156, '结算异常', 3000, 11, 'billing-settlement', 'aigate/billing-settlement/index', '', '', 1, 0, 'C', '0', '0', 'aigate:billingSettlement:list', 'button', 'admin', '2026-08-07 00:00:00', 'admin', '2026-08-07 16:44:44', '异步结算失败任务处置中心');
INSERT INTO `sys_menu` VALUES (3157, '结算失败重试', 3156, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:billingSettlement:retry', '#', 'admin', '2026-08-07 00:00:00', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3158, '结算失败核销', 3156, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:billingSettlement:writeOff', '#', 'admin', '2026-08-07 00:00:00', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3162, '上游失败事件', 3000, 12, 'upstream-failure', 'aigate/upstream-failure/index', '', '', 1, 0, 'C', '0', '0', 'aigate:upstreamFailure:list', 'warning', 'admin', '2026-09-17 00:00:00', '', NULL, 'CLIProxyAPI上游失败事件监控');
INSERT INTO `sys_menu` VALUES (3159, '上游失败事件详情', 3162, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:upstreamFailure:query', '#', 'admin', '2026-09-17 00:00:00', '', NULL, '');
INSERT INTO `sys_menu` VALUES (3160, '上游失败事件删除', 3162, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'aigate:upstreamFailure:remove', '#', 'admin', '2026-09-17 00:00:00', '', NULL, '');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob NULL COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (3, '若依开源框架介绍', '1', 0x3C703E3C7370616E207374796C653D22636F6C6F723A20726762283233302C20302C2030293B223EE9A1B9E79BAEE4BB8BE7BB8D3C2F7370616E3E3C2F703E3C703E3C666F6E7420636F6C6F723D2223333333333333223E52756F5969E5BC80E6BA90E9A1B9E79BAEE698AFE4B8BAE4BC81E4B89AE794A8E688B7E5AE9AE588B6E79A84E5908EE58FB0E8849AE6898BE69EB6E6A186E69EB6EFBC8CE4B8BAE4BC81E4B89AE68993E980A0E79A84E4B880E7AB99E5BC8FE8A7A3E586B3E696B9E6A188EFBC8CE9998DE4BD8EE4BC81E4B89AE5BC80E58F91E68890E69CACEFBC8CE68F90E58D87E5BC80E58F91E69588E78E87E38082E4B8BBE8A681E58C85E68BACE794A8E688B7E7AEA1E79086E38081E8A792E889B2E7AEA1E79086E38081E983A8E997A8E7AEA1E79086E38081E88F9CE58D95E7AEA1E79086E38081E58F82E695B0E7AEA1E79086E38081E5AD97E585B8E7AEA1E79086E380813C2F666F6E743E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE5B297E4BD8DE7AEA1E790863C2F7370616E3E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE38081E5AE9AE697B6E4BBBBE58AA13C2F7370616E3E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE380813C2F7370616E3E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE69C8DE58AA1E79B91E68EA7E38081E799BBE5BD95E697A5E5BF97E38081E6938DE4BD9CE697A5E5BF97E38081E4BBA3E7A081E7949FE68890E7AD89E58A9FE883BDE38082E585B6E4B8ADEFBC8CE8BF98E694AFE68C81E5A49AE695B0E68DAEE6BA90E38081E695B0E68DAEE69D83E99990E38081E59BBDE99985E58C96E380815265646973E7BC93E5AD98E38081446F636B6572E983A8E7BDB2E38081E6BB91E58AA8E9AA8CE8AF81E7A081E38081E7ACACE4B889E696B9E8AEA4E8AF81E799BBE5BD95E38081E58886E5B883E5BC8FE4BA8BE58AA1E380813C2F7370616E3E3C666F6E7420636F6C6F723D2223333333333333223EE58886E5B883E5BC8FE69687E4BBB6E5AD98E582A83C2F666F6E743E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE38081E58886E5BA93E58886E8A1A8E5A484E79086E7AD89E68A80E69CAFE789B9E782B9E380823C2F7370616E3E3C2F703E3C703E3C696D67207372633D2268747470733A2F2F666F727564612E67697465652E636F6D2F696D616765732F313737333933313834383334323433393033322F61346432323331335F313831353039352E706E6722207374796C653D2277696474683A20363470783B223E3C62723E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A20726762283233302C20302C2030293B223EE5AE98E7BD91E58F8AE6BC94E7A4BA3C2F7370616E3E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE88BA5E4BE9DE5AE98E7BD91E59CB0E59D80EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F72756F79692E7669703C2F613E3C6120687265663D22687474703A2F2F72756F79692E76697022207461726765743D225F626C616E6B223E3C2F613E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE88BA5E4BE9DE69687E6A1A3E59CB0E59D80EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F646F632E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F646F632E72756F79692E7669703C2F613E3C62723E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE6BC94E7A4BAE59CB0E59D80E38090E4B88DE58886E7A6BBE78988E38091EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F64656D6F2E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F64656D6F2E72756F79692E7669703C2F613E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE6BC94E7A4BAE59CB0E59D80E38090E58886E7A6BBE78988E69CACE38091EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F7675652E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F7675652E72756F79692E7669703C2F613E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE6BC94E7A4BAE59CB0E59D80E38090E5BEAEE69C8DE58AA1E78988E38091EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F636C6F75642E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F636C6F75642E72756F79692E7669703C2F613E3C2F703E3C703E3C7370616E207374796C653D22636F6C6F723A207267622835312C2035312C203531293B223EE6BC94E7A4BAE59CB0E59D80E38090E7A7BBE58AA8E7ABAFE78988E38091EFBC9A266E6273703B3C2F7370616E3E3C6120687265663D22687474703A2F2F68352E72756F79692E76697022207461726765743D225F626C616E6B223E687474703A2F2F68352E72756F79692E7669703C2F613E3C2F703E3C703E3C6272207374796C653D22636F6C6F723A207267622834382C2034392C203531293B20666F6E742D66616D696C793A202671756F743B48656C766574696361204E6575652671756F743B2C2048656C7665746963612C20417269616C2C2073616E732D73657269663B20666F6E742D73697A653A20313270783B223E3C2F703E, '0', 'admin', '2026-06-01 01:27:16', '', NULL, '管理员');
INSERT INTO `sys_notice` VALUES (10, '4324', '2', 0x3C703E343332343233343C2F703E, '0', 'admin', '2026-07-20 02:16:37', '', NULL, NULL);
INSERT INTO `sys_notice` VALUES (11, '前端布局重构 | 关于倍率调整 ', '1', 0x3C703E312EE4BC98E58C96E9A1B5E99DA2E698BEE7A4BAEFBC8CE5BFABE68DB7E696B9E4BEBFE79A84E8BF9BE8A18CE69FA5E79C8BEFBC8CE697A0E99C80E69DA5E59B9EE8B7B3E8BDAC7E3C2F703E3C703E322E706C75732F70726FE5808DE78E87E4B8B4E697B6E9998DE4BD8EE588B020302E31EFBC8CE681A2E5A48DE697B6E997B4E58FA6E8A18CE9809AE79FA53C2F703E3C703E3C62723E3C2F703E3C703E3C62723E3C2F703E, '0', 'admin', '2026-07-26 12:16:54', 'admin', '2026-07-31 10:12:12', NULL);

-- ----------------------------
-- Table structure for sys_notice_read
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice_read`;
CREATE TABLE `sys_notice_read`  (
  `read_id` bigint NOT NULL AUTO_INCREMENT COMMENT '已读主键',
  `notice_id` int NOT NULL COMMENT '公告id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `read_time` datetime NOT NULL COMMENT '阅读时间',
  PRIMARY KEY (`read_id`) USING BTREE,
  UNIQUE INDEX `uk_user_notice`(`user_id` ASC, `notice_id` ASC) USING BTREE COMMENT '同一用户同一公告只记录一次'
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公告已读记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice_read
-- ----------------------------
INSERT INTO `sys_notice_read` VALUES (1, 3, 1, '2026-06-01 01:31:25');
INSERT INTO `sys_notice_read` VALUES (8, 3, 101, '2026-06-10 14:11:15');
INSERT INTO `sys_notice_read` VALUES (11, 3, 106, '2026-07-20 02:14:11');
INSERT INTO `sys_notice_read` VALUES (14, 10, 1, '2026-07-20 02:17:03');
INSERT INTO `sys_notice_read` VALUES (15, 10, 107, '2026-07-26 11:34:37');
INSERT INTO `sys_notice_read` VALUES (16, 3, 107, '2026-07-26 12:12:57');
INSERT INTO `sys_notice_read` VALUES (19, 11, 107, '2026-07-26 12:19:01');
INSERT INTO `sys_notice_read` VALUES (20, 11, 1, '2026-07-26 12:19:19');

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '模块标题',
  `business_type` int NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求方式',
  `operator_type` int NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '返回参数',
  `status` int NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint NULL DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`) USING BTREE,
  INDEX `idx_sys_oper_log_bt`(`business_type` ASC) USING BTREE,
  INDEX `idx_sys_oper_log_s`(`status` ASC) USING BTREE,
  INDEX `idx_sys_oper_log_ot`(`oper_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 551 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '岗位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, 'ceo', '董事长', 1, '0', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_post` VALUES (2, 'se', '项目经理', 2, '0', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_post` VALUES (3, 'hr', '人力资源', 3, '0', 'admin', '2026-06-01 01:27:16', '', NULL, '');
INSERT INTO `sys_post` VALUES (4, 'user', '普通员工', 4, '0', 'admin', '2026-06-01 01:27:16', '', NULL, '');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', '2026-06-01 01:27:16', '', NULL, '超级管理员');
INSERT INTO `sys_role` VALUES (2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', '2026-06-01 01:27:16', 'admin', '2026-09-04 00:35:43', '普通角色');
INSERT INTO `sys_role` VALUES (100, 'SVIP', 'svip', 3, '1', 1, 1, '0', '2', 'admin', '2026-06-06 23:31:07', '', NULL, NULL);
INSERT INTO `sys_role` VALUES (101, '极简', 'simple', 3, '1', 1, 1, '0', '2', 'admin', '2026-06-10 12:59:34', 'admin', '2026-06-23 10:24:08', NULL);

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色和部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO `sys_role_dept` VALUES (2, 100);
INSERT INTO `sys_role_dept` VALUES (2, 101);
INSERT INTO `sys_role_dept` VALUES (2, 105);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 3117);
INSERT INTO `sys_role_menu` VALUES (1, 3118);
INSERT INTO `sys_role_menu` VALUES (1, 3014);
INSERT INTO `sys_role_menu` VALUES (1, 3131);
INSERT INTO `sys_role_menu` VALUES (1, 3132);
INSERT INTO `sys_role_menu` VALUES (1, 3133);
INSERT INTO `sys_role_menu` VALUES (1, 3134);
INSERT INTO `sys_role_menu` VALUES (1, 3135);
INSERT INTO `sys_role_menu` VALUES (1, 3136);
INSERT INTO `sys_role_menu` VALUES (1, 3137);
INSERT INTO `sys_role_menu` VALUES (1, 3150);
INSERT INTO `sys_role_menu` VALUES (1, 3151);
INSERT INTO `sys_role_menu` VALUES (1, 3152);
INSERT INTO `sys_role_menu` VALUES (1, 3153);
INSERT INTO `sys_role_menu` VALUES (1, 3154);
INSERT INTO `sys_role_menu` VALUES (1, 3155);
INSERT INTO `sys_role_menu` VALUES (1, 3156);
INSERT INTO `sys_role_menu` VALUES (1, 3157);
INSERT INTO `sys_role_menu` VALUES (1, 3158);
INSERT INTO `sys_role_menu` VALUES (2, 4);
INSERT INTO `sys_role_menu` VALUES (2, 3100);
INSERT INTO `sys_role_menu` VALUES (2, 3103);
INSERT INTO `sys_role_menu` VALUES (2, 3104);
INSERT INTO `sys_role_menu` VALUES (2, 3116);
INSERT INTO `sys_role_menu` VALUES (2, 3138);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime NULL DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `balance` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '账户余额（美元）',
  `frozen_balance` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '冻结余额（美元）',
  `used_balance` decimal(18, 10) NOT NULL DEFAULT 0.0000000000 COMMENT '累计应计金额（仅success/partial正常结算累计）',
  `request_count` int NOT NULL DEFAULT 0 COMMENT '请求次数',
  `billing_preference` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'subscription_first' COMMENT 'AI扣费偏好',
  `ai_concurrency_limit` int NOT NULL DEFAULT 100 COMMENT 'AI并发上限，0表示禁用AI中继',
  `active_request_count` int NOT NULL DEFAULT 0 COMMENT 'AI在途并发计数（等于reserved状态账单数）',
  `billing_multiplier` decimal(10, 2) NOT NULL DEFAULT 1.00 COMMENT '用户计费倍率',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 109 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 103, 'admin', 'CHAI', '00', '15902193@qq.com', '13263221890', '0', '/profile/upload/2026/09/04/logo_20260904001905A001.png', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-09-09 09:33:04', '2026-06-01 01:27:16', 'admin', '2026-06-01 01:27:16', '', '2026-09-06 20:46:40', '管理员', 97.8737201340, 0.0000000000, 9.9663074665, 880, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (2, 105, 'ry', '若依', '00', 'ry@qq.com', '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '2', '127.0.0.1', '2026-06-01 01:27:16', '2026-06-01 01:27:16', 'admin', '2026-06-01 01:27:16', 'admin', '2026-06-06 23:18:47', '测试员', 0.0000000000, 0.0000000000, 0.0000000000, 0, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (100, 103, 'a15902193', 'a15902193', '00', 'limengxiao230@163.com', '', '0', '', '$2a$10$44sWVBUx4iaYvZoS8u3OFe8UVGcUFgLh4l4lKKqdP0g1hb8fJihFK', '0', '2', '127.0.0.1', '2026-06-23 13:40:40', '2026-06-01 11:27:32', '', '2026-06-01 11:27:31', 'admin', '2026-07-12 21:26:05', NULL, 101.9994160000, 0.0000000000, 0.0006940000, 4, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (101, 103, 'b15902193', 'DD', '00', '15902192@qq.com', '13263221891', '0', '', '$2a$10$zkd40AUu0KoEumzTpW30IOiVrvFP8IN49dPN/AQVRHddOCrl9xGcK', '0', '2', '127.0.0.1', '2026-06-22 12:21:36', '2026-06-10 13:01:29', 'admin', '2026-06-10 13:00:02', 'admin', '2026-06-22 12:47:28', NULL, 0.0000000000, 0.0000000000, 0.0001050000, 1, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (102, 103, 'c15902193', 'c15902193', '00', '', '', '0', '', '$2a$10$rm8YautM5u6esCoLU0owqurwzEeeGSRQnGVuLDRyiLbV6N3pVkHy.', '0', '2', '127.0.0.1', '2026-06-17 00:06:56', '2026-06-17 00:07:32', 'admin', '2026-06-17 00:05:50', '', '2026-06-17 00:07:32', NULL, 0.0000000000, 0.0000000000, 0.0000000000, 0, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (103, NULL, 'a15902193', 'a15902193', '00', 'limengxiao230@163.com', '', '0', 'http://localhost:8080/profile/upload/2026/06/30/aigate-image-1782145513922-1_20260630004918A002.png', '$2a$10$.THDzx8YwObM3TDQau2dpOXEbU94ymWDr2y7SXomfh4cnVe5VQj7S', '0', '2', '127.0.0.1', '2026-07-01 00:25:39', '2026-06-27 14:21:37', '', '2026-06-27 14:21:36', '', '2026-06-30 00:49:18', NULL, 0.0000000000, 0.0000000000, 0.0000000000, 0, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (104, NULL, 'a15902193', 'a15902193', '00', '13263221890@163.com', '', '0', '', '$2a$10$7bz/vlsa0DYyd9g6DHbOLekqLL3J69Gu018.kMPG/owR2w2VIFRk2', '0', '2', '127.0.0.1', '2026-07-05 14:53:36', '2026-07-02 10:45:21', '', '2026-07-02 10:45:20', '', '2026-08-05 15:09:21', NULL, 0.5975610000, 0.0000000000, 0.0000780000, 2, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (105, NULL, 'b15902193', 'b15902193', '00', '13263221890@163.com', '', '0', '', '$2a$10$ivF1o5/QJB326MS/Mcunvux0JqzMN4kDG/iewrGvUx6rSYHmsbkvS', '0', '2', '127.0.0.1', '2026-07-05 17:35:09', '2026-07-05 17:32:08', '', '2026-07-05 17:32:08', '', '2026-07-05 17:35:13', NULL, 100.5000000000, 0.0000000000, 0.0000000000, 0, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (106, NULL, 'a15902193', 'a15902193', '00', '13263221890@163.com', '', '0', '', '$2a$10$CsxkC1jRSdGbPuN2M35hHuhEQu1aCJS6J2in4GQ4hoNpknVLkt41G', '0', '2', '127.0.0.1', '2026-07-20 10:41:20', '2026-07-18 12:24:25', '', '2026-07-05 19:00:26', '', '2026-08-20 15:45:41', NULL, 21.7191000000, 0.0000000000, 0.0000000000, 0, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (107, 100, 'test1', 'test1', '00', '13263221890@163.com', '13263221892', '1', '', '$2a$10$aPBy2UXTxjPqTRAGPEXxfuO.VE9pj6w8gnlZbULsIXNGcwZij98Rm', '0', '2', '127.0.0.1', '2026-08-02 10:48:40', '2026-07-20 10:52:17', 'admin', '2026-07-20 10:44:13', 'admin', '2026-09-06 15:12:57', NULL, 0.0100000000, 0.0000000000, 4.6132817200, 167, 'subscription_first', 100, 0, 1.00);
INSERT INTO `sys_user` VALUES (108, NULL, 'test1', 'test1', '00', '13263221890@163.com', '', '0', '', '$2a$10$f4G6WEmq.enthHpnRxrNveYRkfZuAaroE4VjRzKgOgoTNwFKTY2Y2', '0', '0', '127.0.0.1', '2026-09-04 11:41:18', '2026-08-02 16:35:12', '', '2026-08-02 16:35:11', '', '2026-09-09 07:27:19', NULL, 100.6000000000, 0.0000000000, 0.0000000000, 0, 'subscription_first', 100, 0, 1.00);

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户与岗位关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES (1, 1);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (108, 2);

SET FOREIGN_KEY_CHECKS = 1;
