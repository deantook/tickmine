package com.tickmine.infra.auth;

import com.tickmine.domain.exception.EmailAlreadyExistsException;
import com.tickmine.domain.exception.InvalidCredentialsException;
import com.tickmine.domain.exception.UserNotFoundException;
import com.tickmine.infra.persistence.entity.UserEntity;
import com.tickmine.infra.persistence.mapper.DomainMapper;
import com.tickmine.infra.persistence.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DomainMapper domainMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResult register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String userId = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(password);
        UserEntity user = domainMapper.toNewUserEntity(userId, normalizedEmail, passwordHash);
        userRepository.save(user);
        return issueAuthResult(user);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueAuthResult(user);
    }

    public void logout(AuthenticatedUser user) {
        tokenBlacklistService.blacklist(user.tokenId(), Duration.between(Instant.now(), user.expiresAt()));
    }

    @Transactional(readOnly = true)
    public UserEntity requireUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private AuthResult issueAuthResult(UserEntity user) {
        JwtService.IssuedToken issued = jwtService.issueToken(user.getId(), user.getEmail());
        return new AuthResult(
                issued.token(),
                user.getId(),
                user.getEmail(),
                issued.expiresAt().toString(),
                user.getSubscriptionTier().name());
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }

    public record AuthResult(
            String accessToken,
            String userId,
            String email,
            String expiresAt,
            String subscriptionTier) {}
}
