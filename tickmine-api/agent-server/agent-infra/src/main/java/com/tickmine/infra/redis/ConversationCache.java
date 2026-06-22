package com.tickmine.infra.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickmine.domain.model.ChatMessage;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationCache {

    private static final int MAX_MESSAGES = 20;
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public List<ChatMessage> get(String userId, UUID goalId) {
        String json = redis.opsForValue().get(key(userId, goalId));
        if (json == null) {
            return null;
        }
        return parse(json);
    }

    public void put(String userId, UUID goalId, List<ChatMessage> messages) {
        List<ChatMessage> trimmed = messages.size() > MAX_MESSAGES
                ? messages.subList(messages.size() - MAX_MESSAGES, messages.size())
                : messages;
        redis.opsForValue().set(key(userId, goalId), serialize(trimmed), TTL);
    }

    public void evict(String userId, UUID goalId) {
        redis.delete(key(userId, goalId));
    }

    public void evictAllForUser(String userId) {
        var keys = redis.keys("conversation:" + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    private String key(String userId, UUID goalId) {
        return "conversation:" + userId + ":" + goalId;
    }

    private String serialize(List<ChatMessage> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize conversation messages", e);
        }
    }

    private List<ChatMessage> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse conversation messages", e);
        }
    }
}
