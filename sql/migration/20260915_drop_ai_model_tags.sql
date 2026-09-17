-- ============================================================================
-- 移除 ai_model 表的 tags（标签）字段
-- 背景：模型管理页面已删除“标签”列，后端域对象与 Mapper 同步移除该字段，
--       本脚本配套清理运行库中的历史列。
-- 执行说明：
--   1. 幂等脚本：通过 information_schema 判断列存在时才执行 DROP，可重复执行。
--   2. 初始化库（sql/cpa-plugin.sql / docker/mysql/init/aigate.sql）已同步
--      移除该列，新部署无需执行本脚本。
-- ============================================================================

SET NAMES utf8mb4;

SET @drop_tags_sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_model'
          AND COLUMN_NAME = 'tags'
    ),
    'ALTER TABLE `ai_model` DROP COLUMN `tags`',
    'SELECT 1'
);

PREPARE stmt FROM @drop_tags_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
