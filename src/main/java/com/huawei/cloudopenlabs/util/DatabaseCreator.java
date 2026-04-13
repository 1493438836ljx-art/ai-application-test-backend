/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * OpenGauss 数据库初始化工具
 * 用于创建数据库（如果不存在）
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
public class DatabaseCreator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCreator.class);

    public static void main(String[] args) {
        String host = "8.218.55.180";
        String port = "5432";
        String username = "remote_user";
        String password = "Gauss@2026";
        String database = "ai_studio";

        // 连接 URL（连接到默认的 postgres 数据库）
        String url = String.format("jdbc:postgresql://%s:%s/postgres?currentSchema=public",
                host, port);

        log.info("========================================");
        log.info("OpenGauss Database Initialization Tool");
        log.info("========================================");
        log.info("Server: {}:{}", host, port);
        log.info("Username: {}", username);
        log.info("Target database: {}", database);
        log.info("========================================");

        try {
            Class.forName("org.postgresql.Driver");
            log.info("OpenGauss driver loaded successfully");

            log.info("Connecting to OpenGauss server...");
            Connection connection = DriverManager.getConnection(url, username, password);
            log.info("Successfully connected to OpenGauss server");

            ensureDatabaseExists(connection, database);

            Statement statement = connection.createStatement();
            listDatabases(statement);

            statement.close();
            connection.close();
            log.info("========================================");
            log.info("Database initialization completed!");
            log.info("========================================");

        } catch (ClassNotFoundException e) {
            log.error("OpenGauss driver not found: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            log.error("Possible causes:");
            log.error("  1. OpenGauss server is not running or unreachable");
            log.error("  2. Incorrect username or password");
            log.error("  3. Insufficient user permissions");
            log.error("  4. Network connection issue");
            log.error("  5. Firewall blocking connection");
            System.exit(1);
        }
    }

    /**
     * 确保数据库存在，不存在则创建
     *
     * @param connection 数据库连接
     * @param database   目标数据库名
     * @throws Exception SQL执行异常
     */
    private static void ensureDatabaseExists(Connection connection, String database) throws Exception {
        log.info("Checking database '{}'...", database);
        Statement statement = connection.createStatement();

        var rs = statement.executeQuery(
                "SELECT 1 FROM pg_database WHERE datname = '" + database + "'");
        if (rs.next()) {
            log.info("Database '{}' already exists", database);
        } else {
            String createDbSQL = "CREATE DATABASE " + database + " ENCODING 'UTF8'";
            statement.executeUpdate(createDbSQL);
            log.info("Database '{}' created", database);
        }

        rs.close();
        statement.close();
    }

    /**
     * 列出服务器上所有非模板数据库
     *
     * @param statement SQL语句对象
     * @throws Exception SQL执行异常
     */
    private static void listDatabases(Statement statement) throws Exception {
        log.info("Databases on current server:");
        var rs = statement.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false");
        while (rs.next()) {
            log.info("  - {}", rs.getString(1));
        }
        rs.close();
    }
}
