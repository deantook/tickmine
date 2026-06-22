package com.tickmine.domain.model;

import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID goalId,
        GoalPhase phase,
        String reply,
        PlanDsl plan,
        List<String> missingFields,
        List<ToolCallRecord> toolCalls) {

    public static ChatResponse collecting(
            UUID goalId, String reply, List<String> missingFields, List<ToolCallRecord> toolCalls) {
        return new ChatResponse(goalId, GoalPhase.COLLECTING, reply, null, missingFields, toolCalls);
    }

    public static ChatResponse planReady(UUID goalId, String reply, PlanDsl plan, List<ToolCallRecord> toolCalls) {
        return new ChatResponse(goalId, GoalPhase.PLAN_READY, reply, plan, null, toolCalls);
    }

    public static ChatResponse chat(UUID goalId, String reply, List<ToolCallRecord> toolCalls) {
        return new ChatResponse(goalId, GoalPhase.CHAT, reply, null, null, toolCalls);
    }
}
