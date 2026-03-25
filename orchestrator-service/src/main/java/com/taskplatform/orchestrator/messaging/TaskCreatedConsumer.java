package com.taskplatform.orchestrator.messaging;

import com.taskplatform.orchestrator.service.OrchestrationService;
import com.taskplatform.schemas.TaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskCreatedConsumer {

    private final OrchestrationService orchestrationService;
    private final StringRedisTemplate redisTemplate;

    private static final String PROCESSED_KEY_PREFIX = "processed:orchestrator:";
    private static final Duration DEDUP_TTL = Duration.ofDays(7);

    @KafkaListener(topics = "${kafka.topics.task-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTaskCreated(ConsumerRecord<String, TaskEvent> record) {
        TaskEvent event = record.value();
        String dedupKey = PROCESSED_KEY_PREFIX + event.getEventId();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.warn("Duplicate event detected, skipping",
                    kv("eventId", event.getEventId()),
                    kv("taskId", event.getTaskId()));
            return;
        }

        log.info("Received task.created event",
                kv("taskId", event.getTaskId()),
                kv("taskType", event.getTaskType()),
                kv("partition", record.partition()),
                kv("offset", record.offset()));

        try {
            orchestrationService.processTaskCreated(event);
            redisTemplate.opsForValue().set(dedupKey, "1", DEDUP_TTL);
        } catch (Exception e) {
            log.error("Failed to process task.created event",
                    kv("taskId", event.getTaskId()),
                    kv("error", e.getMessage()), e);
            throw e;
        }
    }
}
