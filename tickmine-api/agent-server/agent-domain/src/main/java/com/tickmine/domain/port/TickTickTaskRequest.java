package com.tickmine.domain.port;

import java.util.List;

public record TickTickTaskRequest(
        String title,
        String projectId,
        String content,
        Integer priority,
        String startDate,
        String dueDate,
        Boolean isAllDay,
        String timeZone,
        String parentId,
        String kind,
        List<ChecklistItem> items
) {

    public record ChecklistItem(String title) {
    }
}
