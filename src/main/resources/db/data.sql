-- ============================================================
-- 工作流节点类型数据初始化
-- 采用"核心9种类型 + Skill动态加载"模式
-- ============================================================

-- 1. 禁用业务节点类型（保留数据但不显示）
UPDATE workflow_node_type SET enabled = false
WHERE code IN ('textClean', 'textDedupe', 'textGeneralize', 'textGenerate',
    'imageGenerate', 'imageCutout', 'imageEnhance',
    'videoExtractAudio', 'audioToText', 'videoFrame',
    'envConnect', 'tableExtract', 'testPlan', 'apiAuto', 'aiAuto',
    'judgeModel', 'firstTokenLatency', 'tokenOutputTime', 'e2eLatency',
    'reportGenerate', 'reportAnalysis', 'tableGenerate');

-- 2. 删除旧的 condition 节点（已改名为 condition_simple）
DELETE FROM workflow_node_type WHERE code = 'condition';

-- 3. 更新 loop 节点分类为 LOGIC
UPDATE workflow_node_type SET category = 'LOGIC', icon = 'Refresh' WHERE code = 'loop';

-- 4. 更新基础节点配置
UPDATE workflow_node_type SET icon = 'VideoPlay', category = 'BASIC' WHERE code = 'start';
UPDATE workflow_node_type SET icon = 'CircleCheck', category = 'BASIC' WHERE code = 'end';
UPDATE workflow_node_type SET icon = 'Grid', category = 'BASIC' WHERE code = 'loopBodyCanvas';

-- 5. 更新条件节点配置
UPDATE workflow_node_type SET icon = 'Share', category = 'LOGIC' WHERE code = 'condition_simple';

-- 6. 添加缺失的节点类型
INSERT INTO workflow_node_type (code, name, category, description, icon, sort_order, enabled, created_at, updated_at)
VALUES
('condition_multi', '多路分支', 'LOGIC', '多路条件分支，支持多个 case 和 default', 'Grid', 4, true, NOW(), NOW()),
('batch', '批处理', 'LOGIC', '批量并行处理，提高执行效率', 'DataLine', 5, true, NOW(), NOW()),
('async', '异步处理', 'LOGIC', '异步执行节点，不阻塞主流程', 'Connection', 6, true, NOW(), NOW()),
('collect', '结果收集', 'LOGIC', '收集多个分支的执行结果', 'FolderAdd', 7, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    description = VALUES(description),
    icon = VALUES(icon),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled),
    updated_at = NOW();

-- 7. 禁用虚假的 skill 节点类型（真实的 Skill 从 Skill 库动态加载）
UPDATE workflow_node_type SET enabled = false WHERE code = 'skill';
