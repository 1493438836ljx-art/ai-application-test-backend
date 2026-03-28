package com.example.demo.workflow.config;

import com.example.demo.dictionary.entity.DataDictionary;
import com.example.demo.dictionary.mapper.DataDictionaryMapper;
import com.example.demo.workflow.entity.VariableTypeEntity;
import com.example.demo.workflow.mapper.VariableTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private final DataDictionaryMapper dataDictionaryMapper;

    @Override
    public void run(String... args) {
        log.info("初始化变量类型数据...");
        initVariableTypes();
    }

    private void initVariableTypes() {
        // 基本类型
        createVariableTypeIfNotExists("String", "字符串", "BASIC", null, null, null, "字符串类型", 1);
        createVariableTypeIfNotExists("Boolean", "布尔值", "BASIC", null, null, null, "布尔值类型", 2);
        createVariableTypeIfNotExists("Integer", "整数", "BASIC", null, null, null, "整数类型", 3);
        createVariableTypeIfNotExists("Object", "对象", "BASIC", null, null, null, "对象类型", 4);
        createVariableTypeIfNotExists("Times", "时间", "BASIC", null, null, null, "时间类型", 5);

        // 复合类型 - Array
        createVariableTypeIfNotExists("Array", "数组", "COMPOSITE", null, null, null, "数组类型", 10);
        createVariableTypeIfNotExists("Array<String>", "字符串数组", "COMPOSITE", "String", null, null, "字符串数组类型", 11);
        createVariableTypeIfNotExists("Array<Boolean>", "布尔值数组", "COMPOSITE", "Boolean", null, null, "布尔值数组类型", 12);
        createVariableTypeIfNotExists("Array<Integer>", "整数数组", "COMPOSITE", "Integer", null, null, "整数数组类型", 13);
        createVariableTypeIfNotExists("Array<Object>", "对象数组", "COMPOSITE", "Object", null, null, "对象数组类型", 14);
        createVariableTypeIfNotExists("Array<Times>", "时间数组", "COMPOSITE", "Times", null, null, "时间数组类型", 15);
        createVariableTypeIfNotExists("Array<Time>", "时间数组", "COMPOSITE", "Time", null, null, "时间数组类型", 16);
        // Array<File<FileType>> 类型
        createVariableTypeIfNotExists("Array<File<Zip>>", "ZIP文件数组", "COMPOSITE", "File", "Zip", null, "ZIP压缩文件数组类型", 17);
        createVariableTypeIfNotExists("Array<File<Doc>>", "文档文件数组", "COMPOSITE", "File", "Doc", null, "文档文件数组类型", 18);
        createVariableTypeIfNotExists("Array<File<Docx>>", "Docx文件数组", "COMPOSITE", "File", "Docx", null, "Docx文件数组类型", 19);
        createVariableTypeIfNotExists("Array<File<Excel>>", "Excel文件数组", "COMPOSITE", "File", "Excel", null, "Excel表格文件数组类型", 20);
        createVariableTypeIfNotExists("Array<File<Pdf>>", "PDF文件数组", "COMPOSITE", "File", "Pdf", null, "PDF文件数组类型", 21);
        createVariableTypeIfNotExists("Array<File<Txt>>", "文本文件数组", "COMPOSITE", "File", "Txt", null, "文本文件数组类型", 22);

        // 复合类型 - File
        createVariableTypeIfNotExists("File", "文件", "COMPOSITE", null, null, null, "文件类型", 30);
        createVariableTypeIfNotExists("File<Zip>", "ZIP文件", "COMPOSITE", null, "Zip", null, "ZIP压缩文件类型", 31);
        createVariableTypeIfNotExists("File<Doc>", "文档文件", "COMPOSITE", null, "Doc", null, "文档文件类型", 32);
        createVariableTypeIfNotExists("File<Docx>", "Docx文件", "COMPOSITE", null, "Docx", null, "Docx文件类型", 33);
        createVariableTypeIfNotExists("File<Excel>", "Excel文件", "COMPOSITE", null, "Excel", null, "Excel表格文件类型", 34);
        createVariableTypeIfNotExists("File<Pdf>", "PDF文件", "COMPOSITE", null, "Pdf", null, "PDF文件类型", 35);
        createVariableTypeIfNotExists("File<Txt>", "文本文件", "COMPOSITE", null, "Txt", null, "文本文件类型", 36);

        // 复合类型 - Dictionary
        initDictionaryTypes();

        log.info("变量类型数据初始化完成");
    }

    /**
     * 初始化Dictionary类型（动态从数据字典表获取）
     */
    private void initDictionaryTypes() {
        // 创建Dictionary父类型
        createVariableTypeIfNotExists("Dictionary", "数据字典", "COMPOSITE", null, null, null, "数据字典类型", 50);

        // 从数据字典表获取所有字典
        List<DataDictionary> dictionaries = dataDictionaryMapper.selectAllForDropdown();
        int sortOrder = 51;
        for (DataDictionary dict : dictionaries) {
            String code = "Dictionary<" + dict.getName() + ">";
            createVariableTypeIfNotExists(
                code,
                dict.getName(),
                "COMPOSITE",
                null,
                null,
                dict.getName(),  // dictionaryType
                "数据字典: " + dict.getName(),
                sortOrder++
            );
        }
        log.info("Dictionary子类型初始化完成，共 {} 个", dictionaries.size());
    }

    private void createVariableTypeIfNotExists(String code, String name, String category,
            String elementType, String fileType, String dictionaryType, String description, int sortOrder) {
        if (!variableTypeMapper.existsByCode(code)) {
            VariableTypeEntity variableType = new VariableTypeEntity();
            variableType.setCode(code);
            variableType.setName(name);
            variableType.setCategory(category);
            variableType.setElementType(elementType);
            variableType.setFileType(fileType);
            variableType.setDictionaryType(dictionaryType);
            variableType.setDescription(description);
            variableType.setSortOrder(sortOrder);
            variableType.setEnabled(true);
            variableTypeMapper.insert(variableType);
            log.debug("创建变量类型: {}", code);
        }
    }
}
