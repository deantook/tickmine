package com.tickmine.domain.model;

public record QuotaStatus(
        SubscriptionTier tier,
        int dailyLimit,
        int used,
        int remaining
) {
}
