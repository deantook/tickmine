package com.tickmine.domain.model;

import java.util.List;

public record PlanDsl(String projectName, List<MilestoneDsl> milestones, String destination) {
    public PlanDsl {
        if (destination == null || destination.isBlank()) {
            destination = "project";
        }
    }

    public PlanDsl(String projectName, List<MilestoneDsl> milestones) {
        this(projectName, milestones, "project");
    }

    public boolean useInbox() {
        return "inbox".equalsIgnoreCase(destination);
    }
}
