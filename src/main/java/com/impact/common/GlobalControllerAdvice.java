package com.impact.common;

import com.impact.auth.CustomUserDetails;
import com.impact.config.AppClock;
import com.impact.notification.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final AppClock appClock;
    private final NotificationService notificationService;

    public GlobalControllerAdvice(AppClock appClock, NotificationService notificationService) {
        this.appClock = appClock;
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentUser")
    public CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) auth.getPrincipal();
        }
        return null;
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return notificationService.getUnreadCount(((CustomUserDetails) auth.getPrincipal()).getUser().getId());
        }
        return 0;
    }

    @ModelAttribute("simulatedTimeFormatted")
    public String getSimulatedTimeFormatted() {
        ZonedDateTime kolkataTime = appClock.now().atZone(ZoneId.of("Asia/Kolkata"));
        return kolkataTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }
}