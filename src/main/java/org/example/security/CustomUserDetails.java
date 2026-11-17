package org.example.security;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class CustomUserDetails implements UserDetails {
    private final long userId;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String email;
    private final String accountStatus;
    private final List<String> roles;

    public CustomUserDetails(com.nahidglobal.entity.UserAccount user, List<String> roles) {
        this.userId = user.userId();
        this.username = user.username();
        this.passwordHash = user.passwordHash();
        this.fullName = user.fullName();
        this.email = user.email();
        this.accountStatus = user.accountStatus();
        this.roles = List.copyOf(roles);
    }

    public long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public List<String> getRoleNames() { return roles; }
    public boolean hasRole(String role) { return roles.contains(role); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !"LOCKED".equals(accountStatus); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return "ACTIVE".equals(accountStatus); }
}

