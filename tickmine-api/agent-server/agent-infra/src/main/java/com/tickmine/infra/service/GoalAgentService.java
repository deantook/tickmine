package com.tickmine.infra.service;

import com.tickmine.domain.exception.GoalNotFoundException;
import com.tickmine.domain.exception.InvalidGoalPhaseException;
import com.tickmine.domain.model.AgentRunOutcome;
import com.tickmine.domain.model.AgentRunRequest;
import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.ChatResponse;
import com.tickmine.domain.model.ExecutionResult;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.GoalStatus;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.ToolCallRecord;
import com.tickmine.domain.port.AgentOrchestrator;
import com.tickmine.domain.port.PlanExecutor;
import com.tickmine.domain.port.Planner;
import com.tickmine.domain.util.ToolCallCollector;
import com.tickmine.infra.persistence.entity.GoalEntity;
import com.tickmine.infra.persistence.entity.PlanEntity;
import com.tickmine.infra.persistence.entity.PlanExecutionEntity;
import com.tickmine.infra.persistence.entity.TickTickRefs;
import com.tickmine.infra.persistence.mapper.DomainMapper;
import com.tickmine.infra.persistence.repository.GoalRepository;
import com.tickmine.infra.persistence.repository.PlanExecutionRepository;
import com.tickmine.infra.persistence.repository.PlanRepository;
import com.tickmine.infra.service.ChatStreamPrepareResult.AgentStreamReplySource;
import com.tickmine.infra.service.ChatStreamPrepareResult.ReplySource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalAgentService {

    private final QuotaService quotaService;
    private final ConversationService conversationService;
    private final Planner planner;
    private final PlanExecutor planExecutor;
    private final UserService userService;
    private final GoalRepository goalRepository;
    private final PlanRepository planRepository;
    private final PlanExecutionRepository planExecutionRepository;
    private final DomainMapper mapper;
    private final AgentOrchestrator agentOrchestrator;

    @Transactional
    public ChatResponse handleChat(String userId, String message, UUID goalId) {
        ChatStreamPrepareResult prep = prepareStreamChat(userId, message, goalId);
        StringBuilder fullReply = new StringBuilder();
        emitStreamReply(prep, fullReply::append);
        return finalizeStreamChat(prep, fullReply.toString());
    }

    @Transactional
    public ChatStreamPrepareResult prepareStreamChat(String userId, String message, UUID goalId) {
        ToolCallCollector.begin();
        try {
            userService.findOrCreate(userId);
            quotaService.checkAndConsume(userId);

            Goal goal = resolveGoal(userId, goalId, message);
            conversationService.appendMessage(
                    userId,
                    goal.getId(),
                    new ChatMessage("user", message, Instant.now()));

            List<ChatMessage> history = conversationService.loadHistory(userId, goal.getId());
            AgentRunOutcome outcome = new AgentRunOutcome();
            AgentRunRequest agentRequest = new AgentRunRequest(
                    userId,
                    goal,
                    history,
                    outcome,
                    (activeGoal, plan) -> {
                        activeGoal.setPhase(GoalPhase.PLAN_READY);
                        activeGoal.setStatus(GoalStatus.ACTIVE);
                        outcome.setPlan(plan);
                        outcome.setResponsePhase(GoalPhase.PLAN_READY);
                    });

            return new ChatStreamPrepareResult(
                    goal,
                    userId,
                    new AgentStreamReplySource(agentRequest),
                    GoalPhase.CHAT,
                    null,
                    null,
                    List.of(),
                    agentRequest);
        } catch (RuntimeException exception) {
            ToolCallCollector.discard();
            throw exception;
        }
    }

    public void emitStreamReply(ChatStreamPrepareResult prep, Consumer<String> onDelta) {
        ReplySource source = prep.replySource();
        if (source instanceof AgentStreamReplySource agent) {
            agentOrchestrator.streamRun(agent.request(), onDelta);
            return;
        }
        if (source instanceof ChatStreamPrepareResult.LlmReplySource) {
            throw new IllegalStateException("Legacy LLM reply path is no longer supported");
        }
        if (source instanceof ChatStreamPrepareResult.StaticReplySource stat) {
            streamStaticText(stat.text(), onDelta);
        }
    }

    @Transactional
    public ChatResponse finalizeStreamChat(ChatStreamPrepareResult prep, String fullReply) {
        List<ToolCallRecord> toolCalls = ToolCallCollector.drain();
        AgentRunOutcome outcome = prep.agentRequest() != null
                ? prep.agentRequest().outcome()
                : new AgentRunOutcome();
        PlanDsl plan = outcome.plan();
        GoalPhase phase = plan != null ? GoalPhase.PLAN_READY : GoalPhase.CHAT;
        if (plan == null) {
            prep.goal().setPhase(GoalPhase.CHAT);
        } else {
            savePlan(prep.goal().getId(), plan);
        }

        saveGoal(prep.goal());
        conversationService.appendMessage(
                prep.userId(),
                prep.goal().getId(),
                new ChatMessage("assistant", fullReply, Instant.now()));

        return switch (phase) {
            case PLAN_READY -> ChatResponse.planReady(
                    prep.goal().getId(), fullReply, plan, toolCalls);
            default -> ChatResponse.chat(prep.goal().getId(), fullReply, toolCalls);
        };
    }

    private Goal resolveGoal(String userId, UUID goalId, String message) {
        if (goalId != null) {
            GoalEntity entity = goalRepository
                    .findById(goalId)
                    .orElseThrow(() -> new GoalNotFoundException(goalId));
            if (!entity.getUserId().equals(userId)) {
                throw new GoalNotFoundException(goalId);
            }
            return mapper.toDomain(entity);
        }
        return createGoalEntity(userId, truncate(message, 50), GoalPhase.CHAT);
    }

    private static void streamStaticText(String text, Consumer<String> onDelta) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int chunkSize = 8;
        for (int i = 0; i < text.length(); i += chunkSize) {
            onDelta.accept(text.substring(i, Math.min(i + chunkSize, text.length())));
        }
    }

    @Transactional
    public Goal createGoal(String userId, String title, String description) {
        userService.findOrCreate(userId);
        Instant now = Instant.now();
        GoalEntity entity = new GoalEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setStatus(GoalStatus.DRAFT);
        entity.setPhase(GoalPhase.COLLECTING);
        entity.setContext(new GoalContext());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        goalRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Transactional(readOnly = true)
    public GoalDetail getGoalDetail(UUID goalId) {
        GoalEntity entity = goalRepository
                .findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        PlanDsl latestPlan = planRepository
                .findFirstByGoalIdOrderByVersionDesc(goalId)
                .map(PlanEntity::getDsl)
                .orElse(null);
        return new GoalDetail(mapper.toDomain(entity), latestPlan);
    }

    @Transactional(readOnly = true)
    public List<GoalListItem> listGoals(String userId) {
        return goalRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(entity -> new GoalListItem(
                        mapper.toDomain(entity),
                        buildPreview(userId, entity.getId(), entity),
                        entity.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalConversation getConversation(String userId, UUID goalId) {
        GoalDetail detail = getGoalDetail(goalId);
        if (!detail.goal().getUserId().equals(userId)) {
            throw new GoalNotFoundException(goalId);
        }
        List<ChatMessage> messages = conversationService.loadHistory(userId, goalId);
        return new GoalConversation(detail.goal(), detail.latestPlan(), messages);
    }

    @Transactional
    public void deleteGoal(String userId, UUID goalId) {
        GoalEntity entity = goalRepository
                .findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (!entity.getUserId().equals(userId)) {
            throw new GoalNotFoundException(goalId);
        }
        removeGoalData(userId, goalId);
    }

    @Transactional
    public void deleteAllGoals(String userId) {
        List<GoalEntity> goals = goalRepository.findByUserId(userId);
        for (GoalEntity goal : goals) {
            removeGoalData(userId, goal.getId());
        }
    }

    private void removeGoalData(String userId, UUID goalId) {
        List<PlanEntity> plans = planRepository.findByGoalId(goalId);
        if (!plans.isEmpty()) {
            planExecutionRepository.deleteByPlanIdIn(
                    plans.stream().map(PlanEntity::getId).toList());
        }
        planRepository.deleteByGoalId(goalId);
        conversationService.deleteConversation(userId, goalId);
        goalRepository.deleteById(goalId);
    }

    private String buildPreview(String userId, UUID goalId, GoalEntity goal) {
        String title = goal.getTitle();
        if (title != null && !title.isBlank()) {
            return truncate(title, 80);
        }
        return conversationService.loadHistory(userId, goalId).stream()
                .filter(msg -> "user".equals(msg.role()))
                .map(ChatMessage::content)
                .findFirst()
                .map(content -> truncate(content, 80))
                .orElse("新对话");
    }

    @Transactional
    public PlanDsl regeneratePlan(UUID goalId) {
        GoalEntity entity = goalRepository
                .findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        Goal goal = mapper.toDomain(entity);
        if (goal.getContext() == null) {
            goal.setContext(new GoalContext());
        }
        PlanDsl plan = planner.generatePlan(goal, goal.getContext());
        savePlan(goalId, plan);
        entity.setPhase(GoalPhase.PLAN_READY);
        entity.setStatus(GoalStatus.ACTIVE);
        entity.setUpdatedAt(Instant.now());
        goalRepository.save(entity);
        return plan;
    }

    @Transactional
    public ExecutionResult executePlan(UUID goalId) {
        GoalEntity entity = goalRepository
                .findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (entity.getPhase() != GoalPhase.PLAN_READY) {
            throw new InvalidGoalPhaseException(entity.getPhase());
        }

        String token = userService.getDecryptedToken(entity.getUserId());
        PlanEntity planEntity = planRepository
                .findFirstByGoalIdOrderByVersionDesc(goalId)
                .orElseThrow(() -> new IllegalStateException("No plan found for goal: " + goalId));

        entity.setPhase(GoalPhase.EXECUTING);
        entity.setUpdatedAt(Instant.now());
        goalRepository.save(entity);

        ToolCallCollector.begin();
        try {
            ExecutionResult result = planExecutor.execute(planEntity.getDsl(), token);
            List<ToolCallRecord> toolCalls = ToolCallCollector.drain();
            result = new ExecutionResult(
                    result.success(), result.projectId(), result.taskIds(), result.errorMessage(), toolCalls);
            if (result.success()) {
                entity.setPhase(GoalPhase.COMPLETED);
                entity.setTicktickProjectId(result.projectId());
                entity.setStatus(GoalStatus.DONE);
            } else {
                entity.setPhase(GoalPhase.FAILED);
            }
            entity.setUpdatedAt(Instant.now());
            goalRepository.save(entity);
            savePlanExecution(planEntity.getId(), result);
            return result;
        } catch (RuntimeException exception) {
            ToolCallCollector.discard();
            throw exception;
        }
    }

    private Goal createGoalEntity(String userId, String message, GoalPhase phase) {
        Instant now = Instant.now();
        GoalEntity entity = new GoalEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTitle(truncate(message, 100));
        entity.setDescription(message);
        entity.setStatus(GoalStatus.DRAFT);
        entity.setPhase(phase);
        entity.setContext(new GoalContext());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        goalRepository.save(entity);
        return mapper.toDomain(entity);
    }

    private void saveGoal(Goal goal) {
        GoalEntity entity = goalRepository
                .findById(goal.getId())
                .orElseThrow(() -> new GoalNotFoundException(goal.getId()));
        entity.setTitle(goal.getTitle());
        entity.setDescription(goal.getDescription());
        entity.setStatus(goal.getStatus());
        entity.setPhase(goal.getPhase());
        entity.setTargetDate(goal.getTargetDate());
        entity.setContext(goal.getContext());
        entity.setTicktickProjectId(goal.getTicktickProjectId());
        entity.setUpdatedAt(Instant.now());
        goalRepository.save(entity);
    }

    private void savePlan(UUID goalId, PlanDsl plan) {
        int version = planRepository
                .findFirstByGoalIdOrderByVersionDesc(goalId)
                .map(existing -> existing.getVersion() + 1)
                .orElse(1);
        planRepository.save(mapper.toEntity(goalId, plan, version));
    }

    private void savePlanExecution(UUID planId, ExecutionResult result) {
        PlanExecutionEntity execution = new PlanExecutionEntity();
        execution.setId(UUID.randomUUID());
        execution.setPlanId(planId);
        execution.setStatus(result.success() ? "SUCCESS" : "FAILED");
        if (result.projectId() != null) {
            execution.setTicktickRefs(new TickTickRefs(result.projectId(), result.taskIds()));
        }
        execution.setErrorMessage(result.errorMessage());
        execution.setCreatedAt(Instant.now());
        planExecutionRepository.save(execution);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record GoalDetail(Goal goal, PlanDsl latestPlan) {}

    public record GoalListItem(Goal goal, String preview, Instant updatedAt) {}

    public record GoalConversation(Goal goal, PlanDsl latestPlan, List<ChatMessage> messages) {}
}
