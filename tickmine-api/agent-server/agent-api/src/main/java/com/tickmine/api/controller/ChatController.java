package com.tickmine.api.controller;

import com.tickmine.api.dto.ChatRequest;
import com.tickmine.api.dto.ChatResponseDto;
import com.tickmine.api.dto.DtoMapper;
import com.tickmine.api.sse.ChatSseEmitter;
import com.tickmine.domain.model.ChatResponse;
import com.tickmine.api.security.AuthContext;
import com.tickmine.infra.service.ChatStreamPrepareResult;
import com.tickmine.infra.service.GoalAgentService;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final GoalAgentService goalAgentService;
    private final ChatSseEmitter chatSseEmitter;
    private final AuthContext authContext;
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @PostMapping
    public ChatResponseDto chat(@RequestBody ChatRequest request) {
        authContext.requireSameUser(request.userId());
        return DtoMapper.toDto(goalAgentService.handleChat(
                request.userId(),
                request.message(),
                request.goalId()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        authContext.requireSameUser(request.userId());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        streamExecutor.execute(() -> runStreamChat(request, emitter));
        return emitter;
    }

    private void runStreamChat(ChatRequest request, SseEmitter emitter) {
        try {
            ChatStreamPrepareResult prep = goalAgentService.prepareStreamChat(
                    request.userId(),
                    request.message(),
                    request.goalId());
            streamReply(prep, emitter);
        } catch (Exception exception) {
            log.warn("Chat stream prepare failed userId={}", request.userId(), exception);
            completeWithErrorEvent(emitter, exception);
        }
    }

    private void streamReply(ChatStreamPrepareResult prep, SseEmitter emitter) {
        StringBuilder fullReply = new StringBuilder();
        try {
            goalAgentService.emitStreamReply(prep, chunk -> {
                fullReply.append(chunk);
                try {
                    chatSseEmitter.sendDelta(emitter, chunk);
                } catch (IOException exception) {
                    throw new IllegalStateException("SSE delta send failed", exception);
                }
            });
            ChatResponse response = goalAgentService.finalizeStreamChat(prep, fullReply.toString());
            chatSseEmitter.sendDone(emitter, response);
            emitter.complete();
        } catch (Exception exception) {
            log.error("Chat stream failed goalId={}", prep.goal().getId(), exception);
            completeWithErrorEvent(emitter, exception);
        }
    }

    private void completeWithErrorEvent(SseEmitter emitter, Exception exception) {
        try {
            chatSseEmitter.sendError(emitter, exception);
            emitter.complete();
        } catch (IOException ioException) {
            log.warn("Failed to send SSE error event", ioException);
            emitter.completeWithError(ioException);
        }
    }
}
