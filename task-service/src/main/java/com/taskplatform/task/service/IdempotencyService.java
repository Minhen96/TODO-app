package com.taskplatform.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskplatform.task.model.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idempotency:task:";

    public Optional<TaskResponse> get(String idempotencyKey) {
        String cached = redisTemplate.opsForValue().get(PREFIX + idempotencyKey);
        if (cached == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cached, TaskResponse.class));
        } catch (Exception e) {
            log.warn("Failed to deserialize cached idempotency response",
                    kv("idempotencyKey", idempotencyKey));
            return Optional.empty();
        }
    }

    public void store(String idempotencyKey, TaskResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(PREFIX + idempotencyKey, json, TTL);
        } catch (Exception e) {
            log.warn("Failed to store idempotency response",
                    kv("idempotencyKey", idempotencyKey));
        }
    }
}
