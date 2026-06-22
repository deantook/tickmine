package com.tickmine.api.controller;

import com.tickmine.api.dto.AuthResponse;
import com.tickmine.api.dto.LoginRequest;
import com.tickmine.api.dto.MeResponse;
import com.tickmine.api.dto.RegisterRequest;
import com.tickmine.api.security.AuthContext;
import com.tickmine.infra.auth.AuthService;
import com.tickmine.infra.auth.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthContext authContext;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request.email(), request.password());
        return toResponse(result);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request.email(), request.password());
        return toResponse(result);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        AuthenticatedUser user = authContext.requireCurrentUser();
        authService.logout(user);
    }

    @GetMapping("/me")
    public MeResponse me() {
        AuthenticatedUser user = authContext.requireCurrentUser();
        return new MeResponse(user.userId(), user.email());
    }

    private static AuthResponse toResponse(AuthService.AuthResult result) {
        return new AuthResponse(
                result.accessToken(), result.userId(), result.email(), result.expiresAt());
    }
}
