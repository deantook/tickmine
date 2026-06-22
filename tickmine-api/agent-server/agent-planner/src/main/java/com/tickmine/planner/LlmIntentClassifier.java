package com.tickmine.planner;

import com.tickmine.domain.model.ChatIntent;
import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.IntentClassification;
import com.tickmine.domain.port.IntentClassifier;
import com.tickmine.llm.AgentChatService;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmIntentClassifier implements IntentClassifier {

    private static final Pattern QUERY_PATTERN = Pattern.compile(
            "((今天|今日|明天|本周|这周).*(有哪些|有什么|几件事|多少项|多少|哪些))"
                    + "|((有哪些|有什么|看看|列出|查询|查一下).*(待办|todo|任务|清单|事))"
                    + "|((待办|todo|任务|清单).*(有哪些|列表|看看|查))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAN_PATTERN = Pattern.compile(
            "(策划|规划|帮我.*计划|制定.*计划|安排|筹备|组织|准备.*(婚礼|旅行|活动|项目|考试|搬家))",
            Pattern.CASE_INSENSITIVE);
    /** User states a future action with time/place — schedule a task, not query existing todos. */
    private static final Pattern SCHEDULE_PATTERN = Pattern.compile(
            "(我要|我要去|得去|需要去|记得|提醒我|帮我).*(去|做|买|参加|拆|取|缴|还|看)"
                    + "|(今天|明日|明天|后天).*(上午|下午|晚上|\\d{1,2}[点:：]).*(去|做|买|参加|拆|取|缴|还)"
                    + "|.*(下午|上午|晚上|\\d{1,2}[点:：]).*(去|做|买|参加|拆|取|缴|还)",
            Pattern.CASE_INSENSITIVE);

    private final AgentChatService chatService;
    private final PromptLoader promptLoader;

    @Override
    public IntentClassification classify(
            String userId, String message, GoalPhase currentPhase, List<ChatMessage> history) {
        IntentClassification ruleBased = tryRuleBased(message, currentPhase);
        if (ruleBased != null) {
            return ruleBased;
        }

        String prompt = promptLoader.load("intent-router.st", Map.of(
                "message", nullToEmpty(message),
                "currentPhase", currentPhase != null ? currentPhase.name() : "NONE",
                "conversation", formatHistory(history)));
        return chatService.structuredOutput(
                userId, "你是意图分类助手，只返回 JSON。", prompt, IntentClassification.class);
    }

    private static IntentClassification tryRuleBased(String message, GoalPhase currentPhase) {
        String text = nullToEmpty(message).trim();
        if (text.isEmpty()) {
            return new IntentClassification(ChatIntent.CHAT);
        }

        if (PLAN_PATTERN.matcher(text).find() || SCHEDULE_PATTERN.matcher(text).find()) {
            return new IntentClassification(ChatIntent.PLAN);
        }
        if (QUERY_PATTERN.matcher(text).find()) {
            return new IntentClassification(ChatIntent.QUERY);
        }
        if (currentPhase == GoalPhase.COLLECTING || currentPhase == GoalPhase.PLAN_READY) {
            return new IntentClassification(ChatIntent.PLAN);
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String formatHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "（暂无对话）";
        }
        int start = Math.max(0, history.size() - 6);
        return history.subList(start, history.size()).stream()
                .map(msg -> msg.role() + ": " + msg.content())
                .collect(Collectors.joining("\n"));
    }
}
