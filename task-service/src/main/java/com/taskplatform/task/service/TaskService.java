package com.taskplatform.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskplatform.task.exception.TaskNotFoundException;
import com.taskplatform.task.messaging.TaskEventProducer;
import com.taskplatform.task.model.dto.CreateTaskRequest;
import com.taskplatform.task.model.dto.TaskResponse;
import com.taskplatform.task.model.entity.Task;
import com.taskplatform.task.repository.TaskRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskEventProducer taskEventProducer;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final Counter taskCreatedCounter;

    public TaskService(TaskRepository taskRepository,
                       TaskEventProducer taskEventProducer,
                       ObjectMapper objectMapper,
                       Tracer tracer,
                       MeterRegistry meterRegistry) {
        this.taskRepository = taskRepository;
        this.taskEventProducer = taskEventProducer;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.taskCreatedCounter = Counter.builder("task.created.total")
                .description("Total number of tasks created")
                .register(meterRegistry);
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, UUID userId) {
        log.info("Creating task",
                kv("name", request.name()),
                kv("type", request.type()),
                kv("userId", userId));

        String traceId = Optional.ofNullable(tracer.currentSpan())
                .map(span -> span.context().traceId())
                .orElse(null);

        String spanId = Optional.ofNullable(tracer.currentSpan())
                .map(span -> span.context().spanId())
                .orElse(null);

        Task task = Task.builder()
                .userId(userId)
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .priority(request.priority())
                .status(Task.TaskStatus.PENDING)
                .payload(serializePayload(request.payload()))
                .scheduledAt(request.scheduledAt())
                .traceId(traceId)
                .spanId(spanId)
                .build();

        task = taskRepository.save(task);

        log.info("Task created",
                kv("taskId", task.getId()),
                kv("type", task.getType()),
                kv("priority", task.getPriority()));

        // Publish event to Kafka
        taskEventProducer.sendTaskCreatedEvent(task);

        // Increment counter
        taskCreatedCounter.increment();

        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(UUID userId, Pageable pageable) {
        return taskRepository.findByUserId(userId, pageable)
                .map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByStatus(UUID userId, Task.TaskStatus status, Pageable pageable) {
        return taskRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public long countTasks(UUID userId) {
        return taskRepository.countByUserId(userId);
    }

    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize payload", e);
            return null;
        }
    }
}
