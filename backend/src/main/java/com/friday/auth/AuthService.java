package com.friday.auth;

import com.friday.auth.dto.AuthResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwt;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       JwtService jwt, PasswordEncoder encoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.jwt = jwt;
        this.encoder = encoder;
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        User user = users.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return new AuthResponse(jwt.generateAccessToken(username));
    }

    @Transactional
    public String issueRefreshToken(String username) {
        String raw = UUID.randomUUID().toString();
        String hash = sha256(raw);

        RefreshToken rt = new RefreshToken();
        rt.setUser(users.findByUsername(username).orElseThrow());
        rt.setTokenHash(hash);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokens.save(rt);

        return raw;
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken rt = refreshTokens.findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }
        String username = rt.getUser().getUsername();
        return new AuthResponse(jwt.generateAccessToken(username));
    }

    @Transactional
    public void logout(String username) {
        users.findByUsername(username)
            .ifPresent(u -> refreshTokens.deleteAllByUserId(u.getId()));
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(
                md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
