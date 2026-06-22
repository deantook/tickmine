package com.tickmine.infra.auth;

import java.time.Instant;

public record AuthenticatedUser(String userId, String email, String tokenId, Instant expiresAt) {}
