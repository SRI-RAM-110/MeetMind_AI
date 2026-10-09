package com.meetmind.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final MongoTemplate mongoTemplate;

    public HealthController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "MeetMind AI Spring Boot Backend");
        status.put("timestamp", Instant.now().toString());

        boolean mongoHealthy = false;
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            mongoHealthy = true;
        } catch (Exception ignored) {
        }
        status.put("database", mongoHealthy ? "CONNECTED" : "DISCONNECTED");

        return ResponseEntity.ok(status);
    }
}
