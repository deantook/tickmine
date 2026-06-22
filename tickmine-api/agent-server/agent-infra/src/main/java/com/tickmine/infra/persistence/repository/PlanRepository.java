package com.tickmine.infra.persistence.repository;

import com.tickmine.infra.persistence.entity.PlanEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<PlanEntity, UUID> {

    Optional<PlanEntity> findFirstByGoalIdOrderByVersionDesc(UUID goalId);
}
