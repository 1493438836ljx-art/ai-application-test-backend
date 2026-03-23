package com.example.demo.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * MySQL 数据库初始化工具
 * 用于创建数据库（如果不存在）
 */
public class DatabaseCreator {

    public static void main(String[] args) {
        String host = "8.218.55.180";
        String port = "3306";
        String username = "remote_user";
        String password = "873899";
        String database = "ai_test_platform";

        // 连接 URL（不指定数据库）
        String url = String.format("jdbc:mysql://%s:%s/?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8",
                host, port);

        System.out.println("========================================");
        System.out.println("MySQL 数据库初始化工具");
        System.out.println("========================================");
        System.out.println("服务器: " + host + ":" + port);
        System.out.println("用户名: " + username);
        System.out.println("目标数据库: " + database);
        System.out.println("========================================\n");

        try {
            // 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ MySQL 驱动加载成功");

            // 连接到 MySQL 服务器
            System.out.println("\n正在连接到 MySQL 服务器...");
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("✓ 成功连接到 MySQL 服务器");

            // 创建数据库
            System.out.println("\n正在创建数据库 '" + database + "'...");
            Statement statement = connection.createStatement();

            // 创建数据库（如果不存在）
            String createDbSQL = String.format(
                    "CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    database);
            statement.executeUpdate(createDbSQL);
            System.out.println("✓ 数据库 '" + database + "' 已创建或已存在");

            // 验证数据库
            System.out.println("\n验证数据库...");
            statement.execute("USE `" + database + "`");
            System.out.println("✓ 可以访问数据库 '" + database + "'");

            // 显示数据库列表
            System.out.println("\n当前服务器上的数据库列表：");
            var rs = statement.executeQuery("SHOW DATABASES");
            while (rs.next()) {
                System.out.println("  - " + rs.getString(1));
            }

            // 关闭连接
            statement.close();
            connection.close();
            System.out.println("\n========================================");
            System.out.println("✅ 数据库初始化完成！");
            System.out.println("========================================");

        } catch (ClassNotFoundException e) {
            System.err.println("\n❌ 错误: MySQL 驱动未找到");
            System.err.println("错误详情: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("\n❌ 错误: " + e.getMessage());
            System.err.println("\n可能的原因：");
            System.err.println("  1. MySQL 服务器未运行或无法访问");
            System.err.println("  2. 用户名或密码不正确");
            System.err.println("  3. 用户权限不足");
            System.err.println("  4. 网络连接问题");
            System.err.println("  5. 防火墙阻止了连接");
            e.printStackTrace();
            System.exit(1);
        }
    }
}