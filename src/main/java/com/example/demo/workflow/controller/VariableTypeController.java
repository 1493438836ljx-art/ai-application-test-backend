package com.example.demo.workflow.controller;

import com.example.demo.workflow.entity.VariableTypeEntity;
import com.example.demo.workflow.mapper.VariableTypeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 变量类型管理控制器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/variable-types")
@RequiredArgsConstructor
@Tag(name = "变量类型管理", description = "工作流变量类型的查询接口")
public class VariableTypeController {

    private final VariableTypeMapper variableTypeMapper;

    /**
     * 获取所有启用的变量类型
     *
     * @return 变量类型列表
     */
    @GetMapping
    @Operation(summary = "获取所有变量类型", description = "获取所有启用的变量类型列表")
    public ResponseEntity<List<VariableTypeEntity>> getAllVariableTypes() {
        List<VariableTypeEntity> variableTypes = variableTypeMapper.selectEnabled();
        return ResponseEntity.ok(variableTypes);
    }

    /**
     * 获取变量类型分类列表
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    @Operation(summary = "获取变量类型分类列表", description = "获取所有变量类型分类")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = variableTypeMapper.selectAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * 根据分类获取变量类型
     *
     * @param category 分类
     * @return 变量类型列表
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "根据分类获取变量类型", description = "根据分类获取变量类型列表")
    public ResponseEntity<List<VariableTypeEntity>> getVariableTypesByCategory(
            @Parameter(description = "分类 (BASIC/COMPOSITE)", required = true)
            @PathVariable String category) {
        List<VariableTypeEntity> variableTypes = variableTypeMapper.selectEnabledByCategory(category);
        return ResponseEntity.ok(variableTypes);
    }

    /**
     * 根据ID获取变量类型
     *
     * @param id 变量类型ID
     * @return 变量类型
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取变量类型详情", description = "根据ID获取变量类型详情")
    public ResponseEntity<VariableTypeEntity> getVariableTypeById(
            @Parameter(description = "变量类型ID", required = true)
            @PathVariable Long id) {
        VariableTypeEntity variableType = variableTypeMapper.selectById(id);
        if (variableType == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(variableType);
    }
}
