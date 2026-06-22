package com.tickmine.api.dto;

import java.util.UUID;

public record ChatRequest(String userId, String message, UUID goalId) {}
