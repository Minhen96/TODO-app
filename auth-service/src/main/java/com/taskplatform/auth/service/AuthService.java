package com.taskplatform.auth.service;

import com.taskplatform.auth.exception.AuthenticationException;
import com.taskplatform.auth.exception.TokenException;
import com.taskplatform.auth.exception.UserAlreadyExistsException;
import com.taskplatform.auth.model.dto.*;
import com.taskplatform.auth.model.entity.RefreshToken;
import com.taskplatform.auth.model.entity.User;
import com.taskplatform.auth.repository.RefreshTokenRepository;
import com.taskplatform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user", kv("username", request.username()));

        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully", kv("userId", user.getId()), kv("username", user.getUsername()));

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt", kv("username", request.username()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt - invalid password", kv("username", request.username()));
            throw new AuthenticationException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new AuthenticationException("Account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            throw new AuthenticationException("Account is locked");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in successfully", kv("userId", user.getId()), kv("username", user.getUsername()));

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            // Token reuse detected - revoke all tokens for this user
            if (refreshToken.isRevoked()) {
                log.warn("Refresh token reuse detected",
                        kv("userId", refreshToken.getUser().getId()),
                        kv("tokenId", refreshToken.getId()));
                refreshTokenRepository.revokeAllByUser(refreshToken.getUser(), Instant.now());
            }
            throw new TokenException("Refresh token is invalid or expired");
        }

        // Rotate refresh token
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        log.info("Token refreshed", kv("userId", user.getId()));

        return generateAuthResponse(user);
    }

    public TokenValidationResponse validateToken(TokenValidationRequest request) {
        try {
            if (!jwtService.isTokenValid(request.token())) {
                return TokenValidationResponse.invalid("Token is invalid or expired");
            }

            String userId = jwtService.extractSubject(request.token());
            String role = jwtService.extractRole(request.token());

            User user = userRepository.findById(UUID.fromString(userId))
                    .orElse(null);

            if (user == null || !user.isEnabled()) {
                return TokenValidationResponse.invalid("User not found or disabled");
            }

            return TokenValidationResponse.valid(
                    userId,
                    user.getUsername(),
                    List.of("ROLE_" + role)
            );
        } catch (Exception e) {
            log.debug("Token validation failed", e);
            return TokenValidationResponse.invalid("Token validation failed: " + e.getMessage());
        }
    }

    @Transactional
    public void logout(String userId) {
        log.info("User logout", kv("userId", userId));

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AuthenticationException("User not found"));

        refreshTokenRepository.revokeAllByUser(user, Instant.now());
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Store hashed refresh token
        RefreshToken tokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .build();

        refreshTokenRepository.save(tokenEntity);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiration(),
                userInfo
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
