package com.tickmine.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.tickmine.domain.model.ChecklistItemDsl;
import com.tickmine.domain.model.ExecutionResult;
import com.tickmine.domain.model.MilestoneDsl;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.TaskDsl;
import com.tickmine.domain.port.TickTickClient;
import com.tickmine.domain.port.TickTickTaskRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TickTickPlanExecutorTest {

    @Mock
    private TickTickClient tickTickClient;

    private TickTickPlanExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new TickTickPlanExecutor(tickTickClient);
    }

    @Test
    void execute_createsProjectMilestonesAndTasksInOrder() {
        PlanDsl plan = new PlanDsl(
                "Wedding Plan",
                List.of(
                        new MilestoneDsl(
                                "Venue",
                                List.of(new TaskDsl(
                                        "Book venue",
                                        "Research options",
                                        "high",
                                        LocalDate.of(2026, 6, 1),
                                        null,
                                        List.of(new ChecklistItemDsl("Call 3 venues"))))),
                        new MilestoneDsl("Catering", List.of(new TaskDsl(
                                "Choose menu", "Tasting session", "medium", null, null, List.of())))));

        when(tickTickClient.createProject("Wedding Plan", "token")).thenReturn("proj-1");
        when(tickTickClient.createTask(any(TickTickTaskRequest.class), eq("token")))
                .thenReturn("parent-1", "child-1", "parent-2", "child-2");

        ExecutionResult result = executor.execute(plan, "token");

        assertThat(result.success()).isTrue();
        assertThat(result.projectId()).isEqualTo("proj-1");
        assertThat(result.taskIds()).containsExactly("parent-1", "child-1", "parent-2", "child-2");
        assertThat(result.errorMessage()).isNull();

        InOrder order = inOrder(tickTickClient);
        order.verify(tickTickClient).createProject("Wedding Plan", "token");

        ArgumentCaptor<TickTickTaskRequest> requestCaptor = ArgumentCaptor.forClass(TickTickTaskRequest.class);
        order.verify(tickTickClient, org.mockito.Mockito.times(4))
                .createTask(requestCaptor.capture(), eq("token"));

        List<TickTickTaskRequest> requests = requestCaptor.getAllValues();
        assertThat(requests).hasSize(4);
        assertThat(requests.get(0).title()).isEqualTo("Venue");
        assertThat(requests.get(0).projectId()).isEqualTo("proj-1");

        TickTickTaskRequest childTask = requests.get(1);
        assertThat(childTask.title()).isEqualTo("Book venue");
        assertThat(childTask.parentId()).isEqualTo("parent-1");
        assertThat(childTask.priority()).isEqualTo(5);
        assertThat(childTask.startDate()).isEqualTo("2026-05-31T16:00:00+0000");
        assertThat(childTask.dueDate()).isEqualTo("2026-05-31T16:00:00+0000");
        assertThat(childTask.isAllDay()).isTrue();
        assertThat(childTask.timeZone()).isEqualTo("Asia/Shanghai");
        assertThat(childTask.kind()).isEqualTo("CHECKLIST");
        assertThat(childTask.items()).hasSize(1);
        assertThat(childTask.items().getFirst().title()).isEqualTo("Call 3 venues");

        assertThat(requests.get(2).title()).isEqualTo("Catering");

        assertThat(requests.get(3).title()).isEqualTo("Choose menu");
        assertThat(requests.get(3).parentId()).isEqualTo("parent-2");
        assertThat(requests.get(3).priority()).isEqualTo(3);
    }

    @Test
    void execute_inboxPlan_createsTimedTaskWhenDueTimePresent() {
        PlanDsl plan = new PlanDsl(
                "大润发买菜",
                List.of(new MilestoneDsl(
                        "待办",
                        List.of(new TaskDsl(
                                "去大润发买菜",
                                null,
                                "medium",
                                LocalDate.of(2026, 6, 22),
                                "15:00",
                                List.of())))),
                "inbox");

        when(tickTickClient.createTask(any(TickTickTaskRequest.class), eq("token")))
                .thenReturn("task-1");

        ExecutionResult result = executor.execute(plan, "token");

        assertThat(result.success()).isTrue();

        ArgumentCaptor<TickTickTaskRequest> requestCaptor = ArgumentCaptor.forClass(TickTickTaskRequest.class);
        org.mockito.Mockito.verify(tickTickClient).createTask(requestCaptor.capture(), eq("token"));

        TickTickTaskRequest request = requestCaptor.getValue();
        assertThat(request.title()).isEqualTo("去大润发买菜");
        assertThat(request.startDate()).isEqualTo("2026-06-22T07:00:00+0000");
        assertThat(request.dueDate()).isEqualTo("2026-06-22T07:00:00+0000");
        assertThat(request.isAllDay()).isFalse();
    }

    @Test
    void execute_inboxPlan_createsTasksWithoutProjectOrMilestones() {
        PlanDsl plan = new PlanDsl(
                "大润发买菜",
                List.of(new MilestoneDsl(
                        "待办",
                        List.of(
                                new TaskDsl(
                                        "15:00 去大润发买菜",
                                        null,
                                        "medium",
                                        LocalDate.of(2026, 6, 22),
                                        null,
                                        List.of()),
                                new TaskDsl("列购物清单", null, "low", LocalDate.of(2026, 6, 22), null, List.of())))),
                "inbox");

        when(tickTickClient.createTask(any(TickTickTaskRequest.class), eq("token")))
                .thenReturn("task-1", "task-2");

        ExecutionResult result = executor.execute(plan, "token");

        assertThat(result.success()).isTrue();
        assertThat(result.projectId()).isEqualTo("inbox");
        assertThat(result.taskIds()).containsExactly("task-1", "task-2");

        org.mockito.Mockito.verify(tickTickClient, org.mockito.Mockito.never()).createProject(any(), any());

        ArgumentCaptor<TickTickTaskRequest> requestCaptor = ArgumentCaptor.forClass(TickTickTaskRequest.class);
        org.mockito.Mockito.verify(tickTickClient, org.mockito.Mockito.times(2))
                .createTask(requestCaptor.capture(), eq("token"));

        List<TickTickTaskRequest> requests = requestCaptor.getAllValues();
        assertThat(requests).extracting(TickTickTaskRequest::projectId).containsOnly("inbox");
        assertThat(requests).extracting(TickTickTaskRequest::parentId).containsOnly((String) null);
        assertThat(requests.get(0).title()).isEqualTo("15:00 去大润发买菜");
    }
}
