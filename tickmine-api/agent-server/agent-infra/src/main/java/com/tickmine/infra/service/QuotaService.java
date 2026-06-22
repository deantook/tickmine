package com.tickmine.infra.service;

import com.tickmine.domain.model.ChatMessage;
import com.tickmine.domain.model.QuotaStatus;
import com.tickmine.domain.model.SubscriptionTier;
import com.tickmine.domain.exception.QuotaExceededException;
import com.tickmine.domain.exception.UserNotFoundException;
import com.tickmine.infra.config.TickMineProperties;
import com.tickmine.infra.persistence.entity.QuotaUsageEntity;
import com.tickmine.infra.persistence.entity.QuotaUsageId;
import com.tickmine.infra.persistence.entity.UserEntity;
import com.tickmine.infra.persistence.repository.QuotaUsageRepository;
import com.tickmine.infra.persistence.repository.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserRepository userRepository;
    private final QuotaUsageRepository quotaUsageRepository;
    private final TickMineProperties props;

    @Transactional
    public void checkAndConsume(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        int limit = dailyLimit(user.getSubscriptionTier());
        if (limit < 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        QuotaUsageEntity usage = quotaUsageRepository
                .findById(new QuotaUsageId(userId, today))
                .orElseGet(() -> newUsage(userId, today));

        if (usage.getChatCount() >= limit) {
            throw new QuotaExceededException(userId, limit);
        }
        usage.setChatCount(usage.getChatCount() + 1);
        quotaUsageRepository.save(usage);
    }

    @Transactional(readOnly = true)
    public QuotaStatus getStatus(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        int limit = dailyLimit(user.getSubscriptionTier());
        if (limit < 0) {
            return new QuotaStatus(user.getSubscriptionTier(), -1, 0, -1);
        }

        int used = quotaUsageRepository
                .findById(new QuotaUsageId(userId, LocalDate.now()))
                .map(QuotaUsageEntity::getChatCount)
                .orElse(0);
        return new QuotaStatus(user.getSubscriptionTier(), limit, used, Math.max(0, limit - used));
    }

    private int dailyLimit(SubscriptionTier tier) {
        TickMineProperties.QuotaConfig config = props.getQuota().get(tier.name());
        return config != null ? config.getDailyChatLimit() : 0;
    }

    private static QuotaUsageEntity newUsage(String userId, LocalDate today) {
        QuotaUsageEntity usage = new QuotaUsageEntity();
        usage.setId(new QuotaUsageId(userId, today));
        usage.setChatCount(0);
        return usage;
    }
}
