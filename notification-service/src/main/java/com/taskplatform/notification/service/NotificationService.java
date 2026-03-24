package com.taskplatform.notification.service;

import com.taskplatform.schemas.TaskEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
public class NotificationService {

    private final Counter notificationsSentCounter;
    private final Counter notificationsFailedCounter;

    public NotificationService(MeterRegistry meterRegistry) {
        this.notificationsSentCounter = Counter.builder("notification.sent.total")
                .description("Total notifications sent")
                .register(meterRegistry);
        this.notificationsFailedCounter = Counter.builder("notification.failed.total")
                .description("Total notifications failed")
                .register(meterRegistry);
    }

    public void notifyTaskCompleted(TaskEvent event) {
        log.info("Sending task completion notification",
                kv("taskId", event.getTaskId()),
                kv("taskName", event.getTaskName()),
                kv("taskType", event.getTaskType()));

        // In a real implementation, this would:
        // - Send email notification
        // - Send push notification
        // - Send webhook
        // - Update external systems

        // Simulate notification sending
        sendNotification("TASK_COMPLETED",
                String.format("Task '%s' completed successfully", event.getTaskName()),
                event);

        notificationsSentCounter.increment();
    }

    public void notifyTaskFailed(TaskEvent event) {
        log.warn("Sending task failure notification",
                kv("taskId", event.getTaskId()),
                kv("taskName", event.getTaskName()),
                kv("errorMessage", event.getErrorMessage()));

        // Send failure notification
        sendNotification("TASK_FAILED",
                String.format("Task '%s' failed: %s", event.getTaskName(), event.getErrorMessage()),
                event);

        notificationsFailedCounter.increment();
    }

    private void sendNotification(String type, String message, TaskEvent event) {
        // Simulated notification - in production this would integrate with:
        // - Email service (SendGrid, SES)
        // - Push notification service (Firebase, OneSignal)
        // - Slack/Teams webhooks
        // - SMS service (Twilio)

        log.info("Notification sent",
                kv("type", type),
                kv("message", message),
                kv("taskId", event.getTaskId()),
                kv("traceId", event.getTraceId()));
    }
}
