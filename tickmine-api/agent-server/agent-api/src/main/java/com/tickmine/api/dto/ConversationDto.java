package com.tickmine.api.dto;

import com.tickmine.domain.model.PlanDsl;
import java.util.List;
import java.util.UUID;

public record ConversationDto(
        UUID goalId,
        String phase,
        PlanDsl latestPlan,
        List<ChatMessageDto> messages) {}
