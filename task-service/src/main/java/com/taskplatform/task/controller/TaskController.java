package com.taskplatform.task.controller;

import com.taskplatform.task.model.dto.CreateTaskRequest;
import com.taskplatform.task.model.dto.TaskResponse;
import com.taskplatform.task.model.entity.Task;
import com.taskplatform.task.service.TaskService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Timed(value = "task.create", description = "Time taken to create a task")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @RequestHeader("X-User-Id") String userId) {

        TaskResponse response = taskService.createTask(request, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{taskId}")
    @Timed(value = "task.get", description = "Time taken to get a task")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable UUID taskId,
            @RequestHeader("X-User-Id") String userId) {

        TaskResponse response = taskService.getTask(taskId, UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Timed(value = "task.list", description = "Time taken to list tasks")
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Task.TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TaskResponse> response;
        if (status != null) {
            response = taskService.getTasksByStatus(UUID.fromString(userId), status, pageable);
        } else {
            response = taskService.getTasks(UUID.fromString(userId), pageable);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countTasks(@RequestHeader("X-User-Id") String userId) {
        long count = taskService.countTasks(UUID.fromString(userId));
        return ResponseEntity.ok(count);
    }
}
