package com.tickmine.planner;

import java.util.Map;
import java.util.stream.Collectors;

final class PromptVariables {

    private PromptVariables() {
    }

    static String formatAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "（暂无）";
        }
        return attributes.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }
}
