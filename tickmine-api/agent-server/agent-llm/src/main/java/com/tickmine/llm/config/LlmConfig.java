package com.tickmine.llm.config;

import com.tickmine.infra.config.TickMineProperties;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TickMineProperties.class)
public class LlmConfig {

    @Bean
    public ChatModel deepseekChatModel(TickMineProperties props) {
        var cfg = props.getModels().get("FREE");
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(cfg.getBaseUrl())
                        .apiKey(props.getLlm().getDeepseekApiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(cfg.getModel())
                        .build())
                .build();
    }

    @Bean
    public ChatModel qwenChatModel(TickMineProperties props) {
        var cfg = props.getModels().get("SVIP");
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(cfg.getBaseUrl())
                        .apiKey(props.getLlm().getQwenApiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(cfg.getModel())
                        .build())
                .build();
    }
}
