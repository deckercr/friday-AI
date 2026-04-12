package com.friday.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // 256-bit base64 secret for testing
        jwtService = new JwtService(
            "dGVzdC1zZWNyZXQtdGhhdC1pcy1leGFjdGx5LTMyLWNoYXJzLWxvbmchIQ==",
            900_000L,
            604_800_000L
        );
    }

    @Test
    void generateToken_containsUsername() {
        String token = jwtService.generateAccessToken("alice");
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isValid_trueForFreshToken() {
        String token = jwtService.generateAccessToken("alice");
        assertThat(jwtService.isValid(token, "alice")).isTrue();
    }

    @Test
    void isValid_falseForWrongUsername() {
        String token = jwtService.generateAccessToken("alice");
        assertThat(jwtService.isValid(token, "bob")).isFalse();
    }

    @Test
    void extractUsername_throwsForGarbageToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("not.a.token"))
            .isInstanceOf(Exception.class);
    }
}
