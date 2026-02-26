package com.example.demo.task.service;

import com.example.demo.common.enums.TestTaskItemStatus;
import com.example.demo.common.enums.TestTaskStatus;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.environment.entity.Environment;
import com.example.demo.environment.repository.EnvironmentRepository;
import com.example.demo.plugin.entity.Plugin;
import com.example.demo.plugin.loader.PluginLoader;
import com.example.demo.plugin.repository.PluginRepository;
import com.example.demo.plugin.spi.EvaluationContext;
import com.example.demo.plugin.spi.EvaluationPlugin;
import com.example.demo.plugin.spi.EvaluationResult;
import com.example.demo.plugin.spi.ExecutionContext;
import com.example.demo.plugin.spi.ExecutionPlugin;
import com.example.demo.plugin.spi.ExecutionResult;
import com.example.demo.task.dto.*;
import com.example.demo.task.entity.TestTask;
import com.example.demo.task.entity.TestTaskItem;
import com.example.demo.task.mapper.TestTaskMapper;
import com.example.demo.task.repository.TestTaskItemRepository;
import com.example.demo.task.repository.TestTaskRepository;
import com.example.demo.testset.entity.TestCase;
import com.example.demo.testset.repository.TestCaseRepository;
import com.example.demo.testset.repository.TestSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestTaskService {

    private final TestTaskRepository testTaskRepository;
    private final TestTaskItemRepository testTaskItemRepository;
    private final TestSetRepository testSetRepository;
    private final TestCaseRepository testCaseRepository;
    private final EnvironmentRepository environmentRepository;
    private final PluginRepository pluginRepository;
    private final TestTaskMapper testTaskMapper;
    private final PluginLoader pluginLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public TestTaskResponse createTask(TestTaskCreateRequest request) {
        validateTaskRequest(request);

        TestTask task = testTaskMapper.toEntity(request);
        task.setStatus(TestTaskStatus.PENDING);
        task.setCompletedItems(0);
        task.setSuccessItems(0);
        task.setFailedItems(0);

        List<TestCase> testCases = testCaseRepository.findByTestSetIdOrderBySequence(request.getTestSetId());
        task.setTotalItems(testCases.size());

        TestTask savedTask = testTaskRepository.save(task);

        for (TestCase testCase : testCases) {
            TestTaskItem item = TestTaskItem.builder()
                    .testTask(savedTask)
                    .testCaseId(testCase.getId())
                    .sequence(testCase.getSequence())
                    .input(testCase.getInput())
                    .expectedOutput(testCase.getExpectedOutput())
                    .status(TestTaskItemStatus.PENDING)
                    .build();
            testTaskItemRepository.save(item);
        }

        return enrichTaskResponse(testTaskMapper.toResponse(savedTask));
    }

    @Transactional(readOnly = true)
    public Page<TestTaskResponse> getTasks(String name, TestTaskStatus status, Pageable pageable) {
        Page<TestTask> tasks;

        if (StringUtils.isNotBlank(name) && status != null) {
            tasks = testTaskRepository.findByNameContainingIgnoreCase(name, pageable)
                    .filter(t -> t.getStatus() == status);
        } else if (StringUtils.isNotBlank(name)) {
            tasks = testTaskRepository.findByNameContainingIgnoreCase(name, pageable);
        } else if (status != null) {
            tasks = testTaskRepository.findByStatus(status, pageable);
        } else {
            tasks = testTaskRepository.findAll(pageable);
        }

        return tasks.map(task -> enrichTaskResponse(testTaskMapper.toResponse(task)));
    }

    @Transactional(readOnly = true)
    public TestTaskResponse getTaskById(Long id) {
        TestTask task = findTaskById(id);
        return enrichTaskResponse(testTaskMapper.toResponse(task));
    }

    @Transactional(readOnly = true)
    public TaskProgressResponse getTaskProgress(Long id) {
        TestTask task = findTaskById(id);

        double progress = 0.0;
        if (task.getTotalItems() != null && task.getTotalItems() > 0 && task.getCompletedItems() != null) {
            progress = (task.getCompletedItems() * 100.0) / task.getTotalItems();
        }

        return TaskProgressResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .totalItems(task.getTotalItems())
                .completedItems(task.getCompletedItems())
                .successItems(task.getSuccessItems())
                .failedItems(task.getFailedItems())
                .progress(progress)
                .errorMessage(task.getErrorMessage())
                .build();
    }

    @Transactional
    public TestTaskResponse updateTask(Long id, TestTaskUpdateRequest request) {
        TestTask task = findTaskById(id);

        if (task.getStatus() != TestTaskStatus.PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "只能修改待执行状态的任务");
        }

        testTaskMapper.updateEntity(request, task);
        TestTask updated = testTaskRepository.save(task);
        return enrichTaskResponse(testTaskMapper.toResponse(updated));
    }

    @Transactional
    public void deleteTask(Long id) {
        TestTask task = findTaskById(id);

        if (task.getStatus() == TestTaskStatus.RUNNING) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_RUNNING, "运行中的任务不能删除");
        }

        testTaskItemRepository.deleteByTestTaskId(id);
        testTaskRepository.delete(task);
    }

    @Transactional
    public TestTaskResponse startTask(Long id) {
        TestTask task = findTaskById(id);

        if (task.getStatus() == TestTaskStatus.RUNNING) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_RUNNING);
        }

        if (task.getStatus() == TestTaskStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "已完成的任务不能重新启动");
        }

        task.setStatus(TestTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        task.setCompletedAt(null);
        task.setErrorMessage(null);
        testTaskRepository.save(task);

        executeTaskAsync(id);

        return enrichTaskResponse(testTaskMapper.toResponse(task));
    }

    @Async("taskExecutor")
    public void executeTaskAsync(Long taskId) {
        log.info("Starting async execution for task: {}", taskId);

        try {
            TestTask task = testTaskRepository.findById(taskId).orElse(null);
            if (task == null) {
                log.error("Task not found: {}", taskId);
                return;
            }

            Environment environment = environmentRepository.findById(task.getEnvironmentId()).orElse(null);
            Plugin executionPluginEntity = pluginRepository.findById(task.getExecutionPluginId()).orElse(null);
            Plugin evaluationPluginEntity = pluginRepository.findById(task.getEvaluationPluginId()).orElse(null);

            if (environment == null || executionPluginEntity == null || evaluationPluginEntity == null) {
                updateTaskFailed(taskId, "环境或插件配置无效");
                return;
            }

            Optional<ExecutionPlugin> executionPlugin = pluginLoader.getExecutionPlugin(executionPluginEntity.getName());
            Optional<EvaluationPlugin> evaluationPlugin = pluginLoader.getEvaluationPlugin(evaluationPluginEntity.getName());

            if (executionPlugin.isEmpty() || evaluationPlugin.isEmpty()) {
                updateTaskFailed(taskId, "插件未找到或未注册");
                return;
            }

            List<TestTaskItem> items = testTaskItemRepository.findByTestTaskIdOrderBySequence(taskId);

            for (TestTaskItem item : items) {
                if (!isTaskRunning(taskId)) {
                    log.info("Task {} is no longer running, stopping execution", taskId);
                    break;
                }

                if (item.getStatus() == TestTaskItemStatus.SUCCESS || item.getStatus() == TestTaskItemStatus.FAILED) {
                    continue;
                }

                executeTaskItem(taskId, item, environment, executionPlugin.get(), evaluationPlugin.get(),
                        task.getExecutionConfig(), task.getEvaluationConfig());
            }

            if (isTaskRunning(taskId)) {
                completeTask(taskId);
            }

        } catch (Exception e) {
            log.error("Task execution failed", e);
            updateTaskFailed(taskId, "执行异常: " + e.getMessage());
        }
    }

    private void executeTaskItem(Long taskId, TestTaskItem item, Environment environment,
                                  ExecutionPlugin executionPlugin, EvaluationPlugin evaluationPlugin,
                                  String executionConfig, String evaluationConfig) {
        item.setStatus(TestTaskItemStatus.RUNNING);
        item.setStartedAt(LocalDateTime.now());
        testTaskItemRepository.save(item);

        try {
            Map<String, Object> authConfig = parseConfig(environment.getAuthConfig());
            Map<String, Object> execConfig = parseConfig(executionConfig);

            ExecutionContext execContext = ExecutionContext.builder()
                    .taskItemId(item.getId())
                    .input(item.getInput())
                    .apiEndpoint(environment.getApiEndpoint())
                    .authConfig(authConfig)
                    .pluginConfig(execConfig)
                    .build();

            ExecutionResult execResult = executionPlugin.execute(execContext);

            if (!execResult.isSuccess()) {
                item.setStatus(TestTaskItemStatus.FAILED);
                item.setActualOutput(execResult.getOutput());
                item.setErrorMessage(execResult.getErrorMessage());
                item.setExecutionTimeMs(execResult.getExecutionTimeMs());
                item.setCompletedAt(LocalDateTime.now());
                testTaskItemRepository.save(item);
                updateTaskProgress(taskId, false);
                return;
            }

            item.setActualOutput(execResult.getOutput());
            item.setExecutionTimeMs(execResult.getExecutionTimeMs());

            Map<String, Object> evalConfig = parseConfig(evaluationConfig);
            EvaluationContext evalContext = EvaluationContext.builder()
                    .taskItemId(item.getId())
                    .input(item.getInput())
                    .expectedOutput(item.getExpectedOutput())
                    .actualOutput(execResult.getOutput())
                    .pluginConfig(evalConfig)
                    .build();

            EvaluationResult evalResult = evaluationPlugin.evaluate(evalContext);

            item.setStatus(evalResult.isSuccess() ? TestTaskItemStatus.SUCCESS : TestTaskItemStatus.FAILED);
            item.setScore(evalResult.getScore());
            item.setReason(evalResult.getReason());
            if (!evalResult.isSuccess() && evalResult.getErrorMessage() != null) {
                item.setErrorMessage(evalResult.getErrorMessage());
            }
            item.setCompletedAt(LocalDateTime.now());
            testTaskItemRepository.save(item);

            updateTaskProgress(taskId, evalResult.isSuccess());

        } catch (Exception e) {
            log.error("Task item execution failed", e);
            item.setStatus(TestTaskItemStatus.FAILED);
            item.setErrorMessage("执行异常: " + e.getMessage());
            item.setCompletedAt(LocalDateTime.now());
            testTaskItemRepository.save(item);
            updateTaskProgress(taskId, false);
        }
    }

    @Transactional
    public void updateTaskProgress(Long taskId, boolean success) {
        TestTask task = testTaskRepository.findById(taskId).orElse(null);
        if (task == null) return;

        task.setCompletedItems(task.getCompletedItems() + 1);
        if (success) {
            task.setSuccessItems(task.getSuccessItems() + 1);
        } else {
            task.setFailedItems(task.getFailedItems() + 1);
        }
        testTaskRepository.save(task);
    }

    @Transactional
    public TestTaskResponse pauseTask(Long id) {
        TestTask task = findTaskById(id);

        if (task.getStatus() != TestTaskStatus.RUNNING) {
            throw new BusinessException(ErrorCode.TASK_NOT_RUNNING);
        }

        task.setStatus(TestTaskStatus.PAUSED);
        TestTask updated = testTaskRepository.save(task);
        return enrichTaskResponse(testTaskMapper.toResponse(updated));
    }

    @Transactional
    public TestTaskResponse resumeTask(Long id) {
        TestTask task = findTaskById(id);

        if (task.getStatus() != TestTaskStatus.PAUSED) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "只能恢复已暂停的任务");
        }

        task.setStatus(TestTaskStatus.RUNNING);
        TestTask updated = testTaskRepository.save(task);

        executeTaskAsync(id);

        return enrichTaskResponse(testTaskMapper.toResponse(updated));
    }

    @Transactional
    public TestTaskResponse cancelTask(Long id) {
        TestTask task = findTaskById(id);

        if (task.getStatus() == TestTaskStatus.COMPLETED || task.getStatus() == TestTaskStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "任务已完成或已取消");
        }

        task.setStatus(TestTaskStatus.CANCELLED);
        task.setCompletedAt(LocalDateTime.now());
        TestTask updated = testTaskRepository.save(task);
        return enrichTaskResponse(testTaskMapper.toResponse(updated));
    }

    @Transactional(readOnly = true)
    public Page<TestTaskItemResponse> getTaskItems(Long taskId, Pageable pageable) {
        return testTaskItemRepository.findByTestTaskId(taskId, pageable)
                .map(testTaskMapper::toItemResponse);
    }

    @Transactional(readOnly = true)
    public List<TestTaskItemResponse> getAllTaskItems(Long taskId) {
        List<TestTaskItem> items = testTaskItemRepository.findByTestTaskIdOrderBySequence(taskId);
        return testTaskMapper.toItemResponseList(items);
    }

    private void validateTaskRequest(TestTaskCreateRequest request) {
        if (!testSetRepository.existsById(request.getTestSetId())) {
            throw new BusinessException(ErrorCode.TEST_SET_NOT_FOUND, "测评集不存在");
        }
        if (!environmentRepository.existsById(request.getEnvironmentId())) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND, "环境不存在");
        }
        if (!pluginRepository.existsById(request.getExecutionPluginId())) {
            throw new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "执行插件不存在");
        }
        if (!pluginRepository.existsById(request.getEvaluationPluginId())) {
            throw new BusinessException(ErrorCode.PLUGIN_NOT_FOUND, "评估插件不存在");
        }
    }

    private TestTask findTaskById(Long id) {
        return testTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private boolean isTaskRunning(Long taskId) {
        return testTaskRepository.findById(taskId)
                .map(t -> t.getStatus() == TestTaskStatus.RUNNING)
                .orElse(false);
    }

    @Transactional
    public void updateTaskFailed(Long taskId, String errorMessage) {
        testTaskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(TestTaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            task.setCompletedAt(LocalDateTime.now());
            testTaskRepository.save(task);
        });
    }

    @Transactional
    public void completeTask(Long taskId) {
        testTaskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(TestTaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            testTaskRepository.save(task);
            log.info("Task {} completed successfully", taskId);
        });
    }

    private TestTaskResponse enrichTaskResponse(TestTaskResponse response) {
        testSetRepository.findById(response.getTestSetId())
                .ifPresent(ts -> response.setTestSetName(ts.getName()));
        environmentRepository.findById(response.getEnvironmentId())
                .ifPresent(env -> response.setEnvironmentName(env.getName()));
        pluginRepository.findById(response.getExecutionPluginId())
                .ifPresent(p -> response.setExecutionPluginName(p.getName()));
        pluginRepository.findById(response.getEvaluationPluginId())
                .ifPresent(p -> response.setEvaluationPluginName(p.getName()));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String config) {
        if (StringUtils.isBlank(config)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(config, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse config: {}", config, e);
            return Map.of();
        }
    }
}
