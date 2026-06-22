package com.tickmine.domain.model;

import java.util.Map;

public record ToolCallRecord(
        String name,
        Map<String, Object> input,
        Object output,
        long durationMs,
        boolean success,
        String errorMessage) {}
