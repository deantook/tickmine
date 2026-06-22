package com.tickmine.domain.model;

import java.time.LocalDate;
import java.util.List;

public record TaskDsl(
        String title,
        String description,
        String priority,
        LocalDate dueDate,
        String dueTime,
        List<ChecklistItemDsl> checklistItems
) {
    public TaskDsl {
        if (checklistItems == null) {
            checklistItems = List.of();
        }
    }
}
