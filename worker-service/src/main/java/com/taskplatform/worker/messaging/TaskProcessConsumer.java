package com.taskplatform.worker.messaging;

import com.taskplatform.schemas.TaskEvent;
import com.taskplatform.worker.service.TaskProcessor;
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
public class TaskProcessConsumer {

    private final TaskProcessor taskProcessor;
    private final StringRedisTemplate redisTemplate;

    private static final String PROCESSED_KEY_PREFIX = "processed:worker:";
    private static final Duration DEDUP_TTL = Duration.ofDays(7);

    @KafkaListener(topics = "${kafka.topics.task-process}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTaskProcess(ConsumerRecord<String, TaskEvent> record) {
        TaskEvent event = record.value();
        String dedupKey = PROCESSED_KEY_PREFIX + event.getEventId();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.warn("Duplicate event detected, skipping",
                    kv("eventId", event.getEventId()),
                    kv("taskId", event.getTaskId()));
            return;
        }

        log.info("Received task.process event",
                kv("taskId", event.getTaskId()),
                kv("partition", record.partition()),
                kv("offset", record.offset()));

        taskProcessor.processTask(event);
        redisTemplate.opsForValue().set(dedupKey, "1", DEDUP_TTL);
    }
}
