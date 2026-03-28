package com.example.demo.skill.util;

import com.example.demo.common.exception.BusinessException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Skill参数类型验证工具类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public class ParameterTypeValidator {

    /**
     * 基本类型列表
     */
    private static final List<String> BASIC_TYPES = List.of(
            "String", "Boolean", "Integer", "Object", "Times"
    );

    /**
     * Array元素类型列表
     */
    private static final List<String> ARRAY_ELEMENT_TYPES = List.of(
            "String", "Integer", "Boolean", "Time", "Object"
    );

    /**
     * File文件类型列表
     */
    private static final List<String> FILE_TYPES = List.of(
            "Zip", "Doc", "Docx", "Excel", "Pdf", "Txt"
    );

    /**
     * 匹配 Array<ElementType> 格式
     */
    private static final Pattern ARRAY_PATTERN = Pattern.compile("^Array<([A-Za-z]+)>$");

    /**
     * 匹配 File<FileType> 格式
     */
    private static final Pattern FILE_PATTERN = Pattern.compile("^File<([A-Za-z]+)>$");

    /**
     * 匹配 Array<File<FileType>> 格式
     */
    private static final Pattern ARRAY_FILE_PATTERN = Pattern.compile("^Array<File<([A-Za-z]+)>>$");

    /**
     * 验证参数类型是否有效
     *
     * @param type 参数类型字符串
     * @return 是否有效
     */
    public static boolean isValidType(String type) {
        if (type == null || type.isEmpty()) {
            return false;
        }

        // 1. 检查是否是基本类型
        if (BASIC_TYPES.contains(type)) {
            return true;
        }

        // 2. 检查是否是 Array<File<FileType>> 格式
        var arrayFileMatcher = ARRAY_FILE_PATTERN.matcher(type);
        if (arrayFileMatcher.matches()) {
            String fileType = arrayFileMatcher.group(1);
            return FILE_TYPES.contains(fileType);
        }

        // 3. 检查是否是 Array<ElementType> 格式
        var arrayMatcher = ARRAY_PATTERN.matcher(type);
        if (arrayMatcher.matches()) {
            String elementType = arrayMatcher.group(1);
            // 元素类型可以是基本类型或File
            if (ARRAY_ELEMENT_TYPES.contains(elementType)) {
                return true;
            }
            // 检查是否是 Array<File>
            if ("File".equals(elementType)) {
                return true;
            }
            return false;
        }

        // 4. 检查是否是 File<FileType> 格式
        var fileMatcher = FILE_PATTERN.matcher(type);
        if (fileMatcher.matches()) {
            String fileType = fileMatcher.group(1);
            return FILE_TYPES.contains(fileType);
        }

        // 5. 检查是否是单独的 Array 或 File
        if ("Array".equals(type) || "File".equals(type)) {
            return true;
        }

        return false;
    }

    /**
     * 验证参数类型，无效时抛出异常
     *
     * @param type        参数类型字符串
     * @param paramName   参数名称（用于错误信息）
     * @throws BusinessException 如果类型无效
     */
    public static void validateType(String type, String paramName) {
        if (!isValidType(type)) {
            throw BusinessException.invalidParam(
                    String.format("参数 '%s' 的类型 '%s' 无效", paramName, type)
            );
        }
    }

    /**
     * 获取所有有效的基本类型
     */
    public static List<String> getBasicTypes() {
        return BASIC_TYPES;
    }

    /**
     * 获取所有有效的Array元素类型
     */
    public static List<String> getArrayElementTypes() {
        return ARRAY_ELEMENT_TYPES;
    }

    /**
     * 获取所有有效的File类型
     */
    public static List<String> getFileTypes() {
        return FILE_TYPES;
    }
}
