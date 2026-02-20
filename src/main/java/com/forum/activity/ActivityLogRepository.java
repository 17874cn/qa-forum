package com.forum.activity;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface ActivityLogRepository extends ReactiveMongoRepository<ActivityLog, String> {
    Flux<ActivityLog> findByUserIdOrderByTimestampDesc(String userId);
    Flux<ActivityLog> findByUserIdAndActivityTypeOrderByTimestampDesc(String userId, ActivityType activityType);
    Flux<ActivityLog> findByUserIdAndTimestampBetweenOrderByTimestampDesc(String userId, Instant from, Instant to);
    Flux<ActivityLog> findByActivityTypeOrderByTimestampDesc(ActivityType activityType);
}
