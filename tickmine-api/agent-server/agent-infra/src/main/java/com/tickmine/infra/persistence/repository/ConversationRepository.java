package com.tickmine.infra.persistence.repository;

import com.tickmine.infra.persistence.entity.ConversationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    Optional<ConversationEntity> findByUserIdAndGoalId(String userId, UUID goalId);
}
