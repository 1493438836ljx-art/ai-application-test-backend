-- Skill Library Tables
-- Stores skill definitions with input/output parameters and access control

-- 1. Skill main table
CREATE TABLE IF NOT EXISTS skill (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key, UUID',
    name VARCHAR(100) NOT NULL COMMENT 'Skill name, globally unique',
    description VARCHAR(2000) DEFAULT NULL COMMENT 'Skill description',
    suite_path VARCHAR(500) DEFAULT NULL COMMENT 'Executable suite file path',
    execution_type VARCHAR(20) NOT NULL COMMENT 'Execution type: AUTOMATED/AI',
    category VARCHAR(20) NOT NULL COMMENT 'Category: SYSTEM/USER',
    access_type VARCHAR(20) NOT NULL COMMENT 'Access control: PUBLIC/PRIVATE/WHITELIST/PROJECT',
    is_container TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Is container: 0=false, 1=true',
    status VARCHAR(20) NOT NULL COMMENT 'Status: PUBLISHED/DRAFT',
    created_by VARCHAR(100) NOT NULL COMMENT 'Creator',
    updated_by VARCHAR(100) NOT NULL COMMENT 'Updater',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical deletion flag: 0=false, 1=true',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_name (name),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill main table';

-- 2. Skill input parameter table
CREATE TABLE IF NOT EXISTS skill_input_parameter (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key, UUID',
    skill_id VARCHAR(36) NOT NULL COMMENT 'Associated Skill ID',
    param_order INT NOT NULL COMMENT 'Parameter order (starting from 1)',
    variadic TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Is variadic: 0=false, 1=true',
    param_type VARCHAR(50) DEFAULT NULL COMMENT 'Parameter type',
    param_name VARCHAR(100) DEFAULT NULL COMMENT 'Parameter name',
    default_value VARCHAR(1000) DEFAULT NULL COMMENT 'Default value',
    description VARCHAR(500) DEFAULT NULL COMMENT 'Parameter description',
    required TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Is required: 0=false, 1=true',
    PRIMARY KEY (id),
    INDEX idx_skill_id (skill_id),
    UNIQUE INDEX idx_skill_order (skill_id, param_order),
    CONSTRAINT fk_input_param_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill input parameter table';

-- 3. Skill output parameter table
CREATE TABLE IF NOT EXISTS skill_output_parameter (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key, UUID',
    skill_id VARCHAR(36) NOT NULL COMMENT 'Associated Skill ID',
    param_order INT NOT NULL COMMENT 'Parameter order (starting from 1)',
    variadic TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Is variadic: 0=false, 1=true',
    param_type VARCHAR(50) DEFAULT NULL COMMENT 'Parameter type',
    param_name VARCHAR(100) DEFAULT NULL COMMENT 'Parameter name',
    description VARCHAR(500) DEFAULT NULL COMMENT 'Parameter description',
    required TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Is required: 0=false, 1=true',
    PRIMARY KEY (id),
    INDEX idx_skill_id (skill_id),
    UNIQUE INDEX idx_skill_order (skill_id, param_order),
    CONSTRAINT fk_output_param_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill output parameter table';

-- 4. Skill access control table
CREATE TABLE IF NOT EXISTS skill_access_control (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key, UUID',
    skill_id VARCHAR(36) NOT NULL COMMENT 'Associated Skill ID',
    target_type VARCHAR(20) NOT NULL COMMENT 'Target type: USER/PROJECT',
    target_id VARCHAR(36) NOT NULL COMMENT 'Target ID',
    PRIMARY KEY (id),
    INDEX idx_skill_id (skill_id),
    INDEX idx_target (target_type, target_id),
    UNIQUE INDEX idx_skill_target (skill_id, target_type, target_id),
    CONSTRAINT fk_access_control_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Skill access control table';
