package com.huawei.cloudopenlabs.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库迁移工具
 * 用于手动执行数据库迁移脚本（OpenGauss）
 */
public class MigrationTool {

    private static final String DB_URL = "jdbc:postgresql://8.218.55.180:5432/ai_studio?currentSchema=public";
    private static final String DB_USER = "remote_user";
    private static final String DB_PASSWORD = "Gauss@2026";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to OpenGauss database successfully!");

            // 检查字段是否已存在
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_schema = 'public' " +
                    "AND table_name = 'agent_session' " +
                    "AND column_name = 'parse_error_count'";

            ResultSet rs = stmt.executeQuery(checkColumnSql);
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                System.out.println("Column 'parse_error_count' already exists in agent_session table.");
            } else {
                // 添加字段
                String alterSql = "ALTER TABLE agent_session " +
                        "ADD COLUMN parse_error_count INT NOT NULL DEFAULT 0";
                stmt.executeUpdate(alterSql);

                // 添加字段注释
                String commentSql = "COMMENT ON COLUMN agent_session.parse_error_count IS 'Parse error count for limiting retry attempts'";
                stmt.executeUpdate(commentSql);

                System.out.println("Successfully added 'parse_error_count' column to agent_session table!");
            }

            // 检查 start_time 字段是否存在
            String checkStartTimeSql = "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_schema = 'public' " +
                    "AND table_name = 'agent_session' " +
                    "AND column_name = 'start_time'";

            rs = stmt.executeQuery(checkStartTimeSql);
            rs.next();
            int startTimeCount = rs.getInt(1);

            if (startTimeCount == 0) {
                // 添加 start_time 字段
                String alterStartTimeSql = "ALTER TABLE agent_session " +
                        "ADD COLUMN start_time BIGINT DEFAULT NULL";
                stmt.executeUpdate(alterStartTimeSql);

                String commentSql = "COMMENT ON COLUMN agent_session.start_time IS 'Execution start timestamp in milliseconds'";
                stmt.executeUpdate(commentSql);

                System.out.println("Successfully added 'start_time' column to agent_session table!");
            } else {
                System.out.println("Column 'start_time' already exists in agent_session table.");
            }

        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
