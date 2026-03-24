package com.taskplatform.task.model.dto;

import com.taskplatform.task.model.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record CreateTaskRequest(
        @NotBlank(message = "Task name is required")
        @Size(max = 255, message = "Task name must be less than 255 characters")
        String name,

        @Size(max = 5000, message = "Description must be less than 5000 characters")
        String description,

        @NotNull(message = "Task type is required")
        Task.TaskType type,

        Task.TaskPriority priority,

        Map<String, Object> payload,

        Instant scheduledAt
) {
    public CreateTaskRequest {
        if (priority == null) {
            priority = Task.TaskPriority.NORMAL;
        }
    }
}
