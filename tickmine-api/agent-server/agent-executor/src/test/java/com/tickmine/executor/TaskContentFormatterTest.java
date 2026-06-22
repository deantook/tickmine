package com.tickmine.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.tickmine.domain.model.TaskDsl;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskContentFormatterTest {

    @Test
    void format_includesEstimatedDurationAndDescription() {
        TaskDsl task = new TaskDsl(
                "预订场地",
                "比较三家酒店",
                "high",
                LocalDate.of(2026, 6, 1),
                null,
                List.of(),
                "1-2周");

        assertThat(TaskContentFormatter.format(task))
                .isEqualTo("预计耗时：1-2周\n比较三家酒店");
    }

    @Test
    void format_durationOnly() {
        TaskDsl task = new TaskDsl("调研", null, "medium", null, null, List.of(), "2小时");

        assertThat(TaskContentFormatter.format(task)).isEqualTo("预计耗时：2小时");
    }

    @Test
    void format_emptyWhenNoContent() {
        TaskDsl task = new TaskDsl("调研", null, "medium", null, null, List.of(), null);

        assertThat(TaskContentFormatter.format(task)).isNull();
    }
}
