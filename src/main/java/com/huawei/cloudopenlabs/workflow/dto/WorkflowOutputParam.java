/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工作流输出参数 DTO
 * 用于表示结束节点的输出参数，支持文本和文件类型
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowOutputParam {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 显示名称/标签
     */
    private String label;

    /**
     * 参数类型编码（如 String, Integer, File<Pdf>）
     */
    private String type;

    /**
     * 类型分类（BASIC/COMPOSITE）
     */
    private String category;

    /**
     * 文件类型（仅文件类型参数）
     * 从 type 中提取，如 File<Pdf> 提取为 Pdf
     */
    private String fileType;

    /**
     * 参数描述
     */
    private String description;

    /**
     * 参数值
     * 文本类型为字符串/数字等
     * 文件类型为文件ID
     */
    private Object value;

    /**
     * 原始文件名（仅文件类型）
     */
    private String fileName;

    /**
     * 文件大小，单位字节（仅文件类型）
     */
    private Long fileSize;

    /**
     * 文件下载URL（仅文件类型）
     */
    private String downloadUrl;

    /**
     * 文件列表（仅文件数组类型，如 Array<File<Txt>>）
     */
    private List<FileInfo> files;

    /**
     * 文件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        /**
         * 文件ID
         */
        private String fileId;

        /**
         * 原始文件名
         */
        private String fileName;

        /**
         * 文件大小，单位字节
         */
        private Long fileSize;

        /**
         * 文件下载URL
         */
        private String downloadUrl;
    }

    /**
     * 判断是否为文本类型参数
     */
    public boolean isTextType() {
        return "BASIC".equals(category) ||
               (type != null && !type.startsWith("File") && !type.startsWith("Array<File"));
    }

    /**
     * 判断是否为单文件类型参数
     */
    public boolean isFileType() {
        return type != null && type.startsWith("File<");
    }

    /**
     * 判断是否为文件数组类型参数
     */
    public boolean isFileArrayType() {
        return type != null && type.startsWith("Array<File<");
    }
}
