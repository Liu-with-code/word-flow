package com.wordflow.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 工具类单元测试。
 *
 * 覆盖：令牌生成/解析、过期令牌拒绝。
 */
class JwtUtilTest {

    private static final String SECRET = "wordflow-dev-secret-change-me-0123456789abcdef";

    private JwtUtil newUtil(long expireHours) {
        return new JwtUtil(SECRET, expireHours);
    }

    @Test
    void shouldParseUserId_whenTokenGenerated() {
        JwtUtil jwtUtil = newUtil(1);
        String token = jwtUtil.generateToken(42L);

        Long userId = jwtUtil.parseUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void shouldRejectExpiredToken_whenExpireHoursIsNegative() {
        JwtUtil jwtUtil = newUtil(-1);
        String token = jwtUtil.generateToken(42L);

        assertThatThrownBy(() -> jwtUtil.parseUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
