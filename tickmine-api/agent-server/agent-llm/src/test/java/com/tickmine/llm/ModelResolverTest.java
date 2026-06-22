package com.tickmine.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tickmine.domain.model.SubscriptionTier;
import com.tickmine.infra.config.TickMineProperties;
import com.tickmine.infra.config.TickMineProperties.ModelConfig;
import com.tickmine.infra.persistence.entity.UserEntity;
import com.tickmine.infra.persistence.repository.UserRepository;
import com.tickmine.llm.exception.UserNotFoundException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

@ExtendWith(MockitoExtension.class)
class ModelResolverTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatModel deepseekChatModel;

    @Mock
    private ChatModel qwenChatModel;

    private TickMineProperties props;
    private ModelResolver modelResolver;

    @BeforeEach
    void setUp() {
        props = new TickMineProperties();
        props.setModels(Map.of(
                "FREE", modelConfig("deepseek", "deepseek-chat"),
                "VIP", modelConfig("deepseek", "deepseek-chat"),
                "SVIP", modelConfig("qwen", "qwen-plus")));
        modelResolver = new ModelResolver(userRepository, deepseekChatModel, qwenChatModel, props);
    }

    @Test
    void resolve_freeUser_returnsDeepseekModel() {
        when(userRepository.findById("user-free")).thenReturn(Optional.of(user("user-free", SubscriptionTier.FREE)));

        assertThat(modelResolver.resolve("user-free")).isSameAs(deepseekChatModel);
        assertThat(modelResolver.resolveModelName("user-free")).isEqualTo("deepseek-chat");
    }

    @Test
    void resolve_vipUser_returnsDeepseekModel() {
        when(userRepository.findById("user-vip")).thenReturn(Optional.of(user("user-vip", SubscriptionTier.VIP)));

        assertThat(modelResolver.resolve("user-vip")).isSameAs(deepseekChatModel);
        assertThat(modelResolver.resolveModelName("user-vip")).isEqualTo("deepseek-chat");
    }

    @Test
    void resolve_svipUser_returnsQwenModel() {
        when(userRepository.findById("user-svip")).thenReturn(Optional.of(user("user-svip", SubscriptionTier.SVIP)));

        assertThat(modelResolver.resolve("user-svip")).isSameAs(qwenChatModel);
        assertThat(modelResolver.resolveModelName("user-svip")).isEqualTo("qwen-plus");
    }

    @Test
    void resolve_unknownUser_throwsUserNotFoundException() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modelResolver.resolve("missing"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("missing");
    }

    private static UserEntity user(String id, SubscriptionTier tier) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setSubscriptionTier(tier);
        return user;
    }

    private static ModelConfig modelConfig(String provider, String model) {
        ModelConfig config = new ModelConfig();
        config.setProvider(provider);
        config.setModel(model);
        return config;
    }
}
