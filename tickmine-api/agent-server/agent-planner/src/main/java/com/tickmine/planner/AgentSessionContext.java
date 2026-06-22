package com.tickmine.planner;

import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.PlanDsl;
import java.util.List;
import java.util.function.BiConsumer;

public final class AgentSessionContext {

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<>();

    private AgentSessionContext() {}

    public static void begin(
            String userId,
            Goal goal,
            List<ChatMessage> history,
            BiConsumer<Goal, PlanDsl> onPlanProposed) {
        HOLDER.set(new State(userId, goal, history, onPlanProposed));
    }

    public static State require() {
        State state = HOLDER.get();
        if (state == null) {
            throw new IllegalStateException("Agent session not initialized");
        }
        return state;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static final class State {
        private final String userId;
        private final Goal goal;
        private final List<ChatMessage> history;
        private final BiConsumer<Goal, PlanDsl> onPlanProposed;
        private PlanDsl proposedPlan;

        private State(
                String userId,
                Goal goal,
                List<ChatMessage> history,
                BiConsumer<Goal, PlanDsl> onPlanProposed) {
            this.userId = userId;
            this.goal = goal;
            this.history = history;
            this.onPlanProposed = onPlanProposed;
        }

        public String userId() {
            return userId;
        }

        public Goal goal() {
            return goal;
        }

        public List<ChatMessage> history() {
            return history;
        }

        public BiConsumer<Goal, PlanDsl> onPlanProposed() {
            return onPlanProposed;
        }

        public PlanDsl proposedPlan() {
            return proposedPlan;
        }

        public void setProposedPlan(PlanDsl proposedPlan) {
            this.proposedPlan = proposedPlan;
        }
    }
}
