package com.tickmine.api.dto;

public record AuthResponse(String accessToken, String userId, String email, String expiresAt) {}
