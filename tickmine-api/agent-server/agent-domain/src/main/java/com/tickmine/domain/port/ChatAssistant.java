package com.tickmine.domain.port;

import java.util.function.Consumer;

public interface ChatAssistant {

    String chat(String userId, String systemPrompt, String userPrompt);

    void streamChat(String userId, String systemPrompt, String userPrompt, Consumer<String> chunkConsumer);
}
