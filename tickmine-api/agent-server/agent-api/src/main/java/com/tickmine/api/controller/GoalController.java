package com.tickmine.api.controller;

import com.tickmine.api.dto.ConversationDto;
import com.tickmine.api.dto.CreateGoalRequest;
import com.tickmine.api.dto.DtoMapper;
import com.tickmine.api.dto.GoalResponseDto;
import com.tickmine.domain.model.ExecutionResult;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.api.security.AuthContext;
import com.tickmine.infra.service.GoalAgentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalAgentService goalAgentService;
    private final AuthContext authContext;

    @PostMapping
    public GoalResponseDto create(@RequestBody CreateGoalRequest request) {
        authContext.requireSameUser(request.userId());
        return DtoMapper.toDto(goalAgentService.createGoal(
                request.userId(), request.title(), request.description()));
    }

    @GetMapping("/{id}")
    public GoalResponseDto get(@PathVariable UUID id) {
        var detail = goalAgentService.getGoalDetail(id);
        authContext.requireSameUser(detail.goal().getUserId());
        return DtoMapper.toDto(detail);
    }

    @PostMapping("/{id}/plan")
    public PlanDsl replan(@PathVariable UUID id) {
        var detail = goalAgentService.getGoalDetail(id);
        authContext.requireSameUser(detail.goal().getUserId());
        return goalAgentService.regeneratePlan(id);
    }

    @PostMapping("/{id}/execute")
    public ExecutionResult execute(@PathVariable UUID id) {
        var detail = goalAgentService.getGoalDetail(id);
        authContext.requireSameUser(detail.goal().getUserId());
        return goalAgentService.executePlan(id);
    }

    @GetMapping("/{id}/conversation")
    public ConversationDto getConversation(@PathVariable UUID id) {
        var user = authContext.requireCurrentUser();
        return DtoMapper.toDto(goalAgentService.getConversation(user.userId(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        var user = authContext.requireCurrentUser();
        goalAgentService.deleteGoal(user.userId(), id);
    }
}
