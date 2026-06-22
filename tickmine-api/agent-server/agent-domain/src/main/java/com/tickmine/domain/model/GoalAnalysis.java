package com.tickmine.domain.model;

import java.util.List;
import java.util.Map;

public record GoalAnalysis(
        boolean isComplete,
        List<String> missingFields,
        Map<String, Object> extractedAttributes,
        String suggestedTitle
) {
}
