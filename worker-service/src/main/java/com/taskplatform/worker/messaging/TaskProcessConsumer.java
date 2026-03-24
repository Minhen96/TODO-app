package com.taskplatform.worker.messaging;

import com.taskplatform.schemas.TaskEvent;
import com.taskplatform.worker.service.TaskProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskProcessConsumer {

    private final TaskProcessor taskProcessor;

    @KafkaListener(topics = "${kafka.topics.task-process}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTaskProcess(ConsumerRecord<String, TaskEvent> record) {
        TaskEvent event = record.value();

        log.info("Received task.process event",
                kv("taskId", event.getTaskId()),
                kv("partition", record.partition()),
                kv("offset", record.offset()));

        taskProcessor.processTask(event);
    }
}
