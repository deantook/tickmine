package com.tickmine.domain.port;

import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.PlanDsl;

public interface Planner {

    PlanDsl generatePlan(Goal goal, GoalContext context);
}
