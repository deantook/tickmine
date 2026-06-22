package com.tickmine.api.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickmine.api.dto.ChatResponseDto;
import com.tickmine.api.dto.DtoMapper;
import com.tickmine.domain.exception.AccessDeniedException;
import com.tickmine.domain.exception.GoalNotFoundException;
import com.tickmine.domain.exception.InvalidGoalPhaseException;
import com.tickmine.domain.exception.QuotaExceededException;
import com.tickmine.domain.exception.TickTickNotConnectedException;
import com.tickmine.domain.exception.TickTickTokenInvalidException;
import com.tickmine.domain.exception.UserNotFoundException;
import com.tickmine.domain.model.ChatResponse;
import com.tickmine.mcp.exception.TickTickApiException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class ChatSseEmitter {

    private final ObjectMapper objectMapper;

    public void sendDelta(SseEmitter emitter, String text) throws IOException {
        emitter.send(SseEmitter.event()
                .name("delta")
                .data(objectMapper.writeValueAsString(new ChatDeltaEvent(text)), MediaType.APPLICATION_JSON));
    }

    public void sendDone(SseEmitter emitter, ChatResponse response) throws IOException {
        ChatResponseDto dto = DtoMapper.toDto(response);
        emitter.send(SseEmitter.event()
                .name("done")
                .data(objectMapper.writeValueAsString(dto), MediaType.APPLICATION_JSON));
    }

    public void sendError(SseEmitter emitter, Exception exception) throws IOException {
        emitter.send(SseEmitter.event()
                .name("error")
                .data(objectMapper.writeValueAsString(toErrorEvent(exception)), MediaType.APPLICATION_JSON));
    }

    private static ChatErrorEvent toErrorEvent(Exception exception) {
        if (exception instanceof QuotaExceededException quotaExceeded) {
            return new ChatErrorEvent(
                    "QUOTA_EXCEEDED", quotaExceeded.getMessage(), HttpStatus.TOO_MANY_REQUESTS.value());
        }
        if (exception instanceof GoalNotFoundException goalNotFound) {
            return new ChatErrorEvent(
                    "GOAL_NOT_FOUND", goalNotFound.getMessage(), HttpStatus.NOT_FOUND.value());
        }
        if (exception instanceof UserNotFoundException userNotFound) {
            return new ChatErrorEvent(
                    "USER_NOT_FOUND", userNotFound.getMessage(), HttpStatus.NOT_FOUND.value());
        }
        if (exception instanceof TickTickNotConnectedException notConnected) {
            return new ChatErrorEvent(
                    "TICKTICK_NOT_CONNECTED", notConnected.getMessage(), HttpStatus.BAD_REQUEST.value());
        }
        if (exception instanceof TickTickTokenInvalidException tokenInvalid) {
            return new ChatErrorEvent(
                    "TICKTICK_TOKEN_INVALID", tokenInvalid.getMessage(), HttpStatus.BAD_REQUEST.value());
        }
        if (exception instanceof InvalidGoalPhaseException invalidPhase) {
            return new ChatErrorEvent(
                    "INVALID_PHASE", invalidPhase.getMessage(), HttpStatus.CONFLICT.value());
        }
        if (exception instanceof TickTickApiException tickTickApi) {
            if (tickTickApi.getStatusCode().value() == 401) {
                return new ChatErrorEvent(
                        "TICKTICK_TOKEN_INVALID",
                        "滴答 API 口令已失效或未授权。请到设置页重新获取并绑定 API 口令。",
                        HttpStatus.BAD_REQUEST.value());
            }
            return new ChatErrorEvent(
                    "TICKTICK_API_ERROR", tickTickApi.getMessage(), HttpStatus.BAD_GATEWAY.value());
        }
        if (exception instanceof AccessDeniedException accessDenied) {
            return new ChatErrorEvent(
                    "ACCESS_DENIED", accessDenied.getMessage(), HttpStatus.FORBIDDEN.value());
        }
        if (exception instanceof IllegalArgumentException illegalArgument) {
            return new ChatErrorEvent(
                    "VALIDATION_ERROR", illegalArgument.getMessage(), HttpStatus.BAD_REQUEST.value());
        }
        if (exception instanceof ResourceAccessException) {
            return new ChatErrorEvent(
                    "LLM_TIMEOUT",
                    "AI 服务响应超时，请稍后重试",
                    HttpStatus.GATEWAY_TIMEOUT.value());
        }
        return new ChatErrorEvent(
                "UNKNOWN",
                exception.getMessage() != null ? exception.getMessage() : "流式响应失败",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
