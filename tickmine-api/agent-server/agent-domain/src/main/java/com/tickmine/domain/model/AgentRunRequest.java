package com.tickmine.domain.model;

import java.util.List;
import java.util.function.BiConsumer;

public record AgentRunRequest(
        String userId,
        Goal goal,
        List<ChatMessage> history,
        AgentRunOutcome outcome,
        BiConsumer<Goal, PlanDsl> onPlanProposed) {

    public AgentRunRequest {
        history = history != null ? history : List.of();
    }
}
