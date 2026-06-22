package com.tickmine.planner;

import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalAnalysis;
import com.tickmine.domain.port.GoalAnalysisService;
import com.tickmine.llm.AgentChatService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoalAnalyzer implements GoalAnalysisService {

    private final AgentChatService chatService;
    private final PromptLoader promptLoader;

    @Override
    public GoalAnalysis analyze(String userId, Goal goal, List<ChatMessage> history) {
        Map<String, Object> attributes = goal.getContext() != null
                ? goal.getContext().getAttributes()
                : Map.of();
        String prompt = promptLoader.load("goal-analyzer.st", Map.of(
                "title", nullToEmpty(goal.getTitle()),
                "description", nullToEmpty(goal.getDescription()),
                "attributes", PromptVariables.formatAttributes(attributes),
                "todayDate", LocalDate.now().toString(),
                "conversation", formatHistory(history)));
        return chatService.structuredOutput(userId, "你是结构化分析助手。", prompt, GoalAnalysis.class);
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String formatHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "（暂无对话）";
        }
        return history.stream()
                .map(msg -> msg.role() + ": " + msg.content())
                .collect(Collectors.joining("\n"));
    }
}
