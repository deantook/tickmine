package com.tickmine.planner;

import com.tickmine.domain.exception.TickTickNotConnectedException;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalAnalysis;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.GoalStatus;
import com.tickmine.domain.model.MilestoneDsl;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.TaskDsl;
import com.tickmine.domain.port.GoalAnalysisService;
import com.tickmine.domain.port.Planner;
import com.tickmine.domain.port.TaskQueryService;
import com.tickmine.domain.port.TickTickClient;
import com.tickmine.domain.port.TickTickProjectResponse;
import com.tickmine.domain.port.TickTickTaskResponse;
import com.tickmine.infra.service.UserService;
import com.tickmine.mcp.exception.TickTickApiException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TickMineAgentTools {

    private final TaskQueryService taskQueryService;
    private final TickTickClient tickTickClient;
    private final UserService userService;
    private final GoalAnalysisService goalAnalyzer;
    private final Planner planner;

    @Tool(
            description =
                    """
                    查询用户滴答清单中的未完成任务。用于用户想查看已有待办、今天/明天/本周任务、或询问「有哪些任务」。
                    不要用于用户描述「我要去做某事/明天八点去医院」这类安排新任务的场景。
                    """)
    public String queryTasks(
            @ToolParam(description = "用户的查询意图，原样传入即可，如「今天有哪些任务」") String query) {
        AgentSessionContext.State ctx = AgentSessionContext.require();
        return taskQueryService.answerQuery(ctx.userId(), query);
    }

    @Tool(
            description =
                    """
                    列出用户滴答清单中所有项目/清单（含收集箱 inbox）。返回 id 与名称，便于后续 get_ticktick_project_tasks。
                    """)
    public String listTicktickProjects() {
        AgentSessionContext.State ctx = AgentSessionContext.require();
        String token = requireToken(ctx.userId());
        try {
            List<TickTickProjectResponse> projects = tickTickClient.listProjects(token);
            if (projects.isEmpty()) {
                return "用户没有任何清单（收集箱仍可用，projectId=inbox）。";
            }
            StringBuilder sb = new StringBuilder("用户清单列表：\n");
            sb.append("- inbox（收集箱）\n");
            for (TickTickProjectResponse project : projects) {
                if ("inbox".equals(project.id())) {
                    continue;
                }
                sb.append("- ").append(project.name()).append(" (id: ").append(project.id()).append(")\n");
            }
            return sb.toString().trim();
        } catch (TickTickApiException e) {
            return tickTickErrorMessage(ctx.userId(), e);
        }
    }

    @Tool(
            description =
                    """
                    获取指定滴答清单/项目下的未完成任务。projectIdOrName 可为项目 id（如 abc123）或清单名称（模糊匹配）。
                    收集箱请传 inbox 或 收集箱。
                    """)
    public String getTicktickProjectTasks(
            @ToolParam(description = "项目 id 或清单名称") String projectIdOrName) {
        AgentSessionContext.State ctx = AgentSessionContext.require();
        String token = requireToken(ctx.userId());
        try {
            ResolvedProject project = resolveProject(projectIdOrName, token);
            List<TickTickTaskResponse> tasks =
                    tickTickClient.getProjectTasks(project.id(), project.name(), token);
            List<TickTickTaskResponse> active = tasks.stream()
                    .filter(task -> task.status() == null || task.status() != 2)
                    .toList();
            if (active.isEmpty()) {
                return "清单「" + project.name() + "」中没有未完成任务。";
            }
            StringBuilder sb = new StringBuilder("清单「")
                    .append(project.name())
                    .append("」未完成任务（")
                    .append(active.size())
                    .append(" 项）：\n");
            for (int i = 0; i < active.size(); i++) {
                TickTickTaskResponse task = active.get(i);
                sb.append(i + 1).append(". ").append(task.title());
                if (task.dueDate() != null && !task.dueDate().isBlank()) {
                    sb.append(" — ").append(task.dueDate());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (TickTickApiException e) {
            return tickTickErrorMessage(ctx.userId(), e);
        }
    }

    @Tool(
            description =
                    """
                    根据你对用户需求的理解，生成滴答清单计划**草案**（不会写入滴答，需用户在前端确认）。
                    调用前确保已理解用户要什么；不确定时应先向用户澄清，而非猜测。
                    scope: minimal=仅用户明确提到的事项（1-3条，优先收集箱）;
                    standard=中等目标合理展开; comprehensive=复杂多阶段项目。
                    """)
    public String proposePlan(
            @ToolParam(description = "用一两句话总结你对用户需求的理解") String userIntentSummary,
            @ToolParam(description = "minimal | standard | comprehensive") String scope) {
        AgentSessionContext.State ctx = AgentSessionContext.require();
        Goal goal = ctx.goal();
        String normalizedScope = normalizeScope(scope);

        if (userIntentSummary != null && !userIntentSummary.isBlank()) {
            String prefix = goal.getDescription() != null && !goal.getDescription().isBlank()
                    ? goal.getDescription() + "\n\n"
                    : "";
            goal.setDescription(prefix + "【Agent理解】" + userIntentSummary.trim());
        }

        GoalAnalysis analysis = goalAnalyzer.analyze(ctx.userId(), goal, ctx.history());
        if (goal.getContext() == null) {
            goal.setContext(new GoalContext());
        }
        goal.getContext().merge(analysis.extractedAttributes());
        if (analysis.suggestedTitle() != null && !analysis.suggestedTitle().isBlank()) {
            goal.setTitle(analysis.suggestedTitle());
        }
        applyExtractedTargetDate(goal);
        goal.getContext().getAttributes().put("planScope", normalizedScope);

        PlanDsl plan = planner.generatePlan(goal, goal.getContext());
        ctx.setProposedPlan(plan);
        goal.setPhase(GoalPhase.PLAN_READY);
        goal.setStatus(GoalStatus.ACTIVE);
        if (ctx.onPlanProposed() != null) {
            ctx.onPlanProposed().accept(goal, plan);
        }
        return formatPlanForTool(plan, normalizedScope);
    }

    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "standard";
        }
        return switch (scope.trim().toLowerCase(Locale.ROOT)) {
            case "minimal", "simple", "min" -> "minimal";
            case "comprehensive", "complex", "full" -> "comprehensive";
            default -> "standard";
        };
    }

    private String requireToken(String userId) {
        try {
            return userService.getDecryptedToken(userId);
        } catch (TickTickNotConnectedException e) {
            throw new IllegalStateException("用户尚未绑定滴答清单 Token，请先到设置页绑定 API 口令（dp_ 开头）。");
        }
    }

    private ResolvedProject resolveProject(String projectIdOrName, String token) {
        String input = projectIdOrName != null ? projectIdOrName.trim() : "";
        if (input.isEmpty()) {
            throw new IllegalArgumentException("请提供项目 id 或名称");
        }
        if ("inbox".equalsIgnoreCase(input) || input.contains("收集箱")) {
            return new ResolvedProject("inbox", "收集箱");
        }
        List<TickTickProjectResponse> projects = tickTickClient.listProjects(token);
        for (TickTickProjectResponse project : projects) {
            if (input.equals(project.id())) {
                return new ResolvedProject(project.id(), project.name());
            }
        }
        for (TickTickProjectResponse project : projects) {
            if (project.name() != null && project.name().contains(input)) {
                return new ResolvedProject(project.id(), project.name());
            }
        }
        throw new IllegalArgumentException("未找到清单: " + input);
    }

    private String tickTickErrorMessage(String userId, TickTickApiException e) {
        if (e.getStatusCode().value() == 401) {
            userService.invalidateToken(userId);
            return "滴答 API 口令已失效，请到设置页重新绑定。";
        }
        return "滴答 API 调用失败: " + e.getMessage();
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
            // planner falls back to todayDate
        }
    }

    static String formatPlanForTool(PlanDsl plan, String scope) {
        StringBuilder sb = new StringBuilder();
        sb.append("计划草案已生成（scope=").append(scope).append("，待用户确认后才会写入滴答）：\n");
        if (plan.useInbox()) {
            sb.append("写入位置：收集箱\n");
        } else {
            sb.append("写入位置：新建清单「").append(plan.projectName()).append("」\n");
        }
        for (MilestoneDsl milestone : plan.milestones()) {
            sb.append("\n## ").append(milestone.name()).append("\n");
            appendTasks(sb, milestone.tasks());
        }
        sb.append("\n请向用户解释该计划，并提醒点击「确认写入」。");
        return sb.toString();
    }

    private static void appendTasks(StringBuilder sb, List<TaskDsl> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            TaskDsl task = tasks.get(i);
            sb.append(i + 1).append(". ").append(task.title());
            if (task.dueDate() != null) {
                sb.append(" [").append(task.dueDate());
                if (task.dueTime() != null && !task.dueTime().isBlank()) {
                    sb.append(" ").append(task.dueTime());
                }
                sb.append(']');
            }
            sb.append("\n");
        }
    }

    private record ResolvedProject(String id, String name) {}
}
