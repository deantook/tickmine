package com.tickmine.planner;

import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.port.Planner;
import com.tickmine.llm.AgentChatService;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmPlanner implements Planner {

    private final AgentChatService chatService;
    private final PromptLoader promptLoader;

    @Override
    public PlanDsl generatePlan(Goal goal, GoalContext context) {
        Map<String, Object> attributes = context != null ? context.getAttributes() : Map.of();
        String targetDate = goal.getTargetDate() != null
                ? goal.getTargetDate().toString()
                : "（未指定，默认今天）";
        String prompt = promptLoader.load("planner.st", Map.of(
                "title", nullToEmpty(goal.getTitle()),
                "description", nullToEmpty(goal.getDescription()),
                "attributes", PromptVariables.formatAttributes(attributes),
                "targetDate", targetDate,
                "todayDate", LocalDate.now().toString()));
        return chatService.structuredOutput(
                goal.getUserId(), "你是项目规划师。", prompt, PlanDsl.class);
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
