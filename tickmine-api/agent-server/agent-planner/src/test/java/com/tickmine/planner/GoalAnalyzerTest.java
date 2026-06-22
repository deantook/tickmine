package com.tickmine.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalAnalysis;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.llm.AgentChatService;
import java.time.Instant;
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
class GoalAnalyzerTest {

    @Mock
    private AgentChatService chatService;

    private GoalAnalyzer goalAnalyzer;

    @BeforeEach
    void setUp() {
        PromptLoader promptLoader = new PromptLoader(new DefaultResourceLoader());
        goalAnalyzer = new GoalAnalyzer(chatService, promptLoader);
    }

    @Test
    void analyze_callsStructuredOutputWithRenderedPrompt() {
        Goal goal = Goal.builder()
                .userId("user-1")
                .title("策划婚礼")
                .description("上海秋季婚礼")
                .context(contextWith("city", "上海"))
                .build();
        List<ChatMessage> history = List.of(
                new ChatMessage("user", "我们想在秋天办婚礼", Instant.parse("2026-01-01T00:00:00Z")));

        GoalAnalysis expected = new GoalAnalysis(
                false, List.of("budget", "guestCount"), Map.of("season", "秋季"), "上海秋季婚礼策划");
        when(chatService.structuredOutput(
                eq("user-1"), eq("你是结构化分析助手。"), any(), eq(GoalAnalysis.class)))
                .thenReturn(expected);

        GoalAnalysis result = goalAnalyzer.analyze("user-1", goal, history);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService).structuredOutput(
                eq("user-1"), eq("你是结构化分析助手。"), promptCaptor.capture(), eq(GoalAnalysis.class));

        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("策划婚礼");
        assertThat(prompt).contains("上海秋季婚礼");
        assertThat(prompt).contains("city: 上海");
        assertThat(prompt).contains("user: 我们想在秋天办婚礼");
    }

    @Test
    void analyze_emptyHistory_usesPlaceholder() {
        Goal goal = Goal.builder()
                .userId("user-1")
                .title("旅行计划")
                .build();
        GoalAnalysis expected = new GoalAnalysis(true, List.of(), Map.of(), "旅行计划");
        when(chatService.structuredOutput(any(), any(), any(), eq(GoalAnalysis.class)))
                .thenReturn(expected);

        goalAnalyzer.analyze("user-1", goal, List.of());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatService).structuredOutput(any(), any(), promptCaptor.capture(), eq(GoalAnalysis.class));
        assertThat(promptCaptor.getValue()).contains("（暂无对话）");
    }

    private static GoalContext contextWith(String key, Object value) {
        GoalContext context = new GoalContext();
        context.setAttributes(Map.of(key, value));
        return context;
    }
}
