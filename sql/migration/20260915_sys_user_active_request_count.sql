-- ============================================================================
-- sys_user 新增 AI 并发计数列 active_request_count
-- 背景：启用用户级 AI 并发限制（enforce ai_concurrency_limit）。并发计数与
--       ai_billing_record 的 reserved 状态同生命周期：预占事务内条件自增，
--       账单离开 reserved 的出口（主动释放 / 超时过期 / 结算领取 / 核销）减计数。
--       核心不变量：active_request_count == 该用户 status='reserved' 的账单数。
-- 执行说明：
--   1. 幂等脚本：通过 information_schema 判断列存在时才执行 ADD，可重复执行。
--   2. 兼容修复：若历史库（旧版 docker/mysql/init/aigate.sql 初始化）缺失
--      ai_concurrency_limit 列，本脚本会一并幂等补齐（默认 100）。
--   3. 回填：按 reserved 账单数重算 active_request_count，与不变量一致，
--      重复执行结果不变，部署瞬间在途请求不丢计数。
--   4. 初始化库（sql/cpa-plugin.sql / docker/mysql/init/aigate.sql）已同步
--      新增该列，新部署无需执行本脚本。
-- ============================================================================

SET NAMES utf8mb4;

-- 幂等补齐 ai_concurrency_limit（修复旧初始化脚本的列漂移）
SET @add_limit_sql := IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'ai_concurrency_limit'
    ),
    'ALTER TABLE `sys_user` ADD COLUMN `ai_concurrency_limit` int NOT NULL DEFAULT 100 COMMENT ''AI并发上限，0表示禁用AI中继'' AFTER `billing_preference`',
    'SELECT 1'
);

PREPARE stmt FROM @add_limit_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 幂等新增 active_request_count
SET @add_count_sql := IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'active_request_count'
    ),
    'ALTER TABLE `sys_user` ADD COLUMN `active_request_count` int NOT NULL DEFAULT 0 COMMENT ''AI在途并发计数（等于reserved状态账单数）'' AFTER `ai_concurrency_limit`',
    'SELECT 1'
);

PREPARE stmt FROM @add_count_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回填在途计数：按 reserved 账单数对齐（幂等；账单表不存在的旧库跳过）
SET @backfill_sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_billing_record'
    ),
    'UPDATE sys_user u SET u.active_request_count = (SELECT COUNT(*) FROM ai_billing_record b WHERE b.user_id = u.user_id AND b.status = ''reserved'')',
    'SELECT 1'
);

PREPARE stmt FROM @backfill_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
