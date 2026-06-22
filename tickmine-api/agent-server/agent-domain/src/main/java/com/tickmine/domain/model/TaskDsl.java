package com.tickmine.domain.model;

import java.time.LocalDate;
import java.util.List;

public record TaskDsl(
        String title,
        String description,
        String priority,
        LocalDate dueDate,
        String dueTime,
        List<ChecklistItemDsl> checklistItems,
        String estimatedDuration
) {
    public TaskDsl {
        if (checklistItems == null) {
            checklistItems = List.of();
        }
    }

    public TaskDsl(
            String title,
            String description,
            String priority,
            LocalDate dueDate,
            String dueTime,
            List<ChecklistItemDsl> checklistItems) {
        this(title, description, priority, dueDate, dueTime, checklistItems, null);
    }
}
