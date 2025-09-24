package com.vocata.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * 密码哈希生成工具 - 用于生成测试数据
 */
@DisplayName("密码哈希生成工具")
class PasswordHashGeneratorTest {

    @Test
    @DisplayName("生成测试数据的密码哈希")
    void generateTestPasswordHashes() {
        PasswordEncoder encoder = new PasswordEncoder();

        // 生成测试密码的哈希值
        String testPassword = "test123456";
        String hash1 = encoder.encode(testPassword);
        String hash2 = encoder.encode(testPassword);
        String hash3 = encoder.encode(testPassword);

        System.out.println("=== 密码哈希生成结果 ===");
        System.out.printf("原始密码: %s%n", testPassword);
        System.out.printf("哈希值1:  %s%n", hash1);
        System.out.printf("哈希值2:  %s%n", hash2);
        System.out.printf("哈希值3:  %s%n", hash3);
        System.out.println();

        // 验证生成的哈希值
        System.out.println("=== 验证结果 ===");
        System.out.printf("哈希值1验证: %s%n", encoder.matches(testPassword, hash1));
        System.out.printf("哈希值2验证: %s%n", encoder.matches(testPassword, hash2));
        System.out.printf("哈希值3验证: %s%n", encoder.matches(testPassword, hash3));
        System.out.println();

        // 生成SQL更新语句
        System.out.println("=== 数据库更新SQL ===");
        System.out.printf("-- 更新admin用户密码%n");
        System.out.printf("UPDATE vocata_user SET password = '%s' WHERE username = 'admin';%n", hash1);
        System.out.printf("%n-- 更新testuser用户密码%n");
        System.out.printf("UPDATE vocata_user SET password = '%s' WHERE username = 'testuser';%n", hash2);
        System.out.println();

        // 生成其他常用密码的哈希
        String[] commonPasswords = {"123456", "password", "admin123", "user123"};
        System.out.println("=== 其他常用密码哈希 ===");
        for (String pwd : commonPasswords) {
            String hash = encoder.encode(pwd);
            System.out.printf("密码: %-10s -> %s%n", pwd, hash);
        }
    }

    @Test
    @DisplayName("验证现有数据库中的哈希值")
    void verifyExistingHashes() {
        PasswordEncoder encoder = new PasswordEncoder();

        // 这是数据库中现有的哈希值（示例）
        String existingHash = "$2a$12$rK8F6G9H.6X1K2L3M4N5O6P7Q8R9S0T1U2V3W4X5Y6Z7A8B9C0D1E2F";

        // 测试各种可能的密码
        String[] possiblePasswords = {
            "test123456",
            "123456",
            "password",
            "admin",
            "admin123",
            "vocata123"
        };

        System.out.println("=== 验证现有哈希值 ===");
        System.out.printf("现有哈希: %s%n", existingHash);

        boolean found = false;
        for (String pwd : possiblePasswords) {
            try {
                boolean matches = encoder.matches(pwd, existingHash);
                System.out.printf("密码: %-12s -> %s%n", pwd, matches ? "✓ 匹配" : "✗ 不匹配");
                if (matches) {
                    found = true;
                }
            } catch (Exception e) {
                System.out.printf("密码: %-12s -> 错误: %s%n", pwd, e.getMessage());
            }
        }

        if (!found) {
            System.out.println("注意: 现有哈希值可能是示例数据，需要重新生成真实的哈希值");
        }
    }
}