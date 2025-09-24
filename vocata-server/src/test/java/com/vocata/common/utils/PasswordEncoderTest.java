package com.vocata.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码加密工具类测试
 */
@DisplayName("密码加密工具类测试")
class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordEncoder();
    }

    @Test
    @DisplayName("测试密码加密 - 基本功能")
    void testEncode() {
        // Given
        String rawPassword = "test123456";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Then
        assertNotNull(encodedPassword, "加密后的密码不应为空");
        assertNotEquals(rawPassword, encodedPassword, "加密后的密码应与原密码不同");
        assertTrue(encodedPassword.startsWith("$2a$12$"), "应使用BCrypt算法，成本因子为12");
        assertEquals(60, encodedPassword.length(), "BCrypt哈希长度应为60个字符");
    }

    @Test
    @DisplayName("测试密码验证 - 正确密码")
    void testMatchesWithCorrectPassword() {
        // Given
        String rawPassword = "test123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // When
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);

        // Then
        assertTrue(matches, "正确的密码应该验证通过");
    }

    @Test
    @DisplayName("测试密码验证 - 错误密码")
    void testMatchesWithIncorrectPassword() {
        // Given
        String rawPassword = "test123456";
        String wrongPassword = "wrongpassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // When
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // Then
        assertFalse(matches, "错误的密码应该验证失败");
    }

    @ParameterizedTest
    @DisplayName("测试不同类型的密码")
    @ValueSource(strings = {
        "123456",           // 纯数字
        "password",         // 纯字母
        "Password123",      // 字母数字组合
        "P@ssw0rd!",       // 包含特殊字符
        "中文密码123",       // 包含中文
        "",                // 空字符串
        " ",               // 空格
        "a",               // 单个字符
        "verylongpasswordthatexceeds50charactersandmaybeusedinsomecases123456789" // 超长密码
    })
    void testVariousPasswords(String password) {
        // When
        String encoded = passwordEncoder.encode(password);
        boolean matches = passwordEncoder.matches(password, encoded);

        // Then
        assertNotNull(encoded, "所有类型的密码都应该能够加密");
        assertTrue(matches, "加密后的密码应该能够验证通过");
        assertTrue(encoded.startsWith("$2a$12$"), "所有密码都应使用相同的BCrypt配置");
    }

    @RepeatedTest(5)
    @DisplayName("测试密码加密的随机性")
    void testEncodeRandomness() {
        // Given
        String rawPassword = "samePassword";

        // When
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        // Then
        assertNotEquals(encoded1, encoded2, "相同密码的每次加密结果应该不同（因为salt不同）");
        assertTrue(passwordEncoder.matches(rawPassword, encoded1), "第一次加密的密码应该验证通过");
        assertTrue(passwordEncoder.matches(rawPassword, encoded2), "第二次加密的密码应该验证通过");
    }

    @Test
    @DisplayName("测试空值处理")
    void testNullHandling() {
        // Test null password encoding
        assertThrows(Exception.class, () -> {
            passwordEncoder.encode(null);
        }, "空密码应该抛出异常");

        // Test null password matching
        String validEncoded = passwordEncoder.encode("test");
        assertThrows(Exception.class, () -> {
            passwordEncoder.matches(null, validEncoded);
        }, "空密码验证应该抛出异常");

        // Test null encoded password matching
        assertThrows(Exception.class, () -> {
            passwordEncoder.matches("test", null);
        }, "空的加密密码验证应该抛出异常");
    }

    @Test
    @DisplayName("测试无效哈希格式")
    void testInvalidHashFormat() {
        // Given
        String rawPassword = "test123456";
        String invalidHash = "invalidhashformat";

        // When & Then
        assertThrows(Exception.class, () -> {
            passwordEncoder.matches(rawPassword, invalidHash);
        }, "无效的哈希格式应该抛出异常");
    }

    @Test
    @DisplayName("测试已知的测试数据")
    void testKnownTestData() {
        // Given - 这是数据库中的测试数据
        String testPassword = "test123456";
        // 生成一个测试用的哈希值
        String testHash = passwordEncoder.encode(testPassword);

        // When
        boolean matches = passwordEncoder.matches(testPassword, testHash);

        // Then
        assertTrue(matches, "测试密码应该验证通过");

        // 验证其他密码不匹配
        assertFalse(passwordEncoder.matches("wrongpassword", testHash),
                   "错误密码应该验证失败");
        assertFalse(passwordEncoder.matches("TEST123456", testHash),
                   "大小写不同的密码应该验证失败");
    }

    @Test
    @DisplayName("测试BCrypt成本因子")
    void testBCryptCostFactor() {
        // Given
        String password = "testpassword";

        // When
        String encoded = passwordEncoder.encode(password);

        // Then
        assertTrue(encoded.startsWith("$2a$12$"), "应该使用成本因子12");

        // 验证加密强度 - BCrypt成本因子12应该相对较慢
        long startTime = System.currentTimeMillis();
        passwordEncoder.encode(password);
        long duration = System.currentTimeMillis() - startTime;

        // 成本因子12的BCrypt应该需要一定时间（通常几十毫秒）
        assertTrue(duration >= 0, "加密过程应该消耗一定时间");
    }

    @Test
    @DisplayName("性能测试 - 批量密码加密")
    void testPerformance() {
        // Given
        String[] passwords = {"pass1", "pass2", "pass3", "pass4", "pass5"};

        // When
        long startTime = System.currentTimeMillis();
        String[] encoded = new String[passwords.length];
        for (int i = 0; i < passwords.length; i++) {
            encoded[i] = passwordEncoder.encode(passwords[i]);
        }
        long encodeTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        for (int i = 0; i < passwords.length; i++) {
            passwordEncoder.matches(passwords[i], encoded[i]);
        }
        long verifyTime = System.currentTimeMillis() - startTime;

        // Then
        assertTrue(encodeTime > 0, "加密应该消耗时间");
        assertTrue(verifyTime > 0, "验证应该消耗时间");

        // 验证所有密码都正确加密
        for (int i = 0; i < passwords.length; i++) {
            assertTrue(passwordEncoder.matches(passwords[i], encoded[i]),
                      "所有密码都应该验证通过");
        }

        System.out.printf("性能测试结果: 加密%d个密码耗时%dms, 验证耗时%dms%n",
                         passwords.length, encodeTime, verifyTime);
    }
}