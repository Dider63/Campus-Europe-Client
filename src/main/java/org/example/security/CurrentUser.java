package org.example.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}
    public static CustomUserDetails get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("An authenticated user is required.");
        }
        return user;
    }
    public static long id() { return get().getUserId(); }
    public static boolean hasRole(String role) { return get().hasRole(role); }
}
