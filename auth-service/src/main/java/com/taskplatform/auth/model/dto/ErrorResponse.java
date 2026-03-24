package com.taskplatform.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String traceId,
        List<FieldError> fieldErrors
) {
    public record FieldError(
            String field,
            String message
    ) {}

    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(code, message, Instant.now(), traceId, null);
    }

    public static ErrorResponse withFieldErrors(String code, String message, String traceId, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, Instant.now(), traceId, fieldErrors);
    }
}
