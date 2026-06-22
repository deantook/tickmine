package com.tickmine.infra.service;

import com.tickmine.domain.exception.GoalNotFoundException;
import com.tickmine.domain.exception.InvalidGoalPhaseException;
import com.tickmine.domain.model.ChatIntent;
import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.ChatResponse;
import com.tickmine.domain.model.ExecutionResult;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalAnalysis;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.GoalStatus;
import com.tickmine.domain.model.IntentClassification;
import com.tickmine.domain.model.MilestoneDsl;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.TaskDsl;
import com.tickmine.domain.model.ToolCallRecord;
import com.tickmine.domain.port.ChatAssistant;
import com.tickmine.domain.port.GoalAnalysisService;
import com.tickmine.domain.port.IntentClassifier;
import com.tickmine.domain.port.PlanExecutor;
import com.tickmine.domain.port.Planner;
import com.tickmine.domain.port.PromptTemplateLoader;
import com.tickmine.domain.port.TaskQueryService;
import com.tickmine.infra.persistence.entity.GoalEntity;
import com.tickmine.infra.persistence.entity.PlanEntity;
import com.tickmine.infra.persistence.entity.PlanExecutionEntity;
import com.tickmine.infra.persistence.entity.TickTickRefs;
import com.tickmine.infra.persistence.mapper.DomainMapper;
import com.tickmine.infra.persistence.repository.GoalRepository;
import com.tickmine.infra.persistence.repository.PlanExecutionRepository;
import com.tickmine.infra.persistence.repository.PlanRepository;
import com.tickmine.domain.util.ToolCallCollector;
import com.tickmine.infra.service.ChatStreamPrepareResult.LlmReplySource;
import com.tickmine.infra.service.ChatStreamPrepareResult.ReplySource;
import com.tickmine.infra.service.ChatStreamPrepareResult.StaticReplySource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalAgentService {

    private static final String CHAT_SYSTEM_PROMPT =
            "你是 TickMine 任务管理助手。用简洁自然的中文回复，不要编造用户未提及的场景。";

    private final QuotaService quotaService;
    private final ConversationService conversationService;
    private final GoalAnalysisService goalAnalyzer;
    private final IntentClassifier intentClassifier;
    private final TaskQueryService taskQueryService;
    private final Planner planner;
    private final PlanExecutor planExecutor;
    private final UserService userService;
    private final GoalRepository goalRepository;
    private final PlanRepository planRepository;
    private final PlanExecutionRepository planExecutionRepository;
    private final DomainMapper mapper;
    private final PromptTemplateLoader promptLoader;
    private final ChatAssistant chatAssistant;

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

            Goal existingGoal = loadGoalIfPresent(userId, goalId);
            GoalPhase currentPhase = existingGoal != null ? existingGoal.getPhase() : null;
            List<ChatMessage> priorHistory =
                    existingGoal != null ? conversationService.loadHistory(userId, existingGoal.getId()) : List.of();

            IntentClassification classification =
                    intentClassifier.classify(userId, message, currentPhase, priorHistory);
            ChatIntent intent = classification.intent();

            Goal goal = resolveGoalForIntent(userId, message, existingGoal, intent);
            conversationService.appendMessage(
                    userId, goal.getId(), new ChatMessage("user", message, Instant.now()));

            List<ChatMessage> history = conversationService.loadHistory(userId, goal.getId());

            ChatStreamPrepareResult prep =
                    switch (intent) {
                        case QUERY -> prepareQuery(userId, goal, message);
                        case CHAT -> prepareFreeChat(userId, goal, history);
                        default -> preparePlanning(userId, goal, history);
                    };
            return withToolCalls(prep);
        } catch (RuntimeException exception) {
            ToolCallCollector.discard();
            throw exception;
        }
    }

    private ChatStreamPrepareResult withToolCalls(ChatStreamPrepareResult prep) {
        return new ChatStreamPrepareResult(
                prep.goal(),
                prep.userId(),
                prep.replySource(),
                prep.responsePhase(),
                prep.plan(),
                prep.missingFields(),
                ToolCallCollector.drain());
    }

    public void emitStreamReply(ChatStreamPrepareResult prep, Consumer<String> onDelta) {
        ReplySource source = prep.replySource();
        if (source instanceof LlmReplySource llm) {
            chatAssistant.streamChat(llm.userId(), llm.systemPrompt(), llm.userPrompt(), onDelta);
            return;
        }
        if (source instanceof StaticReplySource stat) {
            streamStaticText(stat.text(), onDelta);
        }
    }

    @Transactional
    public ChatResponse finalizeStreamChat(ChatStreamPrepareResult prep, String fullReply) {
        saveGoal(prep.goal());
        conversationService.appendMessage(
                prep.userId(),
                prep.goal().getId(),
                new ChatMessage("assistant", fullReply, Instant.now()));

        return switch (prep.responsePhase()) {
            case COLLECTING -> ChatResponse.collecting(
                    prep.goal().getId(), fullReply, prep.missingFields(), prep.toolCalls());
            case PLAN_READY -> ChatResponse.planReady(
                    prep.goal().getId(), fullReply, prep.plan(), prep.toolCalls());
            default -> ChatResponse.chat(prep.goal().getId(), fullReply, prep.toolCalls());
        };
    }

    private ChatStreamPrepareResult prepareQuery(String userId, Goal goal, String message) {
        String reply = taskQueryService.answerQuery(userId, message);
        goal.setPhase(GoalPhase.CHAT);
        return new ChatStreamPrepareResult(
                goal, userId, new StaticReplySource(reply), GoalPhase.CHAT, null, null, List.of());
    }

    private ChatStreamPrepareResult prepareFreeChat(String userId, Goal goal, List<ChatMessage> history) {
        goal.setPhase(GoalPhase.CHAT);
        ReplySource replySource = new LlmReplySource(
                userId,
                CHAT_SYSTEM_PROMPT,
                promptLoader.load(
                        "chat.st", Map.of("conversation", formatHistory(history))));
        return new ChatStreamPrepareResult(goal, userId, replySource, GoalPhase.CHAT, null, null, List.of());
    }

    private ChatStreamPrepareResult preparePlanning(String userId, Goal goal, List<ChatMessage> history) {
        GoalAnalysis analysis = goalAnalyzer.analyze(userId, goal, history);

        if (goal.getContext() == null) {
            goal.setContext(new GoalContext());
        }
        goal.getContext().merge(analysis.extractedAttributes());
        if (analysis.suggestedTitle() != null && !analysis.suggestedTitle().isBlank()) {
            goal.setTitle(analysis.suggestedTitle());
        }
        applyExtractedTargetDate(goal);

        if (!analysis.isComplete()) {
            goal.setPhase(GoalPhase.COLLECTING);
            ReplySource replySource = new LlmReplySource(
                    userId,
                    CHAT_SYSTEM_PROMPT,
                    promptLoader.load(
                            "follow-up.st",
                            Map.of(
                                    "title", nullToEmpty(goal.getTitle()),
                                    "missingFields", formatMissingFields(analysis.missingFields()),
                                    "attributes",
                                            formatAttributes(goal.getContext().getAttributes()))));
            return new ChatStreamPrepareResult(
                    goal,
                    userId,
                    replySource,
                    GoalPhase.COLLECTING,
                    null,
                    analysis.missingFields(),
                    List.of());
        }

        PlanDsl plan = planner.generatePlan(goal, goal.getContext());
        savePlan(goal.getId(), plan);
        goal.setPhase(GoalPhase.PLAN_READY);
        goal.setStatus(GoalStatus.ACTIVE);
        String preview = formatPlanPreview(plan);
        return new ChatStreamPrepareResult(
                goal,
                userId,
                new StaticReplySource(preview),
                GoalPhase.PLAN_READY,
                plan,
                null,
                List.of());
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

    private Goal loadGoalIfPresent(String userId, UUID goalId) {
        if (goalId == null) {
            return null;
        }
        GoalEntity entity = goalRepository
                .findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));
        if (!entity.getUserId().equals(userId)) {
            throw new GoalNotFoundException(goalId);
        }
        return mapper.toDomain(entity);
    }

    private Goal resolveGoalForIntent(
            String userId, String message, Goal existingGoal, ChatIntent intent) {
        if (intent == ChatIntent.PLAN) {
            if (existingGoal != null && existingGoal.getPhase() == GoalPhase.COLLECTING) {
                return existingGoal;
            }
            return createGoalEntity(userId, message, GoalPhase.COLLECTING);
        }

        if (existingGoal != null && existingGoal.getPhase() == GoalPhase.CHAT) {
            return existingGoal;
        }
        return createGoalEntity(userId, truncate(message, 50), GoalPhase.CHAT);
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

    private static void applyExtractedTargetDate(Goal goal) {
        if (goal.getTargetDate() != null || goal.getContext() == null) {
            return;
        }
        Object rawDate = goal.getContext().getAttributes().get("targetDate");
        if (!(rawDate instanceof String dateText) || dateText.isBlank()) {
            return;
        }
        try {
            goal.setTargetDate(LocalDate.parse(dateText));
        } catch (DateTimeParseException ignored) {
            // LLM may return non-ISO values; planner still has todayDate fallback.
        }
    }

    private static String formatPlanPreview(PlanDsl plan) {
        StringBuilder sb = new StringBuilder();
        if (plan.useInbox()) {
            sb.append("已为您生成待办，确认后将写入收集箱：\n");
            appendTasks(sb, plan);
            sb.append("\n请确认后写入收集箱。");
            return sb.toString();
        }

        sb.append("已为您生成计划「").append(plan.projectName()).append("」：\n");
        for (MilestoneDsl milestone : plan.milestones()) {
            sb.append("\n## ").append(milestone.name()).append("\n");
            appendTasks(sb, milestone.tasks());
        }
        sb.append("\n请确认后执行计划。");
        return sb.toString();
    }

    private static void appendTasks(StringBuilder sb, PlanDsl plan) {
        for (MilestoneDsl milestone : plan.milestones()) {
            appendTasks(sb, milestone.tasks());
        }
    }

    private static void appendTasks(StringBuilder sb, List<TaskDsl> tasks) {
        for (TaskDsl task : tasks) {
            sb.append("- ").append(task.title());
            if (task.dueDate() != null) {
                sb.append(" (").append(task.dueDate());
                if (task.dueTime() != null && !task.dueTime().isBlank()) {
                    sb.append(" ").append(task.dueTime());
                }
                sb.append(")");
            }
            sb.append("\n");
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String formatMissingFields(List<String> missingFields) {
        if (missingFields == null || missingFields.isEmpty()) {
            return "（暂无）";
        }
        return String.join(", ", missingFields);
    }

    public record GoalDetail(Goal goal, PlanDsl latestPlan) {}

    private static String formatAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "（暂无）";
        }
        return attributes.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static String formatHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "（暂无对话）";
        }
        int start = Math.max(0, history.size() - 10);
        return history.subList(start, history.size()).stream()
                .map(msg -> msg.role() + ": " + msg.content())
                .collect(Collectors.joining("\n"));
    }
}
