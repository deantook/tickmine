package com.tickmine.infra.service;

import com.tickmine.domain.exception.TickTickNotConnectedException;
import com.tickmine.domain.exception.TickTickTokenInvalidException;
import com.tickmine.domain.exception.UserNotFoundException;
import com.tickmine.domain.model.TokenStatus;
import com.tickmine.domain.port.TickTickTokenValidator;
import com.tickmine.infra.crypto.TokenEncryptor;
import com.tickmine.infra.persistence.entity.UserEntity;
import com.tickmine.infra.persistence.mapper.DomainMapper;
import com.tickmine.infra.persistence.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenEncryptor tokenEncryptor;
    private final DomainMapper domainMapper;
    private final TickTickTokenValidator tokenValidator;

    @Transactional
    public void bindTickTickToken(String userId, String plainToken) {
        String trimmed = plainToken != null ? plainToken.trim() : "";
        tokenValidator.validate(trimmed);

        UserEntity user = findOrCreate(userId);
        user.setTicktickTokenEnc(tokenEncryptor.encrypt(trimmed));
        user.setTokenStatus(TokenStatus.CONNECTED);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void invalidateToken(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setTicktickTokenEnc(null);
            user.setTokenStatus(TokenStatus.NOT_CONNECTED);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public String getDecryptedToken(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getTokenStatus() != TokenStatus.CONNECTED || user.getTicktickTokenEnc() == null) {
            throw new TickTickNotConnectedException(userId);
        }
        return tokenEncryptor.decrypt(user.getTicktickTokenEnc());
    }

    @Transactional(readOnly = true)
    public TokenStatus getTokenStatus(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return user.getTokenStatus();
    }

    @Transactional
    public UserEntity findOrCreate(String userId) {
        return userRepository.findById(userId).orElseGet(() -> {
            UserEntity user = domainMapper.toNewUserEntity(userId);
            return userRepository.save(user);
        });
    }
}
