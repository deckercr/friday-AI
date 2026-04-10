package com.friday.auth;

import com.friday.auth.dto.AuthResponse;
import com.friday.auth.dto.LoginRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req,
                                               HttpServletResponse response) {
        AuthResponse auth = authService.login(req.username(), req.password());
        String refreshToken = authService.issueRefreshToken(req.username());
        addRefreshCookie(response, refreshToken);
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String token = Arrays.stream(request.getCookies() == null ? new Cookie[0] : request.getCookies())
            .filter(c -> "refreshToken".equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElseThrow(() -> new BadCredentialsException("No refresh token"));
        return ResponseEntity.ok(authService.refresh(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails user,
                                        HttpServletResponse response) {
        authService.logout(user.getUsername());
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/auth");
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

    private void addRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set true in production with HTTPS
        cookie.setPath("/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);
    }
}
