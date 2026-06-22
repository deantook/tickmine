package com.tickmine.api.security;

import com.tickmine.domain.exception.AccessDeniedException;
import com.tickmine.infra.auth.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {

    public AuthenticatedUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Authentication required");
        }
        return user;
    }

    public void requireSameUser(String userId) {
        AuthenticatedUser current = requireCurrentUser();
        if (!current.userId().equals(userId)) {
            throw new AccessDeniedException("Cannot access another user's resources");
        }
    }
}
