package com.huawei.cloudopenlabs.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库迁移工具
 * 用于手动执行数据库迁移脚本
 */
public class MigrationTool {

    private static final String DB_URL = "jdbc:mysql://8.218.55.180:3306/ai_test_platform?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8";
    private static final String DB_USER = "remote_user";
    private static final String DB_PASSWORD = "873899";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to database successfully!");

            // 检查字段是否已存在
            String checkColumnSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'ai_test_platform' " +
                    "AND TABLE_NAME = 'agent_session' " +
                    "AND COLUMN_NAME = 'parse_error_count'";

            ResultSet rs = stmt.executeQuery(checkColumnSql);
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                System.out.println("Column 'parse_error_count' already exists in agent_session table.");
                return;
            }

            // 添加字段
            String alterSql = "ALTER TABLE agent_session " +
                    "ADD COLUMN parse_error_count INT NOT NULL DEFAULT 0 " +
                    "COMMENT 'Parse error count for limiting retry attempts' " +
                    "AFTER round_count";

            stmt.executeUpdate(alterSql);
            System.out.println("Successfully added 'parse_error_count' column to agent_session table!");

        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
