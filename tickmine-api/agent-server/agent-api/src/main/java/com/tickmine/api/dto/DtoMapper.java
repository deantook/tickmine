package com.tickmine.api.dto;

import com.tickmine.domain.model.ChatResponse;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.QuotaStatus;
import com.tickmine.infra.service.GoalAgentService.GoalDetail;

public final class DtoMapper {

    private DtoMapper() {}

    public static ChatResponseDto toDto(ChatResponse response) {
        return new ChatResponseDto(
                response.goalId(),
                response.phase().name(),
                response.reply(),
                response.plan(),
                response.missingFields(),
                response.toolCalls());
    }

    public static GoalResponseDto toDto(Goal goal) {
        return new GoalResponseDto(
                goal.getId(),
                goal.getUserId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getPhase().name(),
                goal.getStatus().name(),
                null,
                goal.getTicktickProjectId());
    }

    public static GoalResponseDto toDto(GoalDetail detail) {
        Goal goal = detail.goal();
        return new GoalResponseDto(
                goal.getId(),
                goal.getUserId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getPhase().name(),
                goal.getStatus().name(),
                detail.latestPlan(),
                goal.getTicktickProjectId());
    }

    public static QuotaResponseDto toDto(QuotaStatus status) {
        return new QuotaResponseDto(
                status.tier().name(),
                status.dailyLimit(),
                status.used(),
                status.remaining());
    }
}
