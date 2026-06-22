package com.tickmine.llm.config;

import com.tickmine.infra.config.TickMineProperties;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableConfigurationProperties(TickMineProperties.class)
@RequiredArgsConstructor
public class LlmConfig {

    private final RestClient.Builder llmRestClientBuilder;

    @Bean
    public ChatModel deepseekChatModel(TickMineProperties props) {
        var cfg = props.getModels().get("FREE");
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(cfg.getBaseUrl())
                        .apiKey(props.getLlm().getDeepseekApiKey())
                        .restClientBuilder(llmRestClientBuilder)
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(cfg.getModel())
                        .build())
                .build();
    }
}
