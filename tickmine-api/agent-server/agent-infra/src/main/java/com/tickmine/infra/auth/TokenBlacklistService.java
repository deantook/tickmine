package com.tickmine.infra.auth;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void blacklist(String tokenId, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redis.opsForValue().set(KEY_PREFIX + tokenId, "1", ttl);
    }

    public boolean isBlacklisted(String tokenId) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + tokenId));
    }
}
