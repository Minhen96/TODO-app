package com.taskplatform.auth.model.dto;

import java.util.List;

public record TokenValidationResponse(
        boolean valid,
        String userId,
        String username,
        List<String> roles,
        String error
) {
    public static TokenValidationResponse valid(String userId, String username, List<String> roles) {
        return new TokenValidationResponse(true, userId, username, roles, null);
    }

    public static TokenValidationResponse invalid(String error) {
        return new TokenValidationResponse(false, null, null, null, error);
    }
}
