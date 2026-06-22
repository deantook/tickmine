package com.tickmine.api.dto;

import java.time.Instant;

public record ChatMessageDto(String role, String content, Instant timestamp) {}
