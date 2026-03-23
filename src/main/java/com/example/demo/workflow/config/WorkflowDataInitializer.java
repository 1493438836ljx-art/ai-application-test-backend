package com.example.demo.workflow.config;

import com.example.demo.workflow.entity.WorkflowNodeTypeEntity;
import com.example.demo.workflow.mapper.WorkflowNodeTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 工作流节点类型数据初始化器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowDataInitializer implements CommandLineRunner {

    private final WorkflowNodeTypeMapper nodeTypeMapper;

    @Override
    public void run(String... args) {
        log.info("初始化工作流节点类型数据...");
        initNodeTypes();
    }

    private void initNodeTypes() {
        // 基础节点
        createNodeTypeIfNotExists("start", "开始节点", "BASIC", "工作流入口", "VideoPlay", "#10b981", 1);
        createNodeTypeIfNotExists("end", "结束节点", "BASIC", "工作流出口", "CircleCheck", "#ef4444", 2);
        createNodeTypeIfNotExists("loopBodyCanvas", "循环体容器", "BASIC", "循环体容器节点", "Grid", "#3b82f6", 3);

        // 逻辑控制
        createNodeTypeIfNotExists("condition", "条件判断", "LOGIC", "条件判断节点", "Share", "#f59e0b", 10);
        createNodeTypeIfNotExists("loop", "循环控制", "LOGIC", "循环控制节点", "Refresh", "#8b5cf6", 11);

        // 数据准备
        createNodeTypeIfNotExists("envConnect", "环境对接", "DATA_PREPARE", "环境对接节点", "Connection", "#3b82f6", 20);
        createNodeTypeIfNotExists("tableExtract", "表格提取", "DATA_PREPARE", "表格提取节点", "Document", "#6366f1", 21);

        // 文本处理
        createNodeTypeIfNotExists("textClean", "文本清洗", "TEXT", "文本清洗", "Scissors", "#14b8a6", 30);
        createNodeTypeIfNotExists("textDedupe", "文本去重", "TEXT", "文本去重", "CopyDocument", "#64748b", 31);
        createNodeTypeIfNotExists("textGeneralize", "文本泛化", "TEXT", "文本泛化", "EditPen", "#8b5cf6", 32);
        createNodeTypeIfNotExists("textGenerate", "文本生成", "TEXT", "文本生成", "Edit", "#06b6d4", 33);

        // 图像处理
        createNodeTypeIfNotExists("imageGenerate", "图像生成", "IMAGE", "图像生成", "Picture", "#ec4899", 40);
        createNodeTypeIfNotExists("imageCutout", "抠图", "IMAGE", "抠图", "Crop", "#f43f5e", 41);
        createNodeTypeIfNotExists("imageEnhance", "画质提升", "IMAGE", "画质提升", "MagicStick", "#a855f7", 42);

        // 音视频处理
        createNodeTypeIfNotExists("videoExtractAudio", "视频提取音频", "AUDIO_VIDEO", "视频提取音频", "Headset", "#0ea5e9", 50);
        createNodeTypeIfNotExists("audioToText", "音频转文本", "AUDIO_VIDEO", "音频转文本", "Microphone", "#14b8a6", 51);
        createNodeTypeIfNotExists("videoFrame", "视频抽帧", "AUDIO_VIDEO", "视频抽帧", "VideoCamera", "#f97316", 52);

        // 测试设计
        createNodeTypeIfNotExists("testPlan", "测试方案生成", "TEST_DESIGN", "测试方案生成", "List", "#3b82f6", 60);

        // 测试执行
        createNodeTypeIfNotExists("apiAuto", "HTTP(S)接口调用", "TEST_EXEC", "HTTP(S)接口调用", "Connection", "#3b82f6", 70);
        createNodeTypeIfNotExists("aiAuto", "AI自动化执行", "TEST_EXEC", "AI自动化执行", "Cpu", "#f97316", 71);

        // 结果评估
        createNodeTypeIfNotExists("judgeModel", "裁判模型评估", "EVALUATE", "裁判模型评估", "DataAnalysis", "#ec4899", 80);
        createNodeTypeIfNotExists("firstTokenLatency", "首Token响应时延", "EVALUATE", "首Token响应时延", "Timer", "#f59e0b", 81);
        createNodeTypeIfNotExists("tokenOutputTime", "每Token输出耗时", "EVALUATE", "每Token输出耗时", "Stopwatch", "#84cc16", 82);
        createNodeTypeIfNotExists("e2eLatency", "端到端时延", "EVALUATE", "端到端时延", "Clock", "#06b6d4", 83);

        // 报告生成
        createNodeTypeIfNotExists("reportGenerate", "生成测试报告", "REPORT", "生成测试报告", "Document", "#3b82f6", 90);
        createNodeTypeIfNotExists("reportAnalysis", "报告分析", "REPORT", "报告分析", "Search", "#8b5cf6", 91);

        log.info("节点类型数据初始化完成");
    }

    private void createNodeTypeIfNotExists(String code, String name, String category, String description, String icon, String color, int sortOrder) {
        var existing = nodeTypeMapper.selectByCode(code);
        if (existing.isPresent()) {
            // 更新现有记录的 icon 和 color 字段
            WorkflowNodeTypeEntity nodeType = existing.get();
            if (nodeType.getIcon() == null || nodeType.getColor() == null) {
                nodeType.setIcon(icon);
                nodeType.setColor(color);
                nodeTypeMapper.updateById(nodeType);
                log.debug("更新节点类型图标和颜色: {}", code);
            }
        } else {
            WorkflowNodeTypeEntity nodeType = new WorkflowNodeTypeEntity();
            nodeType.setCode(code);
            nodeType.setName(name);
            nodeType.setCategory(category);
            nodeType.setDescription(description);
            nodeType.setIcon(icon);
            nodeType.setColor(color);
            nodeType.setSortOrder(sortOrder);
            nodeType.setEnabled(true);
            nodeTypeMapper.insert(nodeType);
            log.debug("创建节点类型: {}", code);
        }
    }
}
