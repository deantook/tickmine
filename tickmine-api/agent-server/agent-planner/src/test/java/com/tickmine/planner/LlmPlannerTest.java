package com.tickmine.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.MilestoneDsl;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.TaskDsl;
import com.tickmine.llm.AgentChatService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;

@ExtendWith(MockitoExtension.class)
class LlmPlannerTest {

    @Mock
    private AgentChatService chatService;

    private LlmPlanner llmPlanner;

    @BeforeEach
    void setUp() {
        PromptLoader promptLoader = new PromptLoader(new DefaultResourceLoader());
        llmPlanner = new LlmPlanner(chatService, promptLoader);
    }

    @Test
    void generatePlan_callsStructuredOutputWithRenderedPrompt() {
        Goal goal = Goal.builder()
                .userId("user-1")
                .title("策划婚礼")
                .description("上海秋季婚礼")
                .targetDate(LocalDate.of(2026, 10, 1))
                .build();
        GoalContext context = new GoalContext();
        context.setAttributes(Map.of("city", "上海", "guestCount", 150));

        PlanDsl expected = new PlanDsl(
                "上海秋季婚礼",
                List.of(new MilestoneDsl(
                        "场地与供应商",
                        List.of(new TaskDsl(
                                "预订婚宴酒店",
                                "比较三家酒店报价",
                                "high",
                                LocalDate.of(2026, 6, 1),
                                null,
                                List.of())))));
        when(chatService.structuredOutput(
                eq("user-1"), eq("你是项目规划师。"), any(), eq(PlanDsl.class)))
                .thenReturn(expected);

        PlanDsl result = llmPlanner.generatePlan(goal, context);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService).structuredOutput(
                eq("user-1"), eq("你是项目规划师。"), promptCaptor.capture(), eq(PlanDsl.class));

        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("策划婚礼");
        assertThat(prompt).contains("上海秋季婚礼");
        assertThat(prompt).contains("city: 上海");
        assertThat(prompt).contains("guestCount: 150");
        assertThat(prompt).contains("2026-10-01");
        assertThat(prompt).contains("2026-06-22");
    }
}
