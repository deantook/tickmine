package com.tickmine.api.dto;

import com.tickmine.domain.model.PlanDsl;
import java.util.UUID;

public record GoalResponseDto(
        UUID id,
        String userId,
        String title,
        String description,
        String phase,
        String status,
        PlanDsl latestPlan,
        String ticktickProjectId) {}
