package com.tickmine.infra.service;

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
        List<ToolCallRecord> toolCalls) {

    public sealed interface ReplySource permits LlmReplySource, StaticReplySource {}

    public record LlmReplySource(String userId, String systemPrompt, String userPrompt)
            implements ReplySource {}

    public record StaticReplySource(String text) implements ReplySource {}
}
