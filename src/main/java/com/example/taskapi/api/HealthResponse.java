package com.example.taskapi.api;

public record HealthResponse(String status, String application, String timestamp, long uptimeSeconds) {
}