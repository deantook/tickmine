package com.tickmine.api.dto;

import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.ToolCallRecord;
import java.util.List;
import java.util.UUID;

public record ChatResponseDto(
        UUID goalId,
        String phase,
        String reply,
        PlanDsl plan,
        List<String> missingFields,
        List<ToolCallRecord> toolCalls) {}
