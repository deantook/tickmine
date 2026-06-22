package com.tickmine.mcp;

import com.tickmine.domain.port.TickTickClient;
import com.tickmine.domain.port.TickTickProjectResponse;
import com.tickmine.domain.port.TickTickTaskRequest;
import com.tickmine.domain.port.TickTickTaskResponse;
import com.tickmine.domain.util.TickTickDates;
import com.tickmine.domain.util.ToolCallCollector;
import com.tickmine.mcp.exception.TickTickApiException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class TickTickClientImpl implements TickTickClient {

    private static final int LOG_MAX_LEN = 500;

    private final WebClient webClient;
    private final MeterRegistry meterRegistry;

    @Autowired
    public TickTickClientImpl(
            WebClient.Builder webClientBuilder,
            MeterRegistry meterRegistry,
            @Value("${tickmine.ticktick.base-url:https://api.dida365.com/open/v1}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String createProject(String name, String token) {
        Map<String, Object> body = Map.of("name", name, "viewMode", "list");
        Map<String, Object> input = Map.of("name", name, "viewMode", "list");
        Map<String, Object> response = post("/project", body, token, "ticktick/create_project", input);
        String projectId = (String) response.get("id");
        log.info("TickTick createProject result projectId={} name={}", projectId, name);
        return projectId;
    }

    @Override
    public String createTask(TickTickTaskRequest task, String token) {
        Map<String, Object> input = summarizeTaskRequest(task);
        Map<String, Object> response = post("/task", task, token, "ticktick/create_task", input);
        String taskId = (String) response.get("id");
        log.info(
                "TickTick createTask result taskId={} title={} projectId={}",
                taskId,
                task.title(),
                task.projectId());
        return taskId;
    }

    @Override
    public void updateTask(String taskId, TickTickTaskRequest task, String token) {
        Map<String, Object> input = summarizeTaskRequest(task);
        input = new java.util.LinkedHashMap<>(input);
        input.put("taskId", taskId);
        post("/task/" + taskId, task, token, "ticktick/update_task", input);
        log.info("TickTick updateTask result taskId={} title={}", taskId, task.title());
    }

    @Override
    public List<TickTickTaskResponse> listTasks(String projectId, String token) {
        return getProjectTasks(projectId, null, token);
    }

    @Override
    public List<TickTickProjectResponse> listProjects(String token) {
        List<Map<String, Object>> response = get(
                "/project",
                "ticktick/list_projects",
                Map.of(),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {},
                token);
        if (response == null) {
            return List.of();
        }
        List<TickTickProjectResponse> projects = response.stream()
                .map(project -> new TickTickProjectResponse(
                        (String) project.get("id"), (String) project.get("name")))
                .toList();
        log.info("TickTick listProjects result count={}", projects.size());
        return projects;
    }

    @Override
    public List<TickTickTaskResponse> getProjectTasks(String projectId, String projectName, String token) {
        Map<String, Object> input = Map.of(
                "projectId", projectId,
                "projectName", projectName != null ? projectName : "");
        Map<String, Object> response = get(
                "/project/" + projectId + "/data",
                "ticktick/get_project_tasks",
                input,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                token);
        if (response == null) {
            return List.of();
        }
        String resolvedName = projectName != null ? projectName : (String) response.get("name");
        List<TickTickTaskResponse> tasks = parseTasks(response, projectId, resolvedName);
        log.info("TickTick getProjectTasks result projectId={} count={}", projectId, tasks.size());
        return tasks;
    }

    private List<TickTickTaskResponse> parseTasks(
            Map<String, Object> projectData, String projectId, String projectName) {
        Object tasksObj = projectData.get("tasks");
        if (!(tasksObj instanceof List<?> tasks)) {
            return List.of();
        }
        List<TickTickTaskResponse> result = new ArrayList<>();
        for (Object item : tasks) {
            if (!(item instanceof Map<?, ?> task)) {
                continue;
            }
            result.add(new TickTickTaskResponse(
                    (String) task.get("id"),
                    projectId,
                    projectName,
                    (String) task.get("title"),
                    TickTickDates.normalize(task.get("dueDate")),
                    TickTickDates.normalize(task.get("startDate")),
                    TickTickDates.normalize(task.get("timeZone")),
                    toBoolean(task.get("isAllDay")),
                    toInteger(task.get("status"))));
        }
        return result;
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return null;
    }

    private <T> T get(
            String path,
            String toolName,
            Map<String, Object> input,
            ParameterizedTypeReference<T> typeRef,
            String token) {
        long startMs = System.currentTimeMillis();
        log.info("TickTick API GET start op={} path={}", toolName, path);
        try {
            T response = webClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(typeRef)
                    .block();
            meterRegistry.counter("tickmine.ticktick.calls", "op", toolName).increment();
            long durationMs = System.currentTimeMillis() - startMs;
            log.info(
                    "TickTick API GET ok op={} path={} durationMs={}",
                    toolName,
                    path,
                    durationMs);
            ToolCallCollector.record(
                    toolName, input, summarizeGetOutput(toolName, response), durationMs, true, null);
            return response;
        } catch (WebClientResponseException e) {
            meterRegistry.counter("tickmine.ticktick.failures").increment();
            logTickTickFailure("GET", toolName, path, startMs, e);
            ToolCallCollector.record(
                    toolName,
                    input,
                    null,
                    System.currentTimeMillis() - startMs,
                    false,
                    truncate(e.getResponseBodyAsString()));
            throw new TickTickApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            meterRegistry.counter("tickmine.ticktick.failures").increment();
            log.error(
                    "TickTick API GET failed op={} path={} durationMs={}",
                    toolName,
                    path,
                    System.currentTimeMillis() - startMs,
                    e);
            ToolCallCollector.record(
                    toolName,
                    input,
                    null,
                    System.currentTimeMillis() - startMs,
                    false,
                    e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> post(String path, Object body, String token, String toolName, Map<String, Object> input) {
        long startMs = System.currentTimeMillis();
        log.info("TickTick API POST start op={} path={} body={}", toolName, path, summarizeBody(body));
        try {
            Map<String, Object> response = webClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            meterRegistry.counter("tickmine.ticktick.calls", "op", toolName).increment();
            long durationMs = System.currentTimeMillis() - startMs;
            log.info(
                    "TickTick API POST ok op={} path={} durationMs={} responseKeys={}",
                    toolName,
                    path,
                    durationMs,
                    response != null ? response.keySet() : null);
            ToolCallCollector.record(toolName, input, summarizePostOutput(response), durationMs, true, null);
            return response;
        } catch (WebClientResponseException e) {
            meterRegistry.counter("tickmine.ticktick.failures").increment();
            logTickTickFailure("POST", toolName, path, startMs, e);
            ToolCallCollector.record(
                    toolName,
                    input,
                    null,
                    System.currentTimeMillis() - startMs,
                    false,
                    truncate(e.getResponseBodyAsString()));
            throw new TickTickApiException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            meterRegistry.counter("tickmine.ticktick.failures").increment();
            log.error(
                    "TickTick API POST failed op={} path={} durationMs={}",
                    toolName,
                    path,
                    System.currentTimeMillis() - startMs,
                    e);
            ToolCallCollector.record(
                    toolName,
                    input,
                    null,
                    System.currentTimeMillis() - startMs,
                    false,
                    e.getMessage());
            throw e;
        }
    }

    private static Map<String, Object> summarizeTaskRequest(TickTickTaskRequest task) {
        java.util.LinkedHashMap<String, Object> input = new java.util.LinkedHashMap<>();
        input.put("title", task.title());
        input.put("projectId", task.projectId());
        input.put("description", task.content());
        input.put("priority", task.priority());
        input.put("startDate", task.startDate());
        input.put("dueDate", task.dueDate());
        input.put("timeZone", task.timeZone());
        input.put("isAllDay", task.isAllDay());
        input.put("parentId", task.parentId());
        input.put("kind", task.kind());
        if (task.items() != null) {
            input.put("checklistCount", task.items().size());
        }
        return input;
    }

    private static Object summarizeGetOutput(String toolName, Object response) {
        if ("ticktick/list_projects".equals(toolName) && response instanceof List<?> projects) {
            java.util.LinkedHashMap<String, Object> summary = new java.util.LinkedHashMap<>();
            summary.put("count", projects.size());
            summary.put(
                    "projects",
                    projects.stream()
                            .filter(Map.class::isInstance)
                            .map(Map.class::cast)
                            .map(project -> entryMap(
                                    "id", project.get("id"),
                                    "name", project.get("name")))
                            .toList());
            return summary;
        }
        if ("ticktick/get_project_tasks".equals(toolName) && response instanceof Map<?, ?> projectData) {
            Object tasksObj = projectData.get("tasks");
            if (!(tasksObj instanceof List<?> tasks)) {
                return entryMap("count", 0, "tasks", List.of());
            }
            java.util.LinkedHashMap<String, Object> summary = new java.util.LinkedHashMap<>();
            summary.put("projectName", projectData.get("name"));
            summary.put("count", tasks.size());
            summary.put(
                    "tasks",
                    tasks.stream()
                            .filter(Map.class::isInstance)
                            .map(Map.class::cast)
                            .map(task -> entryMap(
                                    "id", task.get("id"),
                                    "title", task.get("title"),
                                    "dueDate", task.get("dueDate"),
                                    "startDate", task.get("startDate"),
                                    "status", task.get("status")))
                            .toList());
            return summary;
        }
        return response;
    }

    private static java.util.LinkedHashMap<String, Object> entryMap(Object... keyValues) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> summarizePostOutput(Map<String, Object> response) {
        if (response == null) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> summary = new java.util.LinkedHashMap<>();
        if (response.containsKey("id")) {
            summary.put("id", response.get("id"));
        }
        if (response.containsKey("title")) {
            summary.put("title", response.get("title"));
        }
        if (response.containsKey("projectId")) {
            summary.put("projectId", response.get("projectId"));
        }
        if (summary.isEmpty()) {
            summary.put("keys", response.keySet());
        }
        return summary;
    }

    private static void logTickTickFailure(
            String method, String operation, String path, long startMs, WebClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        log.warn(
                "TickTick API {} failed op={} path={} durationMs={} status={} body={}",
                method,
                operation,
                path,
                System.currentTimeMillis() - startMs,
                status.value(),
                truncate(e.getResponseBodyAsString()));
    }

    private static String summarizeBody(Object body) {
        if (body instanceof TickTickTaskRequest task) {
            return "title=" + task.title() + ", projectId=" + task.projectId() + ", parentId=" + task.parentId();
        }
        if (body instanceof Map<?, ?> map) {
            return map.toString();
        }
        return body != null ? body.getClass().getSimpleName() : "null";
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').trim();
        return normalized.length() <= LOG_MAX_LEN
                ? normalized
                : normalized.substring(0, LOG_MAX_LEN) + "...";
    }
}
