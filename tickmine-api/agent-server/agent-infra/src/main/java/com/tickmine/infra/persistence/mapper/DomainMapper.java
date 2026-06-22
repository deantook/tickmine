package com.tickmine.infra.persistence.mapper;

import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.SubscriptionTier;
import com.tickmine.domain.model.TokenStatus;
import com.tickmine.infra.persistence.entity.ConversationEntity;
import com.tickmine.infra.persistence.entity.GoalEntity;
import com.tickmine.infra.persistence.entity.PlanEntity;
import com.tickmine.infra.persistence.entity.UserEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DomainMapper {

    public Goal toDomain(GoalEntity entity) {
        if (entity == null) {
            return null;
        }
        return Goal.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .phase(entity.getPhase())
                .targetDate(entity.getTargetDate())
                .context(entity.getContext())
                .ticktickProjectId(entity.getTicktickProjectId())
                .build();
    }

    public GoalEntity toEntity(Goal goal) {
        if (goal == null) {
            return null;
        }
        GoalEntity entity = new GoalEntity();
        entity.setId(goal.getId() != null ? goal.getId() : UUID.randomUUID());
        entity.setUserId(goal.getUserId());
        entity.setTitle(goal.getTitle());
        entity.setDescription(goal.getDescription());
        entity.setStatus(goal.getStatus());
        entity.setPhase(goal.getPhase());
        entity.setTargetDate(goal.getTargetDate());
        entity.setContext(goal.getContext() != null ? goal.getContext() : new GoalContext());
        entity.setTicktickProjectId(goal.getTicktickProjectId());
        return entity;
    }

    public SubscriptionTier toSubscriptionTier(UserEntity entity) {
        return entity != null ? entity.getSubscriptionTier() : null;
    }

    public TokenStatus toTokenStatus(UserEntity entity) {
        return entity != null ? entity.getTokenStatus() : null;
    }

    public UserEntity toNewUserEntity(String userId) {
        Instant now = Instant.now();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setSubscriptionTier(SubscriptionTier.FREE);
        entity.setTokenStatus(TokenStatus.NOT_CONNECTED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public UserEntity toNewUserEntity(String userId, String email, String passwordHash) {
        UserEntity entity = toNewUserEntity(userId);
        entity.setEmail(email);
        entity.setPasswordHash(passwordHash);
        return entity;
    }

    public PlanDsl toDomain(PlanEntity entity) {
        return entity != null ? entity.getDsl() : null;
    }

    public PlanEntity toEntity(UUID goalId, PlanDsl dsl, int version) {
        PlanEntity entity = new PlanEntity();
        entity.setId(UUID.randomUUID());
        entity.setGoalId(goalId);
        entity.setDsl(dsl);
        entity.setVersion(version);
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    public PlanEntity toEntity(PlanEntity existing, PlanDsl dsl, int version) {
        if (existing == null) {
            return null;
        }
        PlanEntity entity = new PlanEntity();
        entity.setId(existing.getId());
        entity.setGoalId(existing.getGoalId());
        entity.setDsl(dsl);
        entity.setVersion(version);
        entity.setCreatedAt(existing.getCreatedAt());
        return entity;
    }

    public List<ChatMessage> toDomainMessages(ConversationEntity entity) {
        if (entity == null || entity.getMessages() == null) {
            return List.of();
        }
        return List.copyOf(entity.getMessages());
    }

    public ConversationEntity toEntity(
            UUID id, String userId, UUID goalId, List<ChatMessage> messages) {
        ConversationEntity entity = new ConversationEntity();
        entity.setId(id != null ? id : UUID.randomUUID());
        entity.setUserId(userId);
        entity.setGoalId(goalId);
        entity.setMessages(messages != null ? new ArrayList<>(messages) : new ArrayList<>());
        return entity;
    }
}
