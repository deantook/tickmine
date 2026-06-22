package com.tickmine.planner;

import static org.assertj.core.api.Assertions.assertThat;

import com.tickmine.domain.model.ChatIntent;
import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.IntentClassification;
import com.tickmine.llm.AgentChatService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;

@ExtendWith(MockitoExtension.class)
class LlmIntentClassifierTest {

    @Mock
    private AgentChatService chatService;

    private LlmIntentClassifier classifier;

    @BeforeEach
    void setUp() {
        PromptLoader promptLoader = new PromptLoader(new DefaultResourceLoader());
        classifier = new LlmIntentClassifier(chatService, promptLoader);
    }

    @Test
    void classify_todayTasksQuery_returnsQueryWithoutLlm() {
        IntentClassification result = classifier.classify(
                "user-1", "我今天有哪些任务", null, List.of());

        assertThat(result.intent()).isEqualTo(ChatIntent.QUERY);
    }

    @Test
    void classify_planningMessage_returnsPlanWithoutLlm() {
        IntentClassification result = classifier.classify(
                "user-1", "我想在上海策划一场婚礼", null, List.of());

        assertThat(result.intent()).isEqualTo(ChatIntent.PLAN);
    }

    @Test
    void classify_collectingPhase_returnsPlanWithoutLlm() {
        IntentClassification result = classifier.classify(
                "user-1",
                "预算10万",
                GoalPhase.COLLECTING,
                List.of(new ChatMessage("user", "我想策划婚礼", Instant.now())));

        assertThat(result.intent()).isEqualTo(ChatIntent.PLAN);
    }

    @Test
    void classify_planReadyPhase_returnsPlanWithoutLlm() {
        IntentClassification result = classifier.classify(
                "user-1",
                "再加一个蜜月旅行环节",
                GoalPhase.PLAN_READY,
                List.of(new ChatMessage("user", "我想策划婚礼", Instant.now())));

        assertThat(result.intent()).isEqualTo(ChatIntent.PLAN);
    }

    @Test
    void classify_queryDuringCollecting_returnsQuery() {
        IntentClassification result = classifier.classify(
                "user-1",
                "我今天有哪些任务",
                GoalPhase.COLLECTING,
                List.of(new ChatMessage("user", "我想策划婚礼", Instant.now())));

        assertThat(result.intent()).isEqualTo(ChatIntent.QUERY);
    }
}
