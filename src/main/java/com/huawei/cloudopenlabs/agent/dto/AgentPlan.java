/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行计划
 * <p>
 * 表示 AI 返回的结构化执行计划，包含状态、推理、查询和操作
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentPlan {

    /**
     * 计划状态
     */
    private String status;

    /**
     * AI 的推理过程
     */
    private String reasoning;

    /**
     * 任务摘要
     */
    private String summary;

    /**
     * 执行结果（完成时）
     */
    private JsonNode result;

    /**
     * 查询请求列表
     */
    @Builder.Default
    private List<Query> queries = new ArrayList<>();

    /**
     * 操作请求列表
     */
    @Builder.Default
    private List<Action> actions = new ArrayList<>();

    /**
     * 查询请求定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Query {
        /**
         * 查询ID
         */
        private String id;

        /**
         * API 路径
         */
        private String path;

        /**
         * HTTP 方法
         */
        @Builder.Default
        private String method = "GET";

        /**
         * 查询描述
         */
        private String description;

        /**
         * 查询参数
         */
        private JsonNode params;
    }

    /**
     * 操作请求定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Action {
        /**
         * 操作ID
         */
        private String id;

        /**
         * API 路径
         */
        private String path;

        /**
         * HTTP 方法
         */
        @Builder.Default
        private String method = "POST";

        /**
         * 操作描述
         */
        private String description;

        /**
         * 请求体
         */
        private JsonNode body;
    }

    // ==================== 状态常量 ====================

    /**
     * 状态：需要查询
     */
    public static final String STATUS_QUERY = "query";

    /**
     * 状态：需要执行操作
     */
    public static final String STATUS_ACTION = "action";

    /**
     * 状态：任务完成
     */
    public static final String STATUS_COMPLETE = "complete";

    /**
     * 状态：解析错误
     */
    public static final String STATUS_PARSE_ERROR = "parse_error";

    /**
     * 判断是否为查询状态
     */
    public boolean isQuery() {
        return STATUS_QUERY.equalsIgnoreCase(status);
    }

    /**
     * 判断是否为操作状态
     */
    public boolean isAction() {
        return STATUS_ACTION.equalsIgnoreCase(status);
    }

    /**
     * 判断是否为完成状态
     */
    public boolean isComplete() {
        return STATUS_COMPLETE.equalsIgnoreCase(status);
    }

    /**
     * 判断是否为解析错误状态
     */
    public boolean isParseError() {
        return STATUS_PARSE_ERROR.equalsIgnoreCase(status) || status == null;
    }

    /**
     * 判断是否有查询请求
     */
    public boolean hasQueries() {
        return queries != null && !queries.isEmpty();
    }

    /**
     * 判断是否有操作请求
     */
    public boolean hasActions() {
        return actions != null && !actions.isEmpty();
    }
}
