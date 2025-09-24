package com.vocata.debug.controller;

import com.vocata.common.result.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DatabaseDiagnosisController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/database-info")
    public ApiResponse<Map<String, Object>> getDatabaseInfo() {
        Map<String, Object> info = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            // 基本连接信息
            DatabaseMetaData metaData = conn.getMetaData();
            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("url", metaData.getURL());
            info.put("userName", metaData.getUserName());

            // 当前连接信息
            try (Statement stmt = conn.createStatement()) {
                // 当前数据库
                ResultSet rs = stmt.executeQuery("SELECT current_database()");
                if (rs.next()) {
                    info.put("currentDatabase", rs.getString(1));
                }
                rs.close();

                // 当前schema
                rs = stmt.executeQuery("SELECT current_schema()");
                if (rs.next()) {
                    info.put("currentSchema", rs.getString(1));
                }
                rs.close();

                // 当前用户
                rs = stmt.executeQuery("SELECT current_user");
                if (rs.next()) {
                    info.put("currentUser", rs.getString(1));
                }
                rs.close();

                // search_path
                rs = stmt.executeQuery("SHOW search_path");
                if (rs.next()) {
                    info.put("searchPath", rs.getString(1));
                }
                rs.close();
            }

        } catch (Exception e) {
            info.put("error", "获取数据库信息失败: " + e.getMessage());
        }

        return ApiResponse.success(info);
    }

    @GetMapping("/check-vocata-user-table")
    public ApiResponse<Map<String, Object>> checkVocataUserTable() {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            // 检查表是否存在（在各种schema中）
            DatabaseMetaData metaData = conn.getMetaData();

            // 检查public schema中的表
            ResultSet tables = metaData.getTables(null, "public", "vocata_user", new String[]{"TABLE"});
            boolean tableExistsInPublic = tables.next();
            result.put("tableExistsInPublicSchema", tableExistsInPublic);
            tables.close();

            // 检查所有schema中的vocata_user表
            tables = metaData.getTables(null, null, "vocata_user", new String[]{"TABLE"});
            List<String> schemasWithTable = new ArrayList<>();
            while (tables.next()) {
                schemasWithTable.add(tables.getString("TABLE_SCHEM"));
            }
            result.put("schemasWithVocataUserTable", schemasWithTable);
            tables.close();

            // 尝试直接查询表
            try (Statement stmt = conn.createStatement()) {
                // 尝试查询public.vocata_user
                try {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM public.vocata_user");
                    if (rs.next()) {
                        result.put("publicVocataUserCount", rs.getInt(1));
                        result.put("canAccessPublicVocataUser", true);
                    }
                    rs.close();
                } catch (Exception e) {
                    result.put("canAccessPublicVocataUser", false);
                    result.put("publicAccessError", e.getMessage());
                }

                // 尝试查询vocata_user（不指定schema）
                try {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vocata_user");
                    if (rs.next()) {
                        result.put("vocataUserCount", rs.getInt(1));
                        result.put("canAccessVocataUser", true);
                    }
                    rs.close();
                } catch (Exception e) {
                    result.put("canAccessVocataUser", false);
                    result.put("accessError", e.getMessage());
                }

                // 检查表权限
                try {
                    ResultSet rs = stmt.executeQuery(
                        "SELECT privilege_type FROM information_schema.table_privileges " +
                        "WHERE table_schema = 'public' AND table_name = 'vocata_user' " +
                        "AND grantee = current_user"
                    );
                    List<String> privileges = new ArrayList<>();
                    while (rs.next()) {
                        privileges.add(rs.getString("privilege_type"));
                    }
                    result.put("currentUserPrivileges", privileges);
                    rs.close();
                } catch (Exception e) {
                    result.put("privilegeCheckError", e.getMessage());
                }
            }

        } catch (Exception e) {
            result.put("error", "检查表失败: " + e.getMessage());
        }

        return ApiResponse.success(result);
    }

    @GetMapping("/list-all-tables")
    public ApiResponse<Map<String, Object>> listAllTables() {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取所有表
            ResultSet tables = metaData.getTables(null, null, null, new String[]{"TABLE"});
            List<Map<String, String>> tableList = new ArrayList<>();

            while (tables.next()) {
                Map<String, String> table = new HashMap<>();
                table.put("catalog", tables.getString("TABLE_CAT"));
                table.put("schema", tables.getString("TABLE_SCHEM"));
                table.put("name", tables.getString("TABLE_NAME"));
                table.put("type", tables.getString("TABLE_TYPE"));
                tableList.add(table);
            }
            tables.close();

            result.put("allTables", tableList);
            result.put("tableCount", tableList.size());

            // 特别查找包含"vocata"或"user"的表
            List<Map<String, String>> vocataRelatedTables = new ArrayList<>();
            for (Map<String, String> table : tableList) {
                String tableName = table.get("name").toLowerCase();
                if (tableName.contains("vocata") || tableName.contains("user")) {
                    vocataRelatedTables.add(table);
                }
            }
            result.put("vocataRelatedTables", vocataRelatedTables);

        } catch (Exception e) {
            result.put("error", "获取表列表失败: " + e.getMessage());
        }

        return ApiResponse.success(result);
    }

    @GetMapping("/test-mybaits-connection")
    public ApiResponse<String> testMybatisConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return ApiResponse.success("MyBatis数据源连接正常，连接URL: " + conn.getMetaData().getURL());
        } catch (Exception e) {
            return ApiResponse.error("MyBatis数据源连接失败: " + e.getMessage());
        }
    }
}