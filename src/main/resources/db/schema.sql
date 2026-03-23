-- 工作流变量类型表
CREATE TABLE IF NOT EXISTS workflow_variable_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    code VARCHAR(100) NOT NULL COMMENT '变量类型编码',
    name VARCHAR(100) NOT NULL COMMENT '变量类型名称',
    category VARCHAR(50) NOT NULL COMMENT '分类 (BASIC/COMPOSITE)',
    element_type VARCHAR(50) COMMENT '元素类型（用于数组类型）',
    file_type VARCHAR(50) COMMENT '文件类型（用于文件类型）',
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
