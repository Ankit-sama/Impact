package com.impact.notification;

import com.impact.auth.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PostMapping("/notifications/{id}/read")
    public String markReadAndRedirect(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String link = notificationService.markAsReadAndGetLink(id, userDetails.getUser().getId());
        return "redirect:" + link;
    }

    // Standard fallback endpoint
    @PostMapping("/notifications/mark-all-read")
    public String markAllAsRead(Authentication authentication, RedirectAttributes ra) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        notificationService.markAllAsRead(userDetails.getUser().getId());
        ra.addFlashAttribute("successMsg", "All notifications marked as read.");
        return "redirect:/notifications";
    }

    // NEW: Hidden API endpoint for the in-page JavaScript fetch request
    @PostMapping("/notifications/api/mark-all-read")
    @ResponseBody
    public String markAllAsReadApi(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            notificationService.markAllAsRead(userDetails.getUser().getId());
            return "{\"status\":\"success\"}";
        }
        return "{\"status\":\"error\"}";
    }

    @GetMapping("/notifications/unread-count")
    @ResponseBody
    public long getUnreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return 0;
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return notificationService.getUnreadCount(userDetails.getUser().getId());
    }
}