package com.tickmine.llm;

import com.tickmine.domain.model.SubscriptionTier;
import com.tickmine.infra.config.TickMineProperties;
import com.tickmine.infra.config.TickMineProperties.ModelConfig;
import com.tickmine.infra.persistence.entity.UserEntity;
import com.tickmine.infra.persistence.repository.UserRepository;
import com.tickmine.llm.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModelResolver {

    private final UserRepository userRepository;
    @Qualifier("deepseekChatModel")
    private final ChatModel deepseekChatModel;
    private final TickMineProperties props;

    public ChatModel resolve(String userId) {
        requireUser(userId);
        return deepseekChatModel;
    }

    public String resolveModelName(String userId) {
        UserEntity user = requireUser(userId);
        return props.getModels().get(user.getSubscriptionTier().name()).getModel();
    }

    public SubscriptionTier resolveTier(String userId) {
        return requireUser(userId).getSubscriptionTier();
    }

    public String resolveModelNameForTier(SubscriptionTier tier) {
        return props.getModels().get(tier.name()).getModel();
    }

    private UserEntity requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
