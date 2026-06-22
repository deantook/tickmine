package com.tickmine.domain.model;

public final class AgentRunOutcome {

    private PlanDsl plan;
    private GoalPhase responsePhase = GoalPhase.CHAT;

    public PlanDsl plan() {
        return plan;
    }

    public void setPlan(PlanDsl plan) {
        this.plan = plan;
    }

    public GoalPhase responsePhase() {
        return responsePhase;
    }

    public void setResponsePhase(GoalPhase responsePhase) {
        this.responsePhase = responsePhase;
    }
}
