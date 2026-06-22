package com.tickmine.domain.exception;

import com.tickmine.domain.model.GoalPhase;

public class InvalidGoalPhaseException extends RuntimeException {

    public InvalidGoalPhaseException(GoalPhase phase) {
        super("Invalid goal phase for operation: " + phase);
    }
}
