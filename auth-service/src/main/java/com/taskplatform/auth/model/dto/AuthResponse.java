package com.taskplatform.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UserInfo user
) {
    public record UserInfo(
            String id,
            String username,
            String email,
            String role
    ) {}

    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                Instant.now().plusMillis(expiresIn),
                user
        );
    }
}
