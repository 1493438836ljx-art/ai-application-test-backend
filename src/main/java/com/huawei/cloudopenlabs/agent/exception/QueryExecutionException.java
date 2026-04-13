/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.exception;

/**
 * 查询执行异常
 * <p>
 * 当查询执行失败时抛出
 * </p>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
public class QueryExecutionException extends RuntimeException {

    /**
     * 查询ID
     */
    private String queryId;

    /**
     * 查询路径
     */
    private String path;

    public QueryExecutionException(String message) {
        super(message);
    }

    public QueryExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryExecutionException(String queryId, String path, String message) {
        super(message);
        this.queryId = queryId;
        this.path = path;
    }

    public QueryExecutionException(String queryId, String path, String message, Throwable cause) {
        super(message, cause);
        this.queryId = queryId;
        this.path = path;
    }

    public String getQueryId() {
        return queryId;
    }

    public String getPath() {
        return path;
    }
}
