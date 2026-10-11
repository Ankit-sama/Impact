package com.impact.admin;

import com.impact.config.AppClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final AppClock appClock;

    public ActivityLogService(ActivityLogRepository activityLogRepository, AppClock appClock) {
        this.activityLogRepository = activityLogRepository;
        this.appClock = appClock;
    }

    @Transactional
    public void logAction(Long actorId, String action, String targetType, Long targetId, String details) {
        ActivityLog log = new ActivityLog();
        log.setActorId(actorId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setCreatedAt(appClock.now());
        activityLogRepository.save(log);
    }
}