-- 数据字典主表
CREATE TABLE IF NOT EXISTS data_dictionary (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '数据字典名称',
    description VARCHAR(500) NULL COMMENT '字典描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    INDEX idx_name (name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';

-- 字段定义表
CREATE TABLE IF NOT EXISTS dictionary_column (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dictionary_id BIGINT NOT NULL COMMENT '关联的数据字典ID',
    column_key VARCHAR(50) NOT NULL COMMENT '字段Key',
    column_label VARCHAR(50) NOT NULL COMMENT '字段名称',
    column_type VARCHAR(20) NOT NULL COMMENT '字段类型',
    enum_options VARCHAR(1000) NULL COMMENT '枚举选项JSON数组',
    min_value DECIMAL(20,6) NULL COMMENT '最小值',
    max_value DECIMAL(20,6) NULL COMMENT '最大值',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_dictionary_id (dictionary_id),
    INDEX idx_sort_order (sort_order),
    CONSTRAINT fk_column_dictionary FOREIGN KEY (dictionary_id)
        REFERENCES data_dictionary (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段定义表';

-- 工作流变量类型表
CREATE TABLE IF NOT EXISTS workflow_variable_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    code VARCHAR(100) NOT NULL COMMENT '变量类型编码',
    name VARCHAR(100) NOT NULL COMMENT '变量类型名称',
    category VARCHAR(50) NOT NULL COMMENT '分类 (BASIC/COMPOSITE)',
    element_type VARCHAR(50) COMMENT '元素类型（用于数组类型）',
    file_type VARCHAR(50) COMMENT '文件类型（用于文件类型）',
    dictionary_type VARCHAR(100) COMMENT '数据字典类型（用于Dictionary类型）',
    description VARCHAR(500) COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流变量类型表';

-- 工作流节点类型表
CREATE TABLE IF NOT EXISTS workflow_node_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    code VARCHAR(100) NOT NULL COMMENT '节点类型编码',
    name VARCHAR(100) NOT NULL COMMENT '节点类型名称',
    category VARCHAR(50) NOT NULL COMMENT '分类',
    description VARCHAR(500) COMMENT '描述',
    icon VARCHAR(100) COMMENT '图标',
    color VARCHAR(20) COMMENT '颜色',
    default_config TEXT COMMENT '默认配置JSON',
    input_ports TEXT COMMENT '默认输入端口JSON',
    output_ports TEXT COMMENT '默认输出端口JSON',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点类型表';
