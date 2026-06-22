package com.tickmine.domain.exception;

import java.util.UUID;

public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException(UUID goalId) {
        super("Goal not found: " + goalId);
    }
}
