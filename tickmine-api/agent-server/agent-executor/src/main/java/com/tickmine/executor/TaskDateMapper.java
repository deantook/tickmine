package com.tickmine.executor;

import com.tickmine.domain.model.TaskDsl;
import com.tickmine.domain.port.TickTickTaskRequest;
import com.tickmine.domain.util.TickTickDates;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

final class TaskDateMapper {

    private TaskDateMapper() {}

    static TickTickTaskRequest applyDates(
            TaskDsl task,
            String projectId,
            String parentId,
            Integer priority,
            String kind,
            List<TickTickTaskRequest.ChecklistItem> items,
            String content) {
        ZoneId zone = TickTickDates.DEFAULT_ZONE;
        LocalTime time = TickTickDates.parseTime(task.dueTime());
        String instant = null;
        Boolean isAllDay = null;
        if (task.dueDate() != null) {
            instant = TickTickDates.toTickTickInstant(task.dueDate(), time, zone);
            isAllDay = time == null;
        }
        return new TickTickTaskRequest(
                task.title(),
                projectId,
                content,
                priority,
                instant,
                instant,
                isAllDay,
                zone.getId(),
                parentId,
                kind,
                items);
    }
}
