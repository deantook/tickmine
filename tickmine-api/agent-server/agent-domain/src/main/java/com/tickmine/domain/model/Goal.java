package com.tickmine.domain.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Goal {

    private UUID id;
    private String userId;
    private String title;
    private String description;
    private GoalStatus status;
    private GoalPhase phase;
    private LocalDate targetDate;
    private GoalContext context;
    private String ticktickProjectId;
}
