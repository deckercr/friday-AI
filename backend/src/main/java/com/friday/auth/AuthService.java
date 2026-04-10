package com.friday.auth;

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

    public record AuthTokens(String accessToken, String rawRefreshToken) {}

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
    public AuthTokens authenticate(String username, String password) {
        User user = users.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        // Clear old refresh tokens for this user
        refreshTokens.deleteAllByUserId(user.getId());

        // Issue new opaque refresh token
        String raw = UUID.randomUUID().toString();
        String hash = sha256(raw);
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokens.save(rt);

        return new AuthTokens(jwt.generateAccessToken(username), raw);
    }

    @Transactional
    public AuthTokens refresh(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken rt = refreshTokens.findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            refreshTokens.delete(rt);
            throw new BadCredentialsException("Refresh token expired");
        }
        String username = rt.getUser().getUsername();

        // Rotate: delete consumed token, issue new one
        refreshTokens.delete(rt);
        String newRaw = UUID.randomUUID().toString();
        String newHash = sha256(newRaw);
        RefreshToken newRt = new RefreshToken();
        newRt.setUser(rt.getUser());
        newRt.setTokenHash(newHash);
        newRt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokens.save(newRt);

        return new AuthTokens(jwt.generateAccessToken(username), newRaw);
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
