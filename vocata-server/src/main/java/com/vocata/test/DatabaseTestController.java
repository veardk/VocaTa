package com.vocata.test;

import com.vocata.common.result.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库连接测试控制器
 */
@RestController
@RequestMapping("/test")
public class DatabaseTestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    /**
     * 测试数据库连接和表访问
     */
    @GetMapping("/database")
    public ApiResponse<Map<String, Object>> testDatabase() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 测试1: 基本连接信息
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                result.put("数据库URL", metaData.getURL());
                result.put("数据库用户", metaData.getUserName());
                result.put("数据库产品", metaData.getDatabaseProductName());
                result.put("当前Schema", connection.getSchema());
                result.put("连接状态", "成功");
            }

            // 测试2: 列出所有可见的表
            String sql = "SELECT table_schema, table_name FROM information_schema.tables WHERE table_type = 'BASE TABLE' ORDER BY table_schema, table_name";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(sql);
            result.put("所有表", tables);

            // 测试3: 查找vocata_user表
            String findTableSql = "SELECT table_schema, table_name FROM information_schema.tables WHERE table_name = 'vocata_user'";
            List<Map<String, Object>> userTables = jdbcTemplate.queryForList(findTableSql);
            result.put("vocata_user表位置", userTables);

            // 测试4: 尝试查询vocata_user表（不同schema）
            List<String> queries = new ArrayList<>();
            queries.add("SELECT count(*) FROM vocata_user");
            queries.add("SELECT count(*) FROM public.vocata_user");

            for (String query : queries) {
                try {
                    Integer count = jdbcTemplate.queryForObject(query, Integer.class);
                    result.put("查询结果_" + query, count);
                } catch (Exception e) {
                    result.put("查询错误_" + query, e.getMessage());
                }
            }

            // 测试5: 检查当前用户权限
            try {
                String privilegesSql = "SELECT * FROM information_schema.table_privileges WHERE grantee = current_user AND table_name = 'vocata_user'";
                List<Map<String, Object>> privileges = jdbcTemplate.queryForList(privilegesSql);
                result.put("表权限", privileges);
            } catch (Exception e) {
                result.put("权限检查错误", e.getMessage());
            }

        } catch (Exception e) {
            result.put("连接错误", e.getMessage());
        }

        return ApiResponse.success("数据库测试完成", result);
    }
}