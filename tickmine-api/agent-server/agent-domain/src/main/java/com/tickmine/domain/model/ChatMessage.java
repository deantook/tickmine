package com.tickmine.domain.model;

import java.time.Instant;

public record ChatMessage(String role, String content, Instant timestamp) {
}
