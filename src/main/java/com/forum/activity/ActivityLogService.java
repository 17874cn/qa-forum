package com.forum.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }


    public void log(String userId, ActivityType type, String description,
                    String resourceId, Map<String, String> metadata) {
        ActivityLog entry = new ActivityLog();
        entry.setUserId(userId);
        entry.setActivityType(type);
        entry.setDescription(description);
        entry.setResourceId(resourceId);
        entry.setMetadata(metadata);

        activityLogRepository.save(entry)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        saved -> log.debug("Activity logged: user={}, type={}, resource={}",
                                userId, type, resourceId),
                        error -> log.error("Failed to log activity: user={}, type={}",
                                userId, type, error)
                );
    }

    public void log(String userId, ActivityType type, String description) {
        log(userId, type, description, null, null);
    }

    public void log(String userId, ActivityType type, String description, String resourceId) {
        log(userId, type, description, resourceId, null);
    }

    // --- Query methods ---

    public Flux<ActivityLog> getUserActivity(String userId) {
        return activityLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public Flux<ActivityLog> getUserActivityByType(String userId, ActivityType type) {
        return activityLogRepository.findByUserIdAndActivityTypeOrderByTimestampDesc(userId, type);
    }

    public Flux<ActivityLog> getUserActivityToday(String userId) {
        Instant startOfDay = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = Instant.now();
        return activityLogRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
                userId, startOfDay, now);
    }

    public Flux<ActivityLog> getActivityByType(ActivityType type) {
        return activityLogRepository.findByActivityTypeOrderByTimestampDesc(type);
    }
}
