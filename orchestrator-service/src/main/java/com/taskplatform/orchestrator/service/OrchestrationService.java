package com.taskplatform.orchestrator.service;

import com.taskplatform.schemas.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class OrchestrationService {

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;
    private final Counter tasksValidatedCounter;
    private final Counter tasksFailedValidationCounter;

    @Value("${kafka.topics.task-process}")
    private String taskProcessTopic;

    @Value("${kafka.topics.task-failed}")
    private String taskFailedTopic;

    public OrchestrationService(KafkaTemplate<String, TaskEvent> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.tasksValidatedCounter = Counter.builder("task.validated.total")
                .description("Total tasks validated successfully")
                .register(meterRegistry);
        this.tasksFailedValidationCounter = Counter.builder("task.validation.failed.total")
                .description("Total tasks failed validation")
                .register(meterRegistry);
    }

    public void processTaskCreated(TaskEvent event) {
        log.info("Processing task for orchestration",
                kv("taskId", event.getTaskId()),
                kv("taskType", event.getTaskType()));

        // Validate task
        ValidationResult validation = validateTask(event);

        if (!validation.valid()) {
            log.warn("Task validation failed",
                    kv("taskId", event.getTaskId()),
                    kv("reason", validation.reason()));

            sendFailedEvent(event, validation.reason());
            tasksFailedValidationCounter.increment();
            return;
        }

        // Enrich task
        TaskEvent enrichedEvent = enrichTask(event);

        // Send to processing queue
        log.info("Task validated, sending to process queue",
                kv("taskId", event.getTaskId()));

        kafkaTemplate.send(taskProcessTopic, event.getTaskId().toString(), enrichedEvent);
        tasksValidatedCounter.increment();
    }

    private ValidationResult validateTask(TaskEvent event) {
        // Validate required fields
        if (event.getTaskName() == null || event.getTaskName().isBlank()) {
            return new ValidationResult(false, "Task name is required");
        }

        if (event.getTaskType() == null) {
            return new ValidationResult(false, "Task type is required");
        }

        // Add more validation rules as needed
        return new ValidationResult(true, null);
    }

    private TaskEvent enrichTask(TaskEvent event) {
        return TaskEvent.newBuilder(event)
                .setEventId(UUID.randomUUID().toString())
                .setEventType(EventType.VALIDATED)
                .setEventTimestamp(Instant.now().toEpochMilli())
                .setTaskStatus(TaskStatus.VALIDATING)
                .setSourceService("orchestrator-service")
                .build();
    }

    private void sendFailedEvent(TaskEvent event, String reason) {
        TaskEvent failedEvent = TaskEvent.newBuilder(event)
                .setEventId(UUID.randomUUID().toString())
                .setEventType(EventType.FAILED)
                .setEventTimestamp(Instant.now().toEpochMilli())
                .setTaskStatus(TaskStatus.FAILED)
                .setErrorMessage(reason)
                .setErrorCode("VALIDATION_FAILED")
                .setSourceService("orchestrator-service")
                .build();

        kafkaTemplate.send(taskFailedTopic, event.getTaskId().toString(), failedEvent);
    }

    private record ValidationResult(boolean valid, String reason) {}
}
