package com.tickmine.domain.model;

import java.util.HashMap;
import java.util.Map;

public class GoalContext {

    private Map<String, Object> attributes = new HashMap<>();

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    public void merge(Map<String, Object> updates) {
        if (updates != null) {
            this.attributes.putAll(updates);
        }
    }
}
