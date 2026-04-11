package com.friday.auth;

import com.friday.auth.dto.AuthResponse;
import com.friday.auth.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService,
                          @Value("${app.cookie.secure:true}") boolean cookieSecure) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req,
                                               HttpServletResponse response) {
        AuthService.AuthTokens tokens = authService.authenticate(req.username(), req.password());
        addRefreshCookie(response, tokens.rawRefreshToken());
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = Arrays.stream(request.getCookies() == null ? new jakarta.servlet.http.Cookie[0] : request.getCookies())
            .filter(c -> "refreshToken".equals(c.getName()))
            .map(jakarta.servlet.http.Cookie::getValue)
            .findFirst()
            .orElseThrow(() -> new BadCredentialsException("No refresh token"));
        AuthService.AuthTokens tokens = authService.refresh(token);
        addRefreshCookie(response, tokens.rawRefreshToken());
        return ResponseEntity.ok(new AuthResponse(tokens.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails user,
                                        HttpServletResponse response) {
        if (user == null) return ResponseEntity.status(401).build();
        authService.logout(user.getUsername());
        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .path("/auth")
            .maxAge(Duration.ofSeconds(0))
            .sameSite("Strict")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.noContent().build();
    }

    private void addRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/auth")
            .maxAge(Duration.ofDays(7))
            .sameSite("Strict")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
