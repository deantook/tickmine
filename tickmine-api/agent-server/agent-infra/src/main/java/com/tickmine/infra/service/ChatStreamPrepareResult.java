package com.tickmine.infra.service;

import com.tickmine.domain.model.AgentRunRequest;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.ToolCallRecord;
import java.util.List;

public record ChatStreamPrepareResult(
        Goal goal,
        String userId,
        ReplySource replySource,
        GoalPhase responsePhase,
        PlanDsl plan,
        List<String> missingFields,
        List<ToolCallRecord> toolCalls,
        AgentRunRequest agentRequest) {

    public ChatStreamPrepareResult(
            Goal goal,
            String userId,
            ReplySource replySource,
            GoalPhase responsePhase,
            PlanDsl plan,
            List<String> missingFields,
            List<ToolCallRecord> toolCalls) {
        this(goal, userId, replySource, responsePhase, plan, missingFields, toolCalls, null);
    }

    public sealed interface ReplySource permits AgentStreamReplySource, LlmReplySource, StaticReplySource {}

    public record AgentStreamReplySource(AgentRunRequest request) implements ReplySource {}

    public record LlmReplySource(String userId, String systemPrompt, String userPrompt) implements ReplySource {}

    public record StaticReplySource(String text) implements ReplySource {}
}
