package com.tickmine.llm;

import com.tickmine.domain.port.ChatAssistant;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService implements ChatAssistant {

    private static final int LOG_MAX_LEN = 500;

    private final ModelResolver modelResolver;
    private final MeterRegistry meterRegistry;

    @Override
    public String chat(String userId, String systemPrompt, String userPrompt) {
        Timer.Sample sample = Timer.start(meterRegistry);
        long startMs = System.currentTimeMillis();
        log.info(
                "LLM chat start userId={} systemPrompt={} userPrompt={}",
                userId,
                truncate(systemPrompt),
                truncate(userPrompt));
        try {
            ChatModel model = modelResolver.resolve(userId);
            String modelName = modelResolver.resolveModelName(userId);
            var tier = modelResolver.resolveTier(userId);
            String response = ChatClient.create(model)
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            meterRegistry.counter("tickmine.llm.calls", "tier", tier.name()).increment();
            log.info(
                    "LLM chat ok userId={} model={} tier={} durationMs={} response={}",
                    userId,
                    modelName,
                    tier,
                    System.currentTimeMillis() - startMs,
                    truncate(response));
            return response;
        } catch (RuntimeException e) {
            log.error(
                    "LLM chat failed userId={} durationMs={}",
                    userId,
                    System.currentTimeMillis() - startMs,
                    e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("tickmine.llm.duration"));
        }
    }

    @Override
    public void streamChat(String userId, String systemPrompt, String userPrompt, Consumer<String> chunkConsumer) {
        Timer.Sample sample = Timer.start(meterRegistry);
        long startMs = System.currentTimeMillis();
        log.info(
                "LLM streamChat start userId={} systemPrompt={} userPrompt={}",
                userId,
                truncate(systemPrompt),
                truncate(userPrompt));
        try {
            ChatModel model = modelResolver.resolve(userId);
            String modelName = modelResolver.resolveModelName(userId);
            var tier = modelResolver.resolveTier(userId);
            Flux<String> flux = ChatClient.create(model)
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .stream()
                    .content();
            flux.doOnNext(chunkConsumer).blockLast();
            meterRegistry.counter("tickmine.llm.calls", "tier", tier.name()).increment();
            log.info(
                    "LLM streamChat ok userId={} model={} tier={} durationMs={}",
                    userId,
                    modelName,
                    tier,
                    System.currentTimeMillis() - startMs);
        } catch (RuntimeException e) {
            log.error(
                    "LLM streamChat failed userId={} durationMs={}",
                    userId,
                    System.currentTimeMillis() - startMs,
                    e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("tickmine.llm.duration"));
        }
    }

    public <T> T structuredOutput(String userId, String systemPrompt, String userPrompt, Class<T> type) {
        Timer.Sample sample = Timer.start(meterRegistry);
        long startMs = System.currentTimeMillis();
        log.info(
                "LLM structuredOutput start userId={} outputType={} systemPrompt={} userPrompt={}",
                userId,
                type.getSimpleName(),
                truncate(systemPrompt),
                truncate(userPrompt));
        try {
            ChatModel model = modelResolver.resolve(userId);
            String modelName = modelResolver.resolveModelName(userId);
            var tier = modelResolver.resolveTier(userId);
            T result = ChatClient.create(model)
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(type);
            meterRegistry.counter("tickmine.llm.calls", "tier", tier.name()).increment();
            log.info(
                    "LLM structuredOutput ok userId={} model={} tier={} outputType={} durationMs={} result={}",
                    userId,
                    modelName,
                    tier,
                    type.getSimpleName(),
                    System.currentTimeMillis() - startMs,
                    truncate(String.valueOf(result)));
            return result;
        } catch (RuntimeException e) {
            log.error(
                    "LLM structuredOutput failed userId={} outputType={} durationMs={}",
                    userId,
                    type.getSimpleName(),
                    System.currentTimeMillis() - startMs,
                    e);
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("tickmine.llm.duration"));
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').trim();
        return normalized.length() <= LOG_MAX_LEN
                ? normalized
                : normalized.substring(0, LOG_MAX_LEN) + "...";
    }
}
