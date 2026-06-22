package com.tickmine.api.dto;

import java.time.Instant;
import java.util.UUID;

public record GoalSummaryDto(
        UUID id,
        String title,
        String preview,
        String phase,
        Instant updatedAt) {}
