package com.friday.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-expiry-ms}") long accessExpiryMs,
        @Value("${app.jwt.refresh-expiry-ms}") long refreshExpiryMs
    ) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessExpiryMs = accessExpiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    public String generateAccessToken(String username) {
        return buildToken(username, accessExpiryMs);
    }

    public String generateRefreshToken(String username) {
        return buildToken(username, refreshExpiryMs);
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, String username) {
        try {
            return parseClaims(token).getSubject().equals(username);
        } catch (JwtException e) {
            return false;
        }
    }

    private String buildToken(String subject, long expiryMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(subject)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expiryMs))
            .signWith(key)
            .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
    }
}
