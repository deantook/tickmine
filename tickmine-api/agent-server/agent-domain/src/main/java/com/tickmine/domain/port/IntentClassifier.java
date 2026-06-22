package com.tickmine.domain.port;

import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.IntentClassification;
import java.util.List;

public interface IntentClassifier {

    IntentClassification classify(
            String userId, String message, GoalPhase currentPhase, List<ChatMessage> history);
}
