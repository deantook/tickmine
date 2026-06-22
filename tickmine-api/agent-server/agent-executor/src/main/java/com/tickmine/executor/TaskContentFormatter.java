package com.tickmine.executor;

import com.tickmine.domain.model.TaskDsl;

final class TaskContentFormatter {

    private TaskContentFormatter() {}

    static String format(TaskDsl task) {
        StringBuilder sb = new StringBuilder();
        if (task.estimatedDuration() != null && !task.estimatedDuration().isBlank()) {
            sb.append("预计耗时：").append(task.estimatedDuration().trim());
        }
        if (task.description() != null && !task.description().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(task.description().trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
