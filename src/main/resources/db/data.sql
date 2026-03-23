-- 工作流节点类型数据更新
-- 更新现有节点类型的 icon 和 color 字段

-- 基础节点
UPDATE workflow_node_type SET icon = 'VideoPlay', color = '#10b981' WHERE code = 'start';
UPDATE workflow_node_type SET icon = 'CircleCheck', color = '#ef4444' WHERE code = 'end';
UPDATE workflow_node_type SET icon = 'Grid', color = '#3b82f6' WHERE code = 'loopBodyCanvas';

-- 逻辑控制
UPDATE workflow_node_type SET icon = 'Share', color = '#f59e0b' WHERE code = 'condition';
UPDATE workflow_node_type SET icon = 'Refresh', color = '#8b5cf6' WHERE code = 'loop';

-- 数据准备
UPDATE workflow_node_type SET icon = 'Connection', color = '#3b82f6' WHERE code = 'envConnect';
UPDATE workflow_node_type SET icon = 'Document', color = '#6366f1' WHERE code = 'tableExtract';

-- 文本处理
UPDATE workflow_node_type SET icon = 'Scissors', color = '#14b8a6' WHERE code = 'textClean';
UPDATE workflow_node_type SET icon = 'CopyDocument', color = '#64748b' WHERE code = 'textDedupe';
UPDATE workflow_node_type SET icon = 'EditPen', color = '#8b5cf6' WHERE code = 'textGeneralize';
UPDATE workflow_node_type SET icon = 'Edit', color = '#06b6d4' WHERE code = 'textGenerate';

-- 图像处理
UPDATE workflow_node_type SET icon = 'Picture', color = '#ec4899' WHERE code = 'imageGenerate';
UPDATE workflow_node_type SET icon = 'Crop', color = '#f43f5e' WHERE code = 'imageCutout';
UPDATE workflow_node_type SET icon = 'MagicStick', color = '#a855f7' WHERE code = 'imageEnhance';

-- 音视频处理
UPDATE workflow_node_type SET icon = 'Headset', color = '#0ea5e9' WHERE code = 'videoExtractAudio';
UPDATE workflow_node_type SET icon = 'Microphone', color = '#14b8a6' WHERE code = 'audioToText';
UPDATE workflow_node_type SET icon = 'VideoCamera', color = '#f97316' WHERE code = 'videoFrame';

-- 测试设计
UPDATE workflow_node_type SET icon = 'List', color = '#3b82f6' WHERE code = 'testPlan';

-- 测试执行
UPDATE workflow_node_type SET icon = 'Connection', color = '#3b82f6' WHERE code = 'apiAuto';
UPDATE workflow_node_type SET icon = 'Cpu', color = '#f97316' WHERE code = 'aiAuto';

-- 结果评估
UPDATE workflow_node_type SET icon = 'DataAnalysis', color = '#ec4899' WHERE code = 'judgeModel';
UPDATE workflow_node_type SET icon = 'Timer', color = '#f59e0b' WHERE code = 'firstTokenLatency';
UPDATE workflow_node_type SET icon = 'Stopwatch', color = '#84cc16' WHERE code = 'tokenOutputTime';
UPDATE workflow_node_type SET icon = 'Clock', color = '#06b6d4' WHERE code = 'e2eLatency';

-- 报告生成
UPDATE workflow_node_type SET icon = 'Document', color = '#3b82f6' WHERE code = 'reportGenerate';
UPDATE workflow_node_type SET icon = 'Search', color = '#8b5cf6' WHERE code = 'reportAnalysis';
