package com.tickmine.infra.persistence.repository;

import com.tickmine.infra.persistence.entity.GoalEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {

    List<GoalEntity> findByUserId(String userId);
}
