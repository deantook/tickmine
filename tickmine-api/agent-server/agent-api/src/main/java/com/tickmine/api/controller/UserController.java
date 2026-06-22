package com.tickmine.api.controller;

import com.tickmine.api.dto.BindTokenRequest;
import com.tickmine.api.dto.BindTokenResponse;
import com.tickmine.api.dto.DtoMapper;
import com.tickmine.api.dto.GoalSummaryDto;
import com.tickmine.api.dto.QuotaResponseDto;
import com.tickmine.api.dto.TokenStatusDto;
import com.tickmine.domain.model.TokenStatus;
import com.tickmine.api.security.AuthContext;
import com.tickmine.infra.auth.AuthenticatedUser;
import com.tickmine.infra.service.GoalAgentService;
import com.tickmine.infra.service.QuotaService;
import com.tickmine.infra.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final QuotaService quotaService;
    private final GoalAgentService goalAgentService;
    private final AuthContext authContext;

    @PutMapping("/me/ticktick-token")
    public BindTokenResponse bindMyToken(@RequestBody BindTokenRequest request) {
        AuthenticatedUser user = authContext.requireCurrentUser();
        userService.bindTickTickToken(user.userId(), request.token());
        return new BindTokenResponse(TokenStatus.CONNECTED.name());
    }

    @GetMapping("/me/ticktick-token/status")
    public TokenStatusDto myTokenStatus() {
        AuthenticatedUser user = authContext.requireCurrentUser();
        TokenStatus status = userService.getTokenStatus(user.userId());
        return new TokenStatusDto(status == TokenStatus.CONNECTED);
    }

    @GetMapping("/me/quota")
    public QuotaResponseDto myQuota() {
        AuthenticatedUser user = authContext.requireCurrentUser();
        return DtoMapper.toDto(quotaService.getStatus(user.userId()));
    }

    @GetMapping("/me/goals")
    public List<GoalSummaryDto> myGoals() {
        AuthenticatedUser user = authContext.requireCurrentUser();
        return goalAgentService.listGoals(user.userId()).stream()
                .map(DtoMapper::toSummaryDto)
                .toList();
    }

    @DeleteMapping("/me/goals")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMyGoals() {
        AuthenticatedUser user = authContext.requireCurrentUser();
        goalAgentService.deleteAllGoals(user.userId());
    }

    /** @deprecated Use {@link #bindMyToken(BindTokenRequest)} */
    @Deprecated
    @PutMapping("/{userId}/ticktick-token")
    public BindTokenResponse bindToken(
            @PathVariable String userId, @RequestBody BindTokenRequest request) {
        authContext.requireSameUser(userId);
        userService.bindTickTickToken(userId, request.token());
        return new BindTokenResponse(TokenStatus.CONNECTED.name());
    }

    /** @deprecated Use {@link #myTokenStatus()} */
    @Deprecated
    @GetMapping("/{userId}/ticktick-token/status")
    public TokenStatusDto tokenStatus(@PathVariable String userId) {
        authContext.requireSameUser(userId);
        TokenStatus status = userService.getTokenStatus(userId);
        return new TokenStatusDto(status == TokenStatus.CONNECTED);
    }

    /** @deprecated Use {@link #myQuota()} */
    @Deprecated
    @GetMapping("/{userId}/quota")
    public QuotaResponseDto quota(@PathVariable String userId) {
        authContext.requireSameUser(userId);
        return DtoMapper.toDto(quotaService.getStatus(userId));
    }
}
