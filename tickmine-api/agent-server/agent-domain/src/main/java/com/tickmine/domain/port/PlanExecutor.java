package com.tickmine.domain.port;

import com.tickmine.domain.model.ExecutionResult;
import com.tickmine.domain.model.PlanDsl;

public interface PlanExecutor {

    ExecutionResult execute(PlanDsl plan, String ticktickToken);
}
