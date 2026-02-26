package com.example.demo.result.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.common.enums.TestTaskItemStatus;
import com.example.demo.common.enums.TestTaskStatus;
import com.example.demo.result.dto.TestReportResponse;
import com.example.demo.result.dto.TestResultSummary;
import com.example.demo.result.entity.TestReport;
import com.example.demo.result.mapper.TestReportMapper;
import com.example.demo.result.repository.TestReportRepository;
import com.example.demo.task.entity.TestTask;
import com.example.demo.task.entity.TestTaskItem;
import com.example.demo.task.repository.TestTaskItemRepository;
import com.example.demo.task.repository.TestTaskRepository;
import com.example.demo.testset.entity.TestSet;
import com.example.demo.testset.repository.TestSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 测试报告服务类
 *
 * 提供测试报告的生成、查询、删除等功能，以及测试结果摘要的生成
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestReportService {

    /** 测试报告数据访问层 */
    private final TestReportRepository testReportRepository;

    /** 测试任务数据访问层 */
    private final TestTaskRepository testTaskRepository;

    /** 测试任务项数据访问层 */
    private final TestTaskItemRepository testTaskItemRepository;

    /** 测评集数据访问层 */
    private final TestSetRepository testSetRepository;

    /** 测试报告对象转换器 */
    private final TestReportMapper testReportMapper;

    /** JSON对象映射器 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 为已完成的任务生成测试报告
     *
     * @param taskId 任务ID
     * @return 测试报告响应
     * @throws BusinessException 如果任务未完成或不存在
     */
    @Transactional
    public TestReportResponse generateReport(Long taskId) {
        TestTask task = findTaskById(taskId);

        if (task.getStatus() != TestTaskStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "只能为已完成的任务生成报告");
        }

        Optional<TestReport> existingReport = testReportRepository.findByTaskId(taskId);
        if (existingReport.isPresent()) {
            return testReportMapper.toResponse(existingReport.get());
        }

        TestReport report = createReport(task);
        TestReport saved = testReportRepository.save(report);

        return testReportMapper.toResponse(saved);
    }

    /**
     * 获取或创建任务的测试报告
     *
     * 如果报告已存在则直接返回，否则为已完成的任务创建新报告
     *
     * @param taskId 任务ID
     * @return 测试报告响应
     * @throws BusinessException 如果任务未完成或不存在
     */
    @Transactional
    public TestReportResponse getOrCreateReport(Long taskId) {
        TestTask task = findTaskById(taskId);

        if (task.getStatus() != TestTaskStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "任务尚未完成");
        }

        Optional<TestReport> existingReport = testReportRepository.findByTaskId(taskId);
        if (existingReport.isPresent()) {
            return testReportMapper.toResponse(existingReport.get());
        }

        TestReport report = createReport(task);
        TestReport saved = testReportRepository.save(report);

        return testReportMapper.toResponse(saved);
    }

    /**
     * 分页查询测试报告列表
     *
     * @param taskName 任务名称（可选，用于模糊搜索）
     * @param pageable 分页参数
     * @return 测试报告分页结果
     */
    @Transactional(readOnly = true)
    public Page<TestReportResponse> getReports(String taskName, Pageable pageable) {
        Page<TestReport> reports;
        if (StringUtils.isNotBlank(taskName)) {
            reports = testReportRepository.findByTaskNameContainingIgnoreCase(taskName, pageable);
        } else {
            reports = testReportRepository.findAll(pageable);
        }
        return reports.map(testReportMapper::toResponse);
    }

    /**
     * 根据报告ID获取报告详情
     *
     * @param id 报告ID
     * @return 测试报告响应
     * @throws BusinessException 如果报告不存在
     */
    @Transactional(readOnly = true)
    public TestReportResponse getReportById(Long id) {
        TestReport report = findReportById(id);
        return testReportMapper.toResponse(report);
    }

    /**
     * 根据任务ID获取测试报告
     *
     * @param taskId 任务ID
     * @return 测试报告响应
     * @throws BusinessException 如果该任务没有报告
     */
    @Transactional(readOnly = true)
    public TestReportResponse getReportByTaskId(Long taskId) {
        TestReport report = testReportRepository.findByTaskId(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND, "该任务暂无报告"));
        return testReportMapper.toResponse(report);
    }

    /**
     * 获取任务的测试结果摘要
     *
     * 包含任务统计信息和每个测试项的详细执行结果
     *
     * @param taskId 任务ID
     * @return 测试结果摘要
     * @throws BusinessException 如果任务不存在
     */
    @Transactional(readOnly = true)
    public TestResultSummary getResultSummary(Long taskId) {
        TestTask task = findTaskById(taskId);
        List<TestTaskItem> items = testTaskItemRepository.findByTestTaskIdOrderBySequence(taskId);

        double successRate = 0.0;
        if (task.getTotalItems() != null && task.getTotalItems() > 0 && task.getSuccessItems() != null) {
            successRate = (task.getSuccessItems() * 100.0) / task.getTotalItems();
        }

        double averageScore = items.stream()
                .filter(item -> item.getScore() != null)
                .mapToDouble(TestTaskItem::getScore)
                .average()
                .orElse(0.0);

        long totalExecutionTime = items.stream()
                .filter(item -> item.getExecutionTimeMs() != null)
                .mapToLong(TestTaskItem::getExecutionTimeMs)
                .sum();

        List<TestResultSummary.ItemResult> itemResults = new ArrayList<>();
        for (TestTaskItem item : items) {
            itemResults.add(TestResultSummary.ItemResult.builder()
                    .itemId(item.getId())
                    .sequence(item.getSequence())
                    .status(item.getStatus().getCode())
                    .input(item.getInput())
                    .expectedOutput(item.getExpectedOutput())
                    .actualOutput(item.getActualOutput())
                    .score(item.getScore())
                    .reason(item.getReason())
                    .executionTimeMs(item.getExecutionTimeMs())
                    .build());
        }

        return TestResultSummary.builder()
                .taskId(task.getId())
                .taskName(task.getName())
                .totalItems(task.getTotalItems())
                .successItems(task.getSuccessItems())
                .failedItems(task.getFailedItems())
                .successRate(successRate)
                .averageScore(averageScore)
                .totalExecutionTimeMs(totalExecutionTime)
                .itemResults(itemResults)
                .build();
    }

    /**
     * 删除指定的测试报告
     *
     * @param id 报告ID
     * @throws BusinessException 如果报告不存在
     */
    @Transactional
    public void deleteReport(Long id) {
        if (!testReportRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        testReportRepository.deleteById(id);
    }

    /**
     * 创建测试报告实体
     *
     * 根据任务信息和执行结果生成报告数据
     *
     * @param task 测试任务
     * @return 测试报告实体
     */
    private TestReport createReport(TestTask task) {
        List<TestTaskItem> items = testTaskItemRepository.findByTestTaskIdOrderBySequence(task.getId());

        double successRate = 0.0;
        if (task.getTotalItems() != null && task.getTotalItems() > 0 && task.getSuccessItems() != null) {
            successRate = (task.getSuccessItems() * 100.0) / task.getTotalItems();
        }

        double averageScore = items.stream()
                .filter(item -> item.getScore() != null)
                .mapToDouble(TestTaskItem::getScore)
                .average()
                .orElse(0.0);

        long totalExecutionTime = items.stream()
                .filter(item -> item.getExecutionTimeMs() != null)
                .mapToLong(TestTaskItem::getExecutionTimeMs)
                .sum();

        String testSetName = null;
        if (task.getTestSetId() != null) {
            testSetName = testSetRepository.findById(task.getTestSetId())
                    .map(TestSet::getName)
                    .orElse(null);
        }

        String summary = generateSummary(task, successRate, averageScore);

        return TestReport.builder()
                .taskId(task.getId())
                .taskName(task.getName())
                .testSetId(task.getTestSetId())
                .testSetName(testSetName)
                .totalItems(task.getTotalItems())
                .successItems(task.getSuccessItems())
                .failedItems(task.getFailedItems())
                .successRate(successRate)
                .averageScore(averageScore)
                .totalExecutionTimeMs(totalExecutionTime)
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .summary(summary)
                .build();
    }

    /**
     * 生成测试报告摘要文本
     *
     * @param task 测试任务
     * @param successRate 成功率
     * @param averageScore 平均评分
     * @return 摘要文本
     */
    private String generateSummary(TestTask task, double successRate, double averageScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("测试任务: ").append(task.getName()).append("\n");
        sb.append("总用例数: ").append(task.getTotalItems()).append("\n");
        sb.append("成功: ").append(task.getSuccessItems()).append(", 失败: ").append(task.getFailedItems()).append("\n");
        sb.append("成功率: ").append(String.format("%.2f%%", successRate)).append("\n");
        sb.append("平均评分: ").append(String.format("%.2f", averageScore)).append("\n");

        if (task.getStartedAt() != null && task.getCompletedAt() != null) {
            long durationMs = java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis();
            sb.append("总耗时: ").append(formatDuration(durationMs));
        }

        return sb.toString();
    }

    /**
     * 格式化持续时间
     *
     * 将毫秒数转换为易读的时间格式
     *
     * @param ms 毫秒数
     * @return 格式化后的时间字符串
     */
    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "分" + remainingSeconds + "秒";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "小时" + remainingMinutes + "分" + remainingSeconds + "秒";
    }

    /**
     * 根据ID查找测试任务
     *
     * @param id 任务ID
     * @return 测试任务
     * @throws BusinessException 如果任务不存在
     */
    private TestTask findTaskById(Long id) {
        return testTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    /**
     * 根据ID查找测试报告
     *
     * @param id 报告ID
     * @return 测试报告
     * @throws BusinessException 如果报告不存在
     */
    private TestReport findReportById(Long id) {
        return testReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
    }
}
