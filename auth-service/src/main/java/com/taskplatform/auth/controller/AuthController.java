package com.taskplatform.auth.controller;

import com.taskplatform.auth.model.dto.*;
import com.taskplatform.auth.service.AuthService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Timed(value = "auth.register", description = "Time taken to register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Timed(value = "auth.login", description = "Time taken to authenticate a user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Timed(value = "auth.refresh", description = "Time taken to refresh a token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    @Timed(value = "auth.validate", description = "Time taken to validate a token")
    public ResponseEntity<TokenValidationResponse> validate(@RequestParam String token) {
        TokenValidationResponse response = authService.validateToken(new TokenValidationRequest(token));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @Timed(value = "auth.validate", description = "Time taken to validate a token")
    public ResponseEntity<TokenValidationResponse> validatePost(@Valid @RequestBody TokenValidationRequest request) {
        TokenValidationResponse response = authService.validateToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Timed(value = "auth.logout", description = "Time taken to logout a user")
    public ResponseEntity<Void> logout(Principal principal) {
        if (principal != null) {
            authService.logout(principal.getName());
        }
        return ResponseEntity.noContent().build();
    }
}
