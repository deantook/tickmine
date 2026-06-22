package com.tickmine.infra.persistence.entity;

import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.GoalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "goals")
@Data
public class GoalEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private GoalStatus status;

    @Enumerated(EnumType.STRING)
    private GoalPhase phase;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @JdbcTypeCode(SqlTypes.JSON)
    private GoalContext context;

    @Column(name = "ticktick_project_id")
    private String ticktickProjectId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
