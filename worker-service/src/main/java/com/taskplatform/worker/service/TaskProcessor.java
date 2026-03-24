package com.taskplatform.worker.service;

import com.taskplatform.schemas.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class TaskProcessor {

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;
    private final Counter tasksCompletedCounter;
    private final Counter tasksFailedCounter;
    private final Counter tasksDlqCounter;
    private final Timer processingTimer;
    private final Random random = new Random();

    @Value("${kafka.topics.task-completed}")
    private String taskCompletedTopic;

    @Value("${kafka.topics.task-failed}")
    private String taskFailedTopic;

    @Value("${kafka.topics.task-dlq}")
    private String taskDlqTopic;

    @Value("${worker.retry.max-attempts}")
    private int maxRetryAttempts;

    public TaskProcessor(KafkaTemplate<String, TaskEvent> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.tasksCompletedCounter = Counter.builder("task.completed.total")
                .description("Total tasks completed successfully")
                .register(meterRegistry);
        this.tasksFailedCounter = Counter.builder("task.failed.total")
                .description("Total tasks failed")
                .register(meterRegistry);
        this.tasksDlqCounter = Counter.builder("task.dlq.total")
                .description("Total tasks sent to DLQ")
                .register(meterRegistry);
        this.processingTimer = Timer.builder("task.processing.duration")
                .description("Task processing duration")
                .register(meterRegistry);
    }

    public void processTask(TaskEvent event) {
        log.info("Processing task",
                kv("taskId", event.getTaskId()),
                kv("taskType", event.getTaskType()),
                kv("retryCount", event.getRetryCount()));

        Instant startTime = Instant.now();

        try {
            // Simulate task processing
            executeTask(event);

            Duration duration = Duration.between(startTime, Instant.now());
            processingTimer.record(duration);

            // Send completed event
            sendCompletedEvent(event);
            tasksCompletedCounter.increment();

            log.info("Task completed successfully",
                    kv("taskId", event.getTaskId()),
                    kv("durationMs", duration.toMillis()));

        } catch (Exception e) {
            log.error("Task processing failed",
                    kv("taskId", event.getTaskId()),
                    kv("error", e.getMessage()));

            handleFailure(event, e);
        }
    }

    private void executeTask(TaskEvent event) throws Exception {
        // Simulate processing based on task type
        TaskType type = event.getTaskType();

        switch (type) {
            case COMPUTE -> simulateCompute();
            case IO -> simulateIO();
            case NOTIFICATION -> simulateNotification();
            case BATCH -> simulateBatch();
            default -> simulateDefault();
        }

        // Simulate random failures (10% failure rate for demo)
        if (random.nextInt(10) == 0) {
            throw new RuntimeException("Simulated processing failure");
        }
    }

    private void simulateCompute() throws InterruptedException {
        Thread.sleep(random.nextInt(500) + 100);
    }

    private void simulateIO() throws InterruptedException {
        Thread.sleep(random.nextInt(1000) + 200);
    }

    private void simulateNotification() throws InterruptedException {
        Thread.sleep(random.nextInt(200) + 50);
    }

    private void simulateBatch() throws InterruptedException {
        Thread.sleep(random.nextInt(2000) + 500);
    }

    private void simulateDefault() throws InterruptedException {
        Thread.sleep(random.nextInt(300) + 100);
    }

    private void handleFailure(TaskEvent event, Exception e) {
        int currentRetry = event.getRetryCount();

        if (currentRetry < maxRetryAttempts) {
            // Send retry event
            log.warn("Retrying task",
                    kv("taskId", event.getTaskId()),
                    kv("attempt", currentRetry + 1),
                    kv("maxAttempts", maxRetryAttempts));

            sendRetryEvent(event, e.getMessage());
            tasksFailedCounter.increment();
        } else {
            // Send to DLQ
            log.error("Max retries exceeded, sending to DLQ",
                    kv("taskId", event.getTaskId()));

            sendToDlq(event, e.getMessage());
            tasksDlqCounter.increment();
        }
    }

    private void sendCompletedEvent(TaskEvent event) {
        TaskEvent completedEvent = TaskEvent.newBuilder(event)
                .setEventId(UUID.randomUUID().toString())
                .setEventType(EventType.COMPLETED)
                .setEventTimestamp(Instant.now().toEpochMilli())
                .setTaskStatus(TaskStatus.COMPLETED)
                .setCompletedAt(Instant.now().toEpochMilli())
                .setResult("{\"status\": \"success\"}")
                .setSourceService("worker-service")
                .build();

        kafkaTemplate.send(taskCompletedTopic, event.getTaskId().toString(), completedEvent);
    }

    private void sendRetryEvent(TaskEvent event, String errorMessage) {
        TaskEvent retryEvent = TaskEvent.newBuilder(event)
                .setEventId(UUID.randomUUID().toString())
                .setEventType(EventType.RETRYING)
                .setEventTimestamp(Instant.now().toEpochMilli())
                .setRetryCount(event.getRetryCount() + 1)
                .setErrorMessage(errorMessage)
                .setSourceService("worker-service")
                .build();

        kafkaTemplate.send(taskFailedTopic, event.getTaskId().toString(), retryEvent);
    }

    private void sendToDlq(TaskEvent event, String errorMessage) {
        TaskEvent dlqEvent = TaskEvent.newBuilder(event)
                .setEventId(UUID.randomUUID().toString())
                .setEventType(EventType.DEAD_LETTER)
                .setEventTimestamp(Instant.now().toEpochMilli())
                .setTaskStatus(TaskStatus.DEAD_LETTER)
                .setErrorMessage("Max retries exceeded: " + errorMessage)
                .setErrorCode("MAX_RETRIES_EXCEEDED")
                .setSourceService("worker-service")
                .build();

        kafkaTemplate.send(taskDlqTopic, event.getTaskId().toString(), dlqEvent);
    }
}
