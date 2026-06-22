package com.tickmine.llm.config;

import com.tickmine.infra.config.TickMineProperties;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class LlmHttpClientConfig {

    private final TickMineProperties props;

    @Bean
    RestClient.Builder llmRestClientBuilder() {
        LlmTimeouts timeouts = timeouts();
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                .responseTimeout(Duration.ofSeconds(timeouts.readTimeoutSeconds()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeouts.connectTimeoutMillis());

        return RestClient.builder().requestFactory(new ReactorClientHttpRequestFactory(httpClient));
    }

    private LlmTimeouts timeouts() {
        var llm = props.getLlm();
        int read = llm.getReadTimeoutSeconds() > 0 ? llm.getReadTimeoutSeconds() : 180;
        int connect = llm.getConnectTimeoutSeconds() > 0 ? llm.getConnectTimeoutSeconds() : 30;
        return new LlmTimeouts(read, connect);
    }

    private record LlmTimeouts(int readTimeoutSeconds, int connectTimeoutSeconds) {
        int connectTimeoutMillis() {
            return connectTimeoutSeconds * 1000;
        }
    }
}
