package com.tickmine.api.exception;

import com.tickmine.api.dto.ErrorResponse;
import com.tickmine.domain.exception.AccessDeniedException;
import com.tickmine.domain.exception.EmailAlreadyExistsException;
import com.tickmine.domain.exception.InvalidCredentialsException;
import com.tickmine.domain.exception.GoalNotFoundException;
import com.tickmine.domain.exception.InvalidGoalPhaseException;
import com.tickmine.domain.exception.QuotaExceededException;
import com.tickmine.domain.exception.TickTickNotConnectedException;
import com.tickmine.domain.exception.TickTickTokenInvalidException;
import com.tickmine.domain.exception.UserNotFoundException;
import com.tickmine.mcp.exception.TickTickApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QuotaExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleQuotaExceeded(QuotaExceededException exception) {
        return new ErrorResponse("QUOTA_EXCEEDED", exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFound(UserNotFoundException exception) {
        return new ErrorResponse("USER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyExists(EmailAlreadyExistsException exception) {
        return new ErrorResponse("EMAIL_ALREADY_EXISTS", "该邮箱已被注册");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException exception) {
        return new ErrorResponse("INVALID_CREDENTIALS", "邮箱或密码错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException exception) {
        return new ErrorResponse("ACCESS_DENIED", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ErrorResponse("VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(GoalNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleGoalNotFound(GoalNotFoundException exception) {
        return new ErrorResponse("GOAL_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidGoalPhaseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidGoalPhase(InvalidGoalPhaseException exception) {
        return new ErrorResponse("INVALID_PHASE", exception.getMessage());
    }

    @ExceptionHandler(TickTickNotConnectedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTickTickNotConnected(TickTickNotConnectedException exception) {
        return new ErrorResponse("TICKTICK_NOT_CONNECTED", exception.getMessage());
    }

    @ExceptionHandler(TickTickTokenInvalidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTickTickTokenInvalid(TickTickTokenInvalidException exception) {
        return new ErrorResponse("TICKTICK_TOKEN_INVALID", exception.getMessage());
    }

    @ExceptionHandler(TickTickApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleTickTickApi(TickTickApiException exception) {
        if (exception.getStatusCode().value() == 401) {
            return new ErrorResponse(
                    "TICKTICK_TOKEN_INVALID",
                    "滴答 API 口令已失效或未授权。请到设置页重新获取并绑定 API 口令。");
        }
        return new ErrorResponse("TICKTICK_API_ERROR", exception.getMessage());
    }
}
