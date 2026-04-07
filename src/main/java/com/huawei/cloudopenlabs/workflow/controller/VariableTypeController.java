package com.huawei.cloudopenlabs.workflow.controller;

import com.huawei.cloudopenlabs.workflow.entity.VariableTypeEntity;
import com.huawei.cloudopenlabs.workflow.mapper.VariableTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 变量类型管理控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/variable-types")
@RequiredArgsConstructor
public class VariableTypeController {

    private final VariableTypeMapper variableTypeMapper;

    /**
     * 获取所有启用的变量类型
     *
     * @return 变量类型列表
     */
    @GetMapping
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
    public ResponseEntity<List<VariableTypeEntity>> getVariableTypesByCategory(
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
    public ResponseEntity<VariableTypeEntity> getVariableTypeById(
            @PathVariable Long id) {
        VariableTypeEntity variableType = variableTypeMapper.selectById(id);
        if (variableType == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(variableType);
    }
}
