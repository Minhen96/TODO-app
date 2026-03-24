package com.taskplatform.task.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskplatform.task.model.entity.Task;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskResponse(
        UUID id,
        String name,
        String description,
        Task.TaskType type,
        Task.TaskPriority priority,
        Task.TaskStatus status,
        Object payload,
        Object result,
        String errorMessage,
        int retryCount,
        int maxRetries,
        Instant createdAt,
        Instant updatedAt,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt,
        String traceId
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getType(),
                task.getPriority(),
                task.getStatus(),
                task.getPayload(),
                task.getResult(),
                task.getErrorMessage(),
                task.getRetryCount(),
                task.getMaxRetries(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getScheduledAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getTraceId()
        );
    }
}
