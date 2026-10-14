package com.enterprise.knowledge.dto;

public record AuthResponse(
        String username,
        String fullName,
        String role,
        String message) {
}
