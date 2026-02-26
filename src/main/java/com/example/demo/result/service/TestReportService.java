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

@Slf4j
@Service
@RequiredArgsConstructor
public class TestReportService {

    private final TestReportRepository testReportRepository;
    private final TestTaskRepository testTaskRepository;
    private final TestTaskItemRepository testTaskItemRepository;
    private final TestSetRepository testSetRepository;
    private final TestReportMapper testReportMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Transactional(readOnly = true)
    public TestReportResponse getReportById(Long id) {
        TestReport report = findReportById(id);
        return testReportMapper.toResponse(report);
    }

    @Transactional(readOnly = true)
    public TestReportResponse getReportByTaskId(Long taskId) {
        TestReport report = testReportRepository.findByTaskId(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND, "该任务暂无报告"));
        return testReportMapper.toResponse(report);
    }

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

    @Transactional
    public void deleteReport(Long id) {
        if (!testReportRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        testReportRepository.deleteById(id);
    }

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

    private TestTask findTaskById(Long id) {
        return testTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private TestReport findReportById(Long id) {
        return testReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
    }
}
