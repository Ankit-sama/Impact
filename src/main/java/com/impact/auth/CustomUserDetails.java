package com.impact.auth;

import com.impact.user.User;
import com.impact.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // If the user is active, grant their actual role.
        // If pending, grant ROLE_PENDING so they can only access the OTP page.
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_PENDING"));
        } else if (user.getStatus() == UserStatus.ACTIVE) {
            return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));
        }
        return Collections.emptyList();
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }
    @Override
    public String getUsername() { return user.getEmail(); }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return user.getStatus() != UserStatus.INACTIVE; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return user.getStatus() != UserStatus.INACTIVE; }
}