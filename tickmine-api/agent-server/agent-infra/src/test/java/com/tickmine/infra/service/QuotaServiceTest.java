package com.tickmine.infra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tickmine.domain.exception.QuotaExceededException;
import com.tickmine.domain.exception.UserNotFoundException;
import com.tickmine.domain.model.QuotaStatus;
import com.tickmine.domain.model.SubscriptionTier;
import com.tickmine.infra.config.TickMineProperties;
import com.tickmine.infra.persistence.entity.QuotaUsageEntity;
import com.tickmine.infra.persistence.entity.QuotaUsageId;
import com.tickmine.infra.persistence.entity.UserEntity;
import com.tickmine.infra.persistence.repository.QuotaUsageRepository;
import com.tickmine.infra.persistence.repository.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuotaUsageRepository quotaUsageRepository;

    private TickMineProperties props;
    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        props = new TickMineProperties();
        props.setQuota(Map.of(
                "FREE", quotaConfig(2),
                "VIP", quotaConfig(-1),
                "SVIP", quotaConfig(-1)));
        quotaService = new QuotaService(userRepository, quotaUsageRepository, props);
    }

    @Test
    void checkAndConsume_freeUserWithinLimit_incrementsUsage() {
        UserEntity user = user("free-user", SubscriptionTier.FREE);
        when(userRepository.findById("free-user")).thenReturn(Optional.of(user));
        when(quotaUsageRepository.findById(any())).thenReturn(Optional.empty());
        when(quotaUsageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        quotaService.checkAndConsume("free-user");

        ArgumentCaptor<QuotaUsageEntity> captor = ArgumentCaptor.forClass(QuotaUsageEntity.class);
        verify(quotaUsageRepository).save(captor.capture());
        assertThat(captor.getValue().getChatCount()).isEqualTo(1);
    }

    @Test
    void checkAndConsume_freeUserAtLimit_throwsQuotaExceeded() {
        UserEntity user = user("free-user", SubscriptionTier.FREE);
        QuotaUsageEntity usage = new QuotaUsageEntity();
        usage.setId(new QuotaUsageId("free-user", LocalDate.now()));
        usage.setChatCount(2);

        when(userRepository.findById("free-user")).thenReturn(Optional.of(user));
        when(quotaUsageRepository.findById(any())).thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> quotaService.checkAndConsume("free-user"))
                .isInstanceOf(QuotaExceededException.class);
        verify(quotaUsageRepository, never()).save(any());
    }

    @Test
    void checkAndConsume_vipUser_skipsQuotaTracking() {
        UserEntity user = user("vip-user", SubscriptionTier.VIP);
        when(userRepository.findById("vip-user")).thenReturn(Optional.of(user));

        quotaService.checkAndConsume("vip-user");

        verify(quotaUsageRepository, never()).findById(any());
        verify(quotaUsageRepository, never()).save(any());
    }

    @Test
    void getStatus_freeUser_returnsRemainingQuota() {
        UserEntity user = user("free-user", SubscriptionTier.FREE);
        QuotaUsageEntity usage = new QuotaUsageEntity();
        usage.setId(new QuotaUsageId("free-user", LocalDate.now()));
        usage.setChatCount(1);

        when(userRepository.findById("free-user")).thenReturn(Optional.of(user));
        when(quotaUsageRepository.findById(any())).thenReturn(Optional.of(usage));

        QuotaStatus status = quotaService.getStatus("free-user");

        assertThat(status.tier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(status.dailyLimit()).isEqualTo(2);
        assertThat(status.used()).isEqualTo(1);
        assertThat(status.remaining()).isEqualTo(1);
    }

    @Test
    void getStatus_unknownUser_throwsUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotaService.getStatus("missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    private static UserEntity user(String id, SubscriptionTier tier) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSubscriptionTier(tier);
        return user;
    }

    private static TickMineProperties.QuotaConfig quotaConfig(int dailyChatLimit) {
        TickMineProperties.QuotaConfig config = new TickMineProperties.QuotaConfig();
        config.setDailyChatLimit(dailyChatLimit);
        return config;
    }
}
