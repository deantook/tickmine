package com.tickmine.domain.port;

import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalAnalysis;
import java.util.List;

public interface GoalAnalysisService {

    GoalAnalysis analyze(String userId, Goal goal, List<ChatMessage> history);
}
