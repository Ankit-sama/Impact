package com.impact.notification;

import com.impact.auth.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String listNotifications(Model model, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        model.addAttribute("notifications", notificationService.getUserNotifications(userDetails.getUser().getId()));
        return "notifications";
    }

    @GetMapping("/notifications/read/{id}")
    public String markReadAndRedirect(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String link = notificationService.markAsReadAndGetLink(id, userDetails.getUser().getId());
        return "redirect:" + link;
    }
}