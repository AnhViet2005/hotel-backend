package com.hotel.backend.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        try {
            Integer ok = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Map.of("status", "ok", "db", ok == 1 ? "connected" : "unknown");
        } catch (Exception ex) {
            return Map.of("status", "error", "db", "down", "message", ex.getMessage());
        }
    }
}
