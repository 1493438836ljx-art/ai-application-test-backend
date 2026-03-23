package com.example.demo.workflow.config;

import com.example.demo.workflow.entity.VariableTypeEntity;
import com.example.demo.workflow.mapper.VariableTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 变量类型数据初始化器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class VariableTypeDataInitializer implements CommandLineRunner {

    private final VariableTypeMapper variableTypeMapper;

    @Override
    public void run(String... args) {
        log.info("初始化变量类型数据...");
        initVariableTypes();
    }

    private void initVariableTypes() {
        // 基本类型
        createVariableTypeIfNotExists("String", "字符串", "BASIC", null, null, "字符串类型", 1);
        createVariableTypeIfNotExists("Boolean", "布尔值", "BASIC", null, null, "布尔值类型", 2);
        createVariableTypeIfNotExists("Integer", "整数", "BASIC", null, null, "整数类型", 3);
        createVariableTypeIfNotExists("Object", "对象", "BASIC", null, null, "对象类型", 4);
        createVariableTypeIfNotExists("Times", "时间", "BASIC", null, null, "时间类型", 5);

        // 复合类型 - Array
        createVariableTypeIfNotExists("Array", "数组", "COMPOSITE", null, null, "数组类型", 10);
        createVariableTypeIfNotExists("Array<String>", "字符串数组", "COMPOSITE", "String", null, "字符串数组类型", 11);
        createVariableTypeIfNotExists("Array<Boolean>", "布尔值数组", "COMPOSITE", "Boolean", null, "布尔值数组类型", 12);
        createVariableTypeIfNotExists("Array<Integer>", "整数数组", "COMPOSITE", "Integer", null, "整数数组类型", 13);
        createVariableTypeIfNotExists("Array<Object>", "对象数组", "COMPOSITE", "Object", null, "对象数组类型", 14);
        createVariableTypeIfNotExists("Array<Times>", "时间数组", "COMPOSITE", "Times", null, "时间数组类型", 15);

        // 复合类型 - File
        createVariableTypeIfNotExists("File", "文件", "COMPOSITE", null, null, "文件类型", 20);
        createVariableTypeIfNotExists("File<Zip>", "ZIP文件", "COMPOSITE", null, "Zip", "ZIP压缩文件类型", 21);
        createVariableTypeIfNotExists("File<Doc>", "文档文件", "COMPOSITE", null, "Doc", "文档文件类型", 22);
        createVariableTypeIfNotExists("File<Excel>", "Excel文件", "COMPOSITE", null, "Excel", "Excel表格文件类型", 23);
        createVariableTypeIfNotExists("File<Txt>", "文本文件", "COMPOSITE", null, "Txt", "文本文件类型", 24);

        log.info("变量类型数据初始化完成");
    }

    private void createVariableTypeIfNotExists(String code, String name, String category,
            String elementType, String fileType, String description, int sortOrder) {
        if (!variableTypeMapper.existsByCode(code)) {
            VariableTypeEntity variableType = new VariableTypeEntity();
            variableType.setCode(code);
            variableType.setName(name);
            variableType.setCategory(category);
            variableType.setElementType(elementType);
            variableType.setFileType(fileType);
            variableType.setDescription(description);
            variableType.setSortOrder(sortOrder);
            variableType.setEnabled(true);
            variableTypeMapper.insert(variableType);
            log.debug("创建变量类型: {}", code);
        }
    }
}
