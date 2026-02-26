package com.example.demo.task.mapper;

import com.example.demo.task.dto.*;
import com.example.demo.task.entity.TestTask;
import com.example.demo.task.entity.TestTaskItem;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TestTaskMapper {

    TestTask toEntity(TestTaskCreateRequest request);

    @Mapping(target = "testSetName", ignore = true)
    @Mapping(target = "environmentName", ignore = true)
    @Mapping(target = "executionPluginName", ignore = true)
    @Mapping(target = "evaluationPluginName", ignore = true)
    @Mapping(target = "progress", expression = "java(calculateProgress(testTask))")
    TestTaskResponse toResponse(TestTask testTask);

    List<TestTaskResponse> toResponseList(List<TestTask> testTasks);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(TestTaskUpdateRequest request, @MappingTarget TestTask testTask);

    TestTaskItem toEntity(TestTaskItemResponse response);

    TestTaskItemResponse toItemResponse(TestTaskItem item);

    List<TestTaskItemResponse> toItemResponseList(List<TestTaskItem> items);

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
