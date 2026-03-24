package com.taskplatform.notification.messaging;

import com.taskplatform.notification.service.NotificationService;
import com.taskplatform.schemas.TaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${kafka.topics.task-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTaskCompleted(ConsumerRecord<String, TaskEvent> record) {
        TaskEvent event = record.value();

        log.info("Received task.completed event",
                kv("taskId", event.getTaskId()),
                kv("partition", record.partition()),
                kv("offset", record.offset()));

        notificationService.notifyTaskCompleted(event);
    }

    @KafkaListener(topics = "${kafka.topics.task-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTaskFailed(ConsumerRecord<String, TaskEvent> record) {
        TaskEvent event = record.value();

        log.info("Received task.failed event",
                kv("taskId", event.getTaskId()),
                kv("partition", record.partition()),
                kv("offset", record.offset()));

        notificationService.notifyTaskFailed(event);
    }
}
