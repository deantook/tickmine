package com.tickmine.planner;

import com.tickmine.domain.model.AgentRunOutcome;
import com.tickmine.domain.model.AgentRunRequest;
import com.tickmine.domain.model.AgentRunResult;
import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.port.AgentOrchestrator;
import com.tickmine.domain.exception.TickTickNotConnectedException;
import com.tickmine.infra.service.UserService;
import com.tickmine.llm.ModelResolver;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TickMineAgentOrchestrator implements AgentOrchestrator {

    private final ModelResolver modelResolver;
    private final PromptLoader promptLoader;
    private final TickMineAgentTools agentTools;
    private final UserService userService;

    @Override
    public AgentRunResult run(AgentRunRequest request) {
        StringBuilder buffer = new StringBuilder();
        streamRun(request, buffer::append);
        return toResult(request);
    }

    @Override
    public void streamRun(AgentRunRequest request, Consumer<String> onDelta) {
        AgentSessionContext.begin(
                request.userId(),
                request.goal(),
                request.history(),
                request.onPlanProposed());
        try {
            String systemPrompt = promptLoader.load(
                    "agent.st",
                    Map.of(
                            "todayDate", LocalDate.now().toString(),
                            "goalTitle", nullToEmpty(request.goal().getTitle()),
                            "goalPhase", request.goal().getPhase() != null
                                    ? request.goal().getPhase().name()
                                    : "CHAT",
                            "ticktickConnected", String.valueOf(isTicktickConnected(request.userId())),
                            "conversation", formatHistory(request.history())));

            List<Message> messages = toSpringMessages(request.history());
            String reply = ChatClient.create(modelResolver.resolve(request.userId()))
                    .prompt()
                    .system(systemPrompt)
                    .messages(messages)
                    .tools(agentTools)
                    .call()
                    .content();

            streamText(reply, onDelta);
        } finally {
            AgentSessionContext.clear();
        }
    }

    private AgentRunResult toResult(AgentRunRequest request) {
        AgentRunOutcome outcome = request.outcome();
        if (outcome.plan() != null) {
            return new AgentRunResult(GoalPhase.PLAN_READY, outcome.plan(), request.goal());
        }
        if (request.goal().getPhase() != GoalPhase.PLAN_READY) {
            request.goal().setPhase(GoalPhase.CHAT);
        }
        return new AgentRunResult(GoalPhase.CHAT, null, request.goal());
    }

    private boolean isTicktickConnected(String userId) {
        try {
            userService.getDecryptedToken(userId);
            return true;
        } catch (TickTickNotConnectedException e) {
            return false;
        }
    }

    private static List<Message> toSpringMessages(List<ChatMessage> history) {
        List<Message> messages = new ArrayList<>();
        int start = Math.max(0, history.size() - 20);
        for (ChatMessage msg : history.subList(start, history.size())) {
            if ("assistant".equals(msg.role())) {
                messages.add(new AssistantMessage(msg.content()));
            } else {
                messages.add(new UserMessage(msg.content()));
            }
        }
        return messages;
    }

    private static String formatHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "（暂无对话）";
        }
        int start = Math.max(0, history.size() - 20);
        return history.subList(start, history.size()).stream()
                .map(msg -> msg.role() + ": " + msg.content())
                .collect(Collectors.joining("\n"));
    }

    private static void streamText(String text, Consumer<String> onDelta) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int chunkSize = 8;
        for (int i = 0; i < text.length(); i += chunkSize) {
            onDelta.accept(text.substring(i, Math.min(i + chunkSize, text.length())));
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
