-- ============================================================================
-- 恢复“平台管理”菜单（sys_menu 3003 及其按钮权限 3058~3061）
-- 背景：bd74753 提交移除 AI 网关中继代码时误删了 AiPlatform 后端代码；
--       本脚本配套后端恢复，保证运行库中的菜单记录完整。
-- 执行说明：
--   1. 幂等脚本：先按 menu_id 清理再插入，可重复执行。
--   2. 超级管理员登录后即可看到菜单；普通角色需在角色管理中勾选。
--   3. 初始化库（sql/cpa-plugin.sql / docker/mysql/init/aigate.sql）已包含
--      相同记录，无需重复执行本脚本。
-- ============================================================================

SET NAMES utf8mb4;

DELETE FROM `sys_menu` WHERE `menu_id` IN (3003, 3058, 3059, 3060, 3061);

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
(3003, '平台管理', 3000, 7, 'platform', 'aigate/platform/index', '', '', 1, 0, 'C', '0', '0',
 'aigate:platform:list', 'link', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', 'AI平台管理菜单'),
(3058, '平台查询', 3003, 1, '', '', '', '', 1, 0, 'F', '0', '0',
 'aigate:platform:query', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', ''),
(3059, '平台新增', 3003, 2, '', '', '', '', 1, 0, 'F', '0', '0',
 'aigate:platform:add', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', ''),
(3060, '平台修改', 3003, 3, '', '', '', '', 1, 0, 'F', '0', '0',
 'aigate:platform:edit', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', ''),
(3061, '平台删除', 3003, 4, '', '', '', '', 1, 0, 'F', '0', '0',
 'aigate:platform:remove', '#', 'admin', '2026-06-01 21:14:16', 'admin', '2026-06-27 20:24:43', '');
