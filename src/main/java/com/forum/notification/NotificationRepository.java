package com.forum.notification;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
    Flux<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    Flux<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(String recipientId);
    Mono<Long> countByQuestionIdAndCreatedAtAfter(String questionId, Instant after);
}
