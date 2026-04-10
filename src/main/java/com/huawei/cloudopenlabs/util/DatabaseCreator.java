/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * OpenGauss 数据库初始化工具
 * 用于创建数据库（如果不存在）
 */
public class DatabaseCreator {

    public static void main(String[] args) {
        String host = "8.218.55.180";
        String port = "5432";
        String username = "remote_user";
        String password = "Gauss@2026";
        String database = "ai_studio";

        // 连接 URL（连接到默认的 postgres 数据库）
        String url = String.format("jdbc:postgresql://%s:%s/postgres?currentSchema=public",
                host, port);

        System.out.println("========================================");
        System.out.println("OpenGauss 数据库初始化工具");
        System.out.println("========================================");
        System.out.println("服务器: " + host + ":" + port);
        System.out.println("用户名: " + username);
        System.out.println("目标数据库: " + database);
        System.out.println("========================================\n");

        try {
            // 加载驱动
            Class.forName("org.postgresql.Driver");
            System.out.println("OpenGauss 驱动加载成功");

            // 连接到 OpenGauss 服务器
            System.out.println("\n正在连接到 OpenGauss 服务器...");
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("成功连接到 OpenGauss 服务器");

            // 创建数据库
            System.out.println("\n正在检查数据库 '" + database + "'...");
            Statement statement = connection.createStatement();

            // 检查数据库是否已存在
            var rs = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + database + "'");
            if (rs.next()) {
                System.out.println("数据库 '" + database + "' 已存在");
            } else {
                String createDbSQL = "CREATE DATABASE " + database + " ENCODING 'UTF8'";
                statement.executeUpdate(createDbSQL);
                System.out.println("数据库 '" + database + "' 已创建");
            }

            // 显示数据库列表
            System.out.println("\n当前服务器上的数据库列表：");
            rs = statement.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false");
            while (rs.next()) {
                System.out.println("  - " + rs.getString(1));
            }

            // 关闭连接
            rs.close();
            statement.close();
            connection.close();
            System.out.println("\n========================================");
            System.out.println("数据库初始化完成！");
            System.out.println("========================================");

        } catch (ClassNotFoundException e) {
            System.err.println("\n错误: OpenGauss 驱动未找到");
            System.err.println("错误详情: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("\n错误: " + e.getMessage());
            System.err.println("\n可能的原因：");
            System.err.println("  1. OpenGauss 服务器未运行或无法访问");
            System.err.println("  2. 用户名或密码不正确");
            System.err.println("  3. 用户权限不足");
            System.err.println("  4. 网络连接问题");
            System.err.println("  5. 防火墙阻止了连接");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
