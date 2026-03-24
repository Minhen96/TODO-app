package com.taskplatform.orchestrator.messaging;

import com.taskplatform.orchestrator.service.OrchestrationService;
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
public class TaskCreatedConsumer {

    private final OrchestrationService orchestrationService;

    @KafkaListener(topics = "${kafka.topics.task-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTaskCreated(ConsumerRecord<String, TaskEvent> record) {
        TaskEvent event = record.value();

        log.info("Received task.created event",
                kv("taskId", event.getTaskId()),
                kv("taskType", event.getTaskType()),
                kv("partition", record.partition()),
                kv("offset", record.offset()));

        try {
            orchestrationService.processTaskCreated(event);
        } catch (Exception e) {
            log.error("Failed to process task.created event",
                    kv("taskId", event.getTaskId()),
                    kv("error", e.getMessage()), e);
            throw e;
        }
    }
}
