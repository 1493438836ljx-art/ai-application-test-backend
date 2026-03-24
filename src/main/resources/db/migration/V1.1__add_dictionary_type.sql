-- 为已存在的 workflow_variable_type 表添加 dictionary_type 字段
-- 如果字段已存在，H2会忽略此语句（使用 IF NOT EXISTS）

ALTER TABLE workflow_variable_type ADD COLUMN IF NOT EXISTS dictionary_type VARCHAR(100) COMMENT '数据字典类型（用于Dictionary类型）';
