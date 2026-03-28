-- =====================================================
-- 合并 skill_input_parameter 和 skill_output_parameter 表
-- 执行前请备份数据！
-- =====================================================

-- 1. 创建新的统一参数表
CREATE TABLE IF NOT EXISTS skill_parameter (
    id VARCHAR(36) NOT NULL,
    skill_id VARCHAR(36) NOT NULL,
    param_direction VARCHAR(10) NOT NULL COMMENT '参数方向: INPUT/OUTPUT',
    param_order INT NOT NULL COMMENT '参数顺序',
    param_type VARCHAR(50) COMMENT '参数类型',
    param_name VARCHAR(100) COMMENT '参数名称',
    default_value VARCHAR(1000) COMMENT '默认值（仅入参）',
    description VARCHAR(500) COMMENT '参数描述',
    required TINYINT(1) DEFAULT 0 COMMENT '是否必填',
    PRIMARY KEY (id),
    INDEX idx_skill_id (skill_id),
    INDEX idx_skill_direction (skill_id, param_direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill参数表';

-- 2. 迁移入参数据
INSERT INTO skill_parameter (id, skill_id, param_direction, param_order, param_type, param_name, default_value, description, required)
SELECT
    id,
    skill_id,
    'INPUT' as param_direction,
    param_order,
    param_type,
    param_name,
    default_value,
    description,
    IFNULL(required, 0) as required
FROM skill_input_parameter;

-- 3. 迁移出参数据（注意：出参没有default_value字段）
INSERT INTO skill_parameter (id, skill_id, param_direction, param_order, param_type, param_name, default_value, description, required)
SELECT
    id,
    skill_id,
    'OUTPUT' as param_direction,
    param_order,
    param_type,
    param_name,
    NULL as default_value,
    description,
    IFNULL(required, 0) as required
FROM skill_output_parameter;

-- 4. 验证数据迁移（可选，用于检查）
-- SELECT 'input_count' as source, COUNT(*) as cnt FROM skill_input_parameter
-- UNION ALL
-- SELECT 'output_count', COUNT(*) FROM skill_output_parameter
-- UNION ALL
-- SELECT 'new_input_count', COUNT(*) FROM skill_parameter WHERE param_direction = 'INPUT'
-- UNION ALL
-- SELECT 'new_output_count', COUNT(*) FROM skill_parameter WHERE param_direction = 'OUTPUT';

-- 5. 删除旧表
DROP TABLE IF EXISTS skill_input_parameter;
DROP TABLE IF EXISTS skill_output_parameter;
