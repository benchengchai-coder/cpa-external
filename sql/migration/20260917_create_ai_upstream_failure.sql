-- ============================================================================
-- 新增 CLIProxyAPI 上游失败事件表 ai_upstream_failure 及其管理菜单
-- 背景：订阅 CLIProxyAPI errors 实时事件通道（上游凭证尝试失败时发布），
--       落库到独立表供渠道健康排查；该通道仅实时推送、无积压补收，
--       事件无 request_id，无法与用户请求关联，表为追加写入、按保留期清理。
-- 执行说明：
--   1. 幂等脚本：建表用 CREATE TABLE IF NOT EXISTS，菜单先按 menu_id/
--      perms 清理再插入，可重复执行。
--   2. 菜单结构：C 型菜单 3162「上游失败事件」挂在 AI网关目录（3000）下，
--       组件 aigate/upstream-failure/index，自身携带 aigate:upstreamFailure:list；
--       F 型按钮 3159（query）/3160（remove）挂在该菜单下。
--       超级管理员登录即可见；普通角色需在角色管理中勾选。
--   3. 初始化库（sql/cpa-plugin.sql / docker/mysql/init/aigate.sql）已包含
--      相同结构与菜单，新部署无需执行本脚本。
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_upstream_failure`  (
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

-- 按 menu_id 或权限字符任一命中即清理，同时兼容执行过早期版本（按钮曾挂在 3010 下）的库
DELETE FROM `sys_menu`
WHERE `menu_id` IN (3159, 3160, 3161, 3162)
   OR `perms` IN ('aigate:upstreamFailure:list', 'aigate:upstreamFailure:query', 'aigate:upstreamFailure:remove');

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(3162, '上游失败事件', 3000, 12, 'upstream-failure', 'aigate/upstream-failure/index', '', '', 1, 0, 'C', '0', '0',
 'aigate:upstreamFailure:list', 'warning', 'admin', '2026-09-17 00:00:00', '', NULL, 'CLIProxyAPI上游失败事件监控'),
(3159, '上游失败事件详情', 3162, 1, '', '', '', '', 1, 0, 'F', '0', '0',
 'aigate:upstreamFailure:query', '#', 'admin', '2026-09-17 00:00:00', '', NULL, ''),
(3160, '上游失败事件删除', 3162, 2, '', '', '', '', 1, 0, 'F', '0', '0',
 'aigate:upstreamFailure:remove', '#', 'admin', '2026-09-17 00:00:00', '', NULL, '');
