package com.impact.auth;

import com.impact.user.Role;
import com.impact.user.UserStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserStatus status = userDetails.getUser().getStatus();

        if (status == UserStatus.PENDING_VERIFICATION) {
            response.sendRedirect("/otp");
            return;
        }

        Role role = userDetails.getUser().getRole();
        if (role == Role.ROLE_VOLUNTEER) {
            response.sendRedirect("/volunteer/dashboard");
        } else if (role == Role.ROLE_ORG_MANAGER) {
            response.sendRedirect("/manager/dashboard");
        } else if (role == Role.ROLE_ADMIN) {
            response.sendRedirect("/admin/dashboard");
        } else {
            response.sendRedirect("/");
        }
    }
}