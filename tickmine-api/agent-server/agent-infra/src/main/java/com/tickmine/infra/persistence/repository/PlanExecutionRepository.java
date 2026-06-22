package com.tickmine.infra.persistence.repository;

import com.tickmine.infra.persistence.entity.PlanExecutionEntity;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanExecutionRepository extends JpaRepository<PlanExecutionEntity, UUID> {

    void deleteByPlanId(UUID planId);

    void deleteByPlanIdIn(Collection<UUID> planIds);
}
