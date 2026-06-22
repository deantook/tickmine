package com.tickmine.infra.service;

import com.tickmine.domain.model.ChatMessage;
import com.tickmine.infra.persistence.entity.ConversationEntity;
import com.tickmine.infra.persistence.mapper.DomainMapper;
import com.tickmine.infra.persistence.repository.ConversationRepository;
import com.tickmine.infra.redis.ConversationCache;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationCache cache;
    private final DomainMapper mapper;

    @Transactional(readOnly = true)
    public List<ChatMessage> loadHistory(String userId, UUID goalId) {
        List<ChatMessage> cached = cache.get(userId, goalId);
        if (cached != null) {
            return cached;
        }

        List<ChatMessage> fromDb = conversationRepository.findByUserIdAndGoalId(userId, goalId)
                .map(mapper::toDomainMessages)
                .orElse(List.of());
        if (!fromDb.isEmpty()) {
            cache.put(userId, goalId, fromDb);
        }
        return fromDb;
    }

    @Transactional
    public void appendMessage(String userId, UUID goalId, ChatMessage message) {
        List<ChatMessage> history = new ArrayList<>(loadHistory(userId, goalId));
        history.add(message);

        Instant now = Instant.now();
        ConversationEntity entity = conversationRepository.findByUserIdAndGoalId(userId, goalId)
                .orElseGet(() -> {
                    ConversationEntity created = mapper.toEntity(null, userId, goalId, new ArrayList<>());
                    created.setCreatedAt(now);
                    return created;
                });
        entity.setMessages(history);
        entity.setUpdatedAt(now);
        conversationRepository.save(entity);

        cache.put(userId, goalId, history);
    }

    @Transactional
    public void deleteConversation(String userId, UUID goalId) {
        conversationRepository.deleteByUserIdAndGoalId(userId, goalId);
        cache.evict(userId, goalId);
    }

    @Transactional
    public void deleteAllForUser(String userId) {
        conversationRepository.deleteByUserId(userId);
        cache.evictAllForUser(userId);
    }
}
