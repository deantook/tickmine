package com.tickmine.infra.auth;

import com.tickmine.infra.config.TickMineProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long tokenTtlHours;

    public JwtService(TickMineProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.getAuth().getJwtSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.tokenTtlHours = properties.getAuth().getTokenTtlHours();
    }

    public IssuedToken issueToken(String userId, String email) {
        String tokenId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(tokenTtlHours * 3600);
        String token = Jwts.builder()
                .id(tokenId)
                .subject(userId)
                .claim("email", email)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, tokenId, expiresAt);
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.getId(),
                claims.getExpiration().toInstant());
    }

    public record IssuedToken(String token, String tokenId, Instant expiresAt) {}
}
