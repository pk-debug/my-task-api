package com.example.taskapi.api;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                "task-api",
                Instant.now().toString(),
                Math.max(0, java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000));
    }
}