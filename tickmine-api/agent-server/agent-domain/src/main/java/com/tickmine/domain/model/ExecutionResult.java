package com.tickmine.domain.model;

import java.util.List;

public record ExecutionResult(
        boolean success,
        String projectId,
        List<String> taskIds,
        String errorMessage,
        List<ToolCallRecord> toolCalls) {

    public ExecutionResult(boolean success, String projectId, List<String> taskIds, String errorMessage) {
        this(success, projectId, taskIds, errorMessage, List.of());
    }
}
