-- ============================================================================
-- 平台管理移除“图标”和“描述”字段（ai_platform 表）
-- 背景：平台管理页面精简字段，ai_platform 表不再保留 description/icon 列，
--       后端 domain、Mapper XML、前端类型与页面已同步移除。
-- 执行说明：
--   1. 一次性脚本：删除已存在的列，重复执行会因列不存在而报错，属预期。
--   2. 新初始化库（sql/cpa-plugin.sql / docker/mysql/init/aigate.sql）已不含
--      这两列，无需执行本脚本。
-- ============================================================================

SET NAMES utf8mb4;

ALTER TABLE `ai_platform` DROP COLUMN `description`, DROP COLUMN `icon`;
