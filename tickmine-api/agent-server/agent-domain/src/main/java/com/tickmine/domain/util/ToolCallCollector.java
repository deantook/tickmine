package com.tickmine.domain.util;

import com.tickmine.domain.model.ToolCallRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ToolCallCollector {

    private static final ThreadLocal<List<ToolCallRecord>> CALLS = new ThreadLocal<>();

    private ToolCallCollector() {}

    public static void begin() {
        CALLS.set(new ArrayList<>());
    }

    public static void record(
            String name,
            Map<String, Object> input,
            Object output,
            long durationMs,
            boolean success,
            String errorMessage) {
        List<ToolCallRecord> calls = CALLS.get();
        if (calls == null) {
            return;
        }
        calls.add(new ToolCallRecord(name, input, output, durationMs, success, errorMessage));
    }

    public static List<ToolCallRecord> drain() {
        List<ToolCallRecord> calls = CALLS.get();
        CALLS.remove();
        return calls != null ? List.copyOf(calls) : List.of();
    }

    public static void discard() {
        CALLS.remove();
    }
}
