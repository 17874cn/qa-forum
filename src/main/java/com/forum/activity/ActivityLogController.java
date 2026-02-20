package com.forum.activity;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/{userId}")
    public Flux<ActivityLog> getUserActivity(@PathVariable String userId) {
        return activityLogService.getUserActivity(userId);
    }

    @GetMapping("/{userId}/today")
    public Flux<ActivityLog> getUserActivityToday(@PathVariable String userId) {
        return activityLogService.getUserActivityToday(userId);
    }

    @GetMapping("/{userId}/type/{type}")
    public Flux<ActivityLog> getUserActivityByType(@PathVariable String userId,
                                                   @PathVariable ActivityType type) {
        return activityLogService.getUserActivityByType(userId, type);
    }
}
