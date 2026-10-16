package com.impact.notification;

import com.impact.config.AppClock;
import com.impact.user.Role;
import com.impact.user.User;
import com.impact.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AppClock appClock;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, AppClock appClock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.appClock = appClock;
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void createNotification(Long userId, String message, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setRead(false);
        notification.setCreatedAt(appClock.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyAdmins(String message, String link) {
        List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
        for (User admin : admins) {
            createNotification(admin.getId(), message, link);
        }
    }

    @Transactional
    public String markAsReadAndGetLink(Long notificationId, Long userId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                    return n.getLink() != null ? n.getLink() : "/";
                }).orElse("/");
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}