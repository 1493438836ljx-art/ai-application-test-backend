-- 添加工作流发布相关字段
-- 请在数据库管理工具中执行以下SQL

-- 添加触发类型字段
ALTER TABLE workflow ADD COLUMN trigger_type VARCHAR(20) DEFAULT NULL COMMENT '触发类型：MANUAL/SCHEDULE/API';

-- 添加触发配置字段
ALTER TABLE workflow ADD COLUMN trigger_config TEXT DEFAULT NULL COMMENT '触发配置（JSON格式）';

-- 添加发布时间字段
ALTER TABLE workflow ADD COLUMN published_at DATETIME DEFAULT NULL COMMENT '发布时间';

-- 添加发布人字段
ALTER TABLE workflow ADD COLUMN published_by VARCHAR(100) DEFAULT NULL COMMENT '发布人';

-- 检查表结构
-- DESC workflow;
