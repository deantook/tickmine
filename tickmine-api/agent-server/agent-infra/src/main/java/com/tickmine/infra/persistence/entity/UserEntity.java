package com.tickmine.infra.persistence.entity;

import com.tickmine.domain.model.SubscriptionTier;
import com.tickmine.domain.model.TokenStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class UserEntity {

    @Id
    private String id;

    @Column(name = "subscription_tier")
    @Enumerated(EnumType.STRING)
    private SubscriptionTier subscriptionTier;

    @Column(name = "ticktick_token_enc")
    private String ticktickTokenEnc;

    @Column(name = "token_status")
    @Enumerated(EnumType.STRING)
    private TokenStatus tokenStatus;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;
}
