package com.taskplatform.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        return ResponseEntity.status(503).body(Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "message", "Auth service is temporarily unavailable. Please try again later.",
                "timestamp", Instant.now().toString()
        ));
    }

    @RequestMapping("/fallback/task")
    public ResponseEntity<Map<String, Object>> taskFallback() {
        return ResponseEntity.status(503).body(Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "message", "Task service is temporarily unavailable. Please try again later.",
                "timestamp", Instant.now().toString()
        ));
    }

    @RequestMapping("/fallback/orchestrator")
    public ResponseEntity<Map<String, Object>> orchestratorFallback() {
        return ResponseEntity.status(503).body(Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "message", "Orchestrator service is temporarily unavailable. Please try again later.",
                "timestamp", Instant.now().toString()
        ));
    }
}
