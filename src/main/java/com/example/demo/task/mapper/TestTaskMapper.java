package com.example.demo.task.mapper;

import com.example.demo.task.dto.*;
import com.example.demo.task.entity.TestTask;
import com.example.demo.task.entity.TestTaskItem;
import org.mapstruct.*;

import java.util.List;

/**
 * 测试任务对象转换器
 * 提供测试任务实体与DTO之间的转换方法
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TestTaskMapper {

    /**
     * 将创建请求DTO转换为实体
     *
     * @param request 创建请求DTO
     * @return 测试任务实体
     */
    TestTask toEntity(TestTaskCreateRequest request);

    /**
     * 将实体转换为响应DTO
     *
     * @param testTask 测试任务实体
     * @return 测试任务响应DTO
     */
    @Mapping(target = "testSetName", ignore = true)
    @Mapping(target = "environmentName", ignore = true)
    @Mapping(target = "executionPluginName", ignore = true)
    @Mapping(target = "evaluationPluginName", ignore = true)
    @Mapping(target = "progress", expression = "java(calculateProgress(testTask))")
    TestTaskResponse toResponse(TestTask testTask);

    /**
     * 将实体列表转换为响应DTO列表
     *
     * @param testTasks 测试任务实体列表
     * @return 测试任务响应DTO列表
     */
    List<TestTaskResponse> toResponseList(List<TestTask> testTasks);

    /**
     * 使用更新请求DTO更新实体
     *
     * @param request 更新请求DTO
     * @param testTask 待更新的测试任务实体
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(TestTaskUpdateRequest request, @MappingTarget TestTask testTask);

    /**
     * 将执行项响应DTO转换为实体
     *
     * @param response 执行项响应DTO
     * @return 执行项实体
     */
    TestTaskItem toEntity(TestTaskItemResponse response);

    /**
     * 将执行项实体转换为响应DTO
     *
     * @param item 执行项实体
     * @return 执行项响应DTO
     */
    TestTaskItemResponse toItemResponse(TestTaskItem item);

    /**
     * 将执行项实体列表转换为响应DTO列表
     *
     * @param items 执行项实体列表
     * @return 执行项响应DTO列表
     */
    List<TestTaskItemResponse> toItemResponseList(List<TestTaskItem> items);

    /**
     * 计算任务执行进度
     *
     * @param testTask 测试任务实体
     * @return 执行进度百分比(0-100)
     */
    default Double calculateProgress(TestTask testTask) {
        if (testTask.getTotalItems() == null || testTask.getTotalItems() == 0) {
            return 0.0;
        }
        if (testTask.getCompletedItems() == null) {
            return 0.0;
        }
        return (testTask.getCompletedItems() * 100.0) / testTask.getTotalItems();
    }
}
