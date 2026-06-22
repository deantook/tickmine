package com.tickmine.executor;

import com.tickmine.domain.model.ExecutionResult;
import com.tickmine.domain.model.MilestoneDsl;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.TaskDsl;
import com.tickmine.domain.port.PlanExecutor;
import com.tickmine.domain.port.TickTickClient;
import com.tickmine.domain.port.TickTickTaskRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TickTickPlanExecutor implements PlanExecutor {

    static final String INBOX_PROJECT_ID = "inbox";

    private final TickTickClient tickTickClient;

    @Override
    public ExecutionResult execute(PlanDsl plan, String ticktickToken) {
        if (plan.useInbox()) {
            return executeInInbox(plan, ticktickToken);
        }
        return executeAsProject(plan, ticktickToken);
    }

    private ExecutionResult executeInInbox(PlanDsl plan, String ticktickToken) {
        List<String> taskIds = new ArrayList<>();
        try {
            for (MilestoneDsl milestone : plan.milestones()) {
                for (TaskDsl task : milestone.tasks()) {
                    String taskId =
                            tickTickClient.createTask(toRequest(task, INBOX_PROJECT_ID, null), ticktickToken);
                    taskIds.add(taskId);
                }
            }
            return new ExecutionResult(true, INBOX_PROJECT_ID, taskIds, null);
        } catch (Exception e) {
            return new ExecutionResult(false, null, taskIds, e.getMessage());
        }
    }

    private ExecutionResult executeAsProject(PlanDsl plan, String ticktickToken) {
        List<String> taskIds = new ArrayList<>();
        try {
            String projectId = tickTickClient.createProject(plan.projectName(), ticktickToken);
            for (MilestoneDsl milestone : plan.milestones()) {
                String parentId = tickTickClient.createTask(
                        new TickTickTaskRequest(
                                milestone.name(), projectId, null, null, null, null, null, null, null, null, null),
                        ticktickToken);
                taskIds.add(parentId);
                for (TaskDsl task : milestone.tasks()) {
                    String taskId = tickTickClient.createTask(toRequest(task, projectId, parentId), ticktickToken);
                    taskIds.add(taskId);
                }
            }
            return new ExecutionResult(true, projectId, taskIds, null);
        } catch (Exception e) {
            return new ExecutionResult(false, null, taskIds, e.getMessage());
        }
    }

    private TickTickTaskRequest toRequest(TaskDsl task, String projectId, String parentId) {
        String kind = null;
        List<TickTickTaskRequest.ChecklistItem> items = null;
        if (!task.checklistItems().isEmpty()) {
            kind = "CHECKLIST";
            items = task.checklistItems().stream()
                    .map(item -> new TickTickTaskRequest.ChecklistItem(item.title()))
                    .toList();
        }
        return TaskDateMapper.applyDates(
                task,
                projectId,
                parentId,
                PriorityMapper.toTickTick(task.priority()),
                kind,
                items,
                TaskContentFormatter.format(task));
    }
}
