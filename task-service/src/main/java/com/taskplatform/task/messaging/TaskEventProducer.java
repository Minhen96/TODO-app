package com.taskplatform.task.messaging;

import com.taskplatform.schemas.*;
import com.taskplatform.task.config.KafkaConfig;
import com.taskplatform.task.model.entity.Task;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventProducer {

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;
    private final KafkaConfig kafkaConfig;
    private final Tracer tracer;

    public void sendTaskCreatedEvent(Task task) {
        TaskEvent event = buildTaskEvent(task, EventType.CREATED);

        String topic = kafkaConfig.getTaskCreated();
        String key = task.getId().toString();

        log.info("Sending task created event",
                kv("taskId", task.getId()),
                kv("topic", topic));

        CompletableFuture<SendResult<String, TaskEvent>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send task created event",
                        kv("taskId", task.getId()),
                        kv("error", ex.getMessage()));
            } else {
                log.debug("Task created event sent successfully",
                        kv("taskId", task.getId()),
                        kv("partition", result.getRecordMetadata().partition()),
                        kv("offset", result.getRecordMetadata().offset()));
            }
        });
    }

    private TaskEvent buildTaskEvent(Task task, EventType eventType) {
        String traceId = Optional.ofNullable(tracer.currentSpan())
                .map(span -> span.context().traceId())
                .orElse(null);

        String spanId = Optional.ofNullable(tracer.currentSpan())
                .map(span -> span.context().spanId())
                .orElse(null);

        return TaskEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(eventType)
                .setEventTimestamp(Instant.now().toEpochMilli())
                .setTaskId(task.getId().toString())
                .setTaskName(task.getName())
                .setTaskType(mapTaskType(task.getType()))
                .setTaskStatus(mapTaskStatus(task.getStatus()))
                .setTaskPriority(mapTaskPriority(task.getPriority()))
                .setDescription(task.getDescription())
                .setPayload(task.getPayload())
                .setResult(task.getResult())
                .setErrorMessage(task.getErrorMessage())
                .setRetryCount(task.getRetryCount())
                .setMaxRetries(task.getMaxRetries())
                .setScheduledAt(task.getScheduledAt() != null ? task.getScheduledAt().toEpochMilli() : null)
                .setStartedAt(task.getStartedAt() != null ? task.getStartedAt().toEpochMilli() : null)
                .setCompletedAt(task.getCompletedAt() != null ? task.getCompletedAt().toEpochMilli() : null)
                .setTraceId(traceId)
                .setSpanId(spanId)
                .setSourceService("task-service")
                .setMetadata(new HashMap<>())
                .build();
    }

    private TaskType mapTaskType(Task.TaskType type) {
        return TaskType.valueOf(type.name());
    }

    private TaskStatus mapTaskStatus(Task.TaskStatus status) {
        return TaskStatus.valueOf(status.name());
    }

    private TaskPriority mapTaskPriority(Task.TaskPriority priority) {
        return TaskPriority.valueOf(priority.name());
    }
}
