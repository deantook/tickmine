package com.tickmine.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.GoalPhase;
import com.tickmine.domain.model.GoalStatus;
import com.tickmine.domain.model.SubscriptionTier;
import com.tickmine.domain.model.TokenStatus;
import com.tickmine.infra.InfraTestApplication;
import com.tickmine.infra.persistence.entity.GoalEntity;
import com.tickmine.infra.persistence.entity.UserEntity;
import com.tickmine.infra.persistence.repository.GoalRepository;
import com.tickmine.infra.persistence.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(InfraTestApplication.class)
class RepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Test
    void saveUserAndGoalWithJsonContext_findByUserIdWorks() {
        Instant now = Instant.now();
        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setSubscriptionTier(SubscriptionTier.FREE);
        user.setTokenStatus(TokenStatus.NOT_CONNECTED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.saveAndFlush(user);

        GoalContext context = new GoalContext();
        context.merge(Map.of("skillLevel", "beginner", "hoursPerWeek", 5));

        GoalEntity goal = new GoalEntity();
        goal.setId(UUID.randomUUID());
        goal.setUserId("user-1");
        goal.setTitle("Learn Java");
        goal.setDescription("Master Spring Boot");
        goal.setStatus(GoalStatus.DRAFT);
        goal.setPhase(GoalPhase.COLLECTING);
        goal.setContext(context);
        goal.setCreatedAt(now);
        goal.setUpdatedAt(now);
        goalRepository.saveAndFlush(goal);

        var found = goalRepository.findByUserId("user-1");

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getTitle()).isEqualTo("Learn Java");
        assertThat(found.getFirst().getContext().getAttributes())
                .containsEntry("skillLevel", "beginner")
                .containsEntry("hoursPerWeek", 5);
    }
}
