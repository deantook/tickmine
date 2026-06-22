package com.tickmine.executor;

import com.tickmine.domain.exception.TickTickNotConnectedException;
import com.tickmine.domain.exception.TickTickTokenInvalidException;
import com.tickmine.domain.port.TaskQueryService;
import com.tickmine.domain.port.TickTickClient;
import com.tickmine.domain.port.TickTickProjectResponse;
import com.tickmine.domain.port.TickTickTaskResponse;
import com.tickmine.infra.service.UserService;
import com.tickmine.domain.util.TickTickDates;
import com.tickmine.mcp.exception.TickTickApiException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TickTickTaskQueryService implements TaskQueryService {

    private static final Pattern TODAY_PATTERN = Pattern.compile("今天|今日");
    private static final Pattern TOMORROW_PATTERN = Pattern.compile("明天");
    private static final Pattern WEEK_PATTERN = Pattern.compile("本周|这周");
    private static final int STATUS_COMPLETED = 2;
    private static final int UNDATED_FALLBACK_LIMIT = 15;

    private final TickTickClient tickTickClient;
    private final UserService userService;

    @Override
    public String answerQuery(String userId, String message) {
        String token;
        try {
            token = userService.getDecryptedToken(userId);
        } catch (TickTickNotConnectedException e) {
            return "你还没有绑定滴答清单 Token。请先到设置页绑定，然后我就能帮你查任务了。";
        }

        List<TickTickTaskResponse> activeTasks;
        try {
            activeTasks = fetchActiveTasks(token);
        } catch (TickTickApiException e) {
            if (e.getStatusCode().value() == 401) {
                userService.invalidateToken(userId);
                return "滴答 API 口令已失效或未授权。请到设置页重新获取并绑定 API 口令（dp_ 开头）。";
            }
            throw e;
        }
        QueryScope scope = detectScope(message);
        List<TickTickTaskResponse> filtered = filterByScope(activeTasks, scope);

        if (filtered.isEmpty() && scope == QueryScope.TODAY) {
            List<TickTickTaskResponse> undated = activeTasks.stream()
                    .filter(TickTickTaskQueryService::isUndated)
                    .limit(UNDATED_FALLBACK_LIMIT)
                    .toList();
            if (!undated.isEmpty()) {
                return formatUndatedFallback(undated, activeTasks.size());
            }
        }

        if (filtered.isEmpty()) {
            return emptyMessage(scope);
        }
        return formatTasks(filtered, scope);
    }

    private List<TickTickTaskResponse> fetchActiveTasks(String token) {
        List<TickTickTaskResponse> all = new ArrayList<>();
        all.addAll(tickTickClient.getProjectTasks("inbox", "收集箱", token));
        for (TickTickProjectResponse project : tickTickClient.listProjects(token)) {
            if ("inbox".equals(project.id())) {
                continue;
            }
            all.addAll(tickTickClient.getProjectTasks(project.id(), project.name(), token));
        }
        return all.stream()
                .filter(task -> task.status() == null || task.status() != STATUS_COMPLETED)
                .toList();
    }

    private static QueryScope detectScope(String message) {
        String text = message != null ? message : "";
        if (TODAY_PATTERN.matcher(text).find()) {
            return QueryScope.TODAY;
        }
        if (TOMORROW_PATTERN.matcher(text).find()) {
            return QueryScope.TOMORROW;
        }
        if (WEEK_PATTERN.matcher(text).find()) {
            return QueryScope.WEEK;
        }
        if (text.contains("任务") || text.contains("待办") || text.contains("todo")) {
            return QueryScope.ALL;
        }
        return QueryScope.ALL;
    }

    private static List<TickTickTaskResponse> filterByScope(
            List<TickTickTaskResponse> tasks, QueryScope scope) {
        LocalDate today = LocalDate.now(TickTickDates.DEFAULT_ZONE);
        return tasks.stream()
                .filter(task -> matchesScope(task, scope, today))
                .sorted(Comparator.comparing(
                        task -> TickTickDates.sortKey(task.dueDate(), task.startDate(), task.timeZone()),
                        Comparator.nullsLast(LocalDate::compareTo)))
                .toList();
    }

    private static boolean matchesScope(TickTickTaskResponse task, QueryScope scope, LocalDate today) {
        String timeZone = task.timeZone();
        return switch (scope) {
            case TODAY -> TickTickDates.matchesDay(task.startDate(), task.dueDate(), timeZone, today);
            case TOMORROW -> TickTickDates.matchesDay(
                    task.startDate(), task.dueDate(), timeZone, today.plusDays(1));
            case WEEK -> TickTickDates.overlapsRange(
                    task.startDate(), task.dueDate(), timeZone, today, today.plusDays(6));
            case ALL -> true;
        };
    }

    private static boolean isUndated(TickTickTaskResponse task) {
        return TickTickDates.isUndated(task.startDate(), task.dueDate(), task.timeZone());
    }

    private static String emptyMessage(QueryScope scope) {
        return switch (scope) {
            case TODAY -> "你今天没有到期的待办任务。";
            case TOMORROW -> "明天没有到期的待办任务。";
            case WEEK -> "本周没有到期的待办任务。";
            case ALL -> "你的滴答清单中目前没有未完成的任务。";
        };
    }

    private static String formatUndatedFallback(List<TickTickTaskResponse> undated, int totalActive) {
        StringBuilder sb = new StringBuilder("你今天没有设置到期日或逾期的任务。");
        if (totalActive > undated.size()) {
            sb.append("你还有 ").append(totalActive - undated.size()).append(" 项未来日期的待办。");
        }
        sb.append("\n\n以下是没有日期的待办（共 ")
                .append(undated.size())
                .append(" 项）：\n");
        appendTaskLines(sb, undated, false);
        sb.append("\n如需规划新目标，直接告诉我，比如「下午三点去大润发买菜」。");
        return sb.toString().trim();
    }

    private static String formatTasks(List<TickTickTaskResponse> tasks, QueryScope scope) {
        String header = switch (scope) {
            case TODAY -> "你今天有 " + tasks.size() + " 项待办：\n";
            case TOMORROW -> "明天有 " + tasks.size() + " 项待办：\n";
            case WEEK -> "本周有 " + tasks.size() + " 项待办：\n";
            case ALL -> "你共有 " + tasks.size() + " 项未完成待办：\n";
        };
        StringBuilder sb = new StringBuilder(header);
        appendTaskLines(sb, tasks, true);
        sb.append("\n如需规划新目标，直接告诉我，比如「帮我策划一场婚礼」。");
        return sb.toString().trim();
    }

    private static void appendTaskLines(
            StringBuilder sb, List<TickTickTaskResponse> tasks, boolean showDate) {
        LocalDate today = LocalDate.now(TickTickDates.DEFAULT_ZONE);
        for (int i = 0; i < tasks.size(); i++) {
            TickTickTaskResponse task = tasks.get(i);
            sb.append(i + 1).append(". ").append(task.title());
            if (task.projectName() != null && !task.projectName().isBlank()) {
                sb.append("（").append(task.projectName()).append("）");
            }
            if (showDate) {
                LocalDate[] range =
                        TickTickDates.dateRange(task.startDate(), task.dueDate(), task.timeZone());
                if (range != null) {
                    if (range[0].equals(range[1])) {
                        sb.append(" — ").append(range[1]);
                    } else {
                        sb.append(" — ").append(range[0]).append(" 至 ").append(range[1]);
                    }
                    if (range[1].isBefore(today)) {
                        sb.append("（已逾期）");
                    }
                }
            }
            sb.append("\n");
        }
    }

    private enum QueryScope {
        TODAY,
        TOMORROW,
        WEEK,
        ALL
    }
}
