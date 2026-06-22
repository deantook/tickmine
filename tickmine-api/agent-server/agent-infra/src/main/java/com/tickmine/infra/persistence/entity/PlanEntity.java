package com.tickmine.infra.persistence.entity;

import com.tickmine.domain.model.PlanDsl;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "plans")
@Data
public class PlanEntity {

    @Id
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @JdbcTypeCode(SqlTypes.JSON)
    private PlanDsl dsl;

    private int version;

    @Column(name = "created_at")
    private Instant createdAt;
}
