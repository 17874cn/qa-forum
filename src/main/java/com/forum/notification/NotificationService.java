package com.forum.notification;

import com.forum.event.ForumEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Notification service - consumes Kafka events and creates notifications
 * max 3 notifications per question per day.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_NOTIFICATIONS_PER_QUESTION_PER_DAY = 3;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void handleAnswerCreated(ForumEvent event) {
        if (event.getQuestionAuthorId() != null
                && event.getQuestionAuthorId().equals(event.getAuthorId())) {
            log.info("Skipping self-answer notification for user={}", event.getAuthorId());
            return;
        }

        Notification notification = new Notification();
        notification.setRecipientId(event.getQuestionAuthorId());
        notification.setType("ANSWER_RECEIVED");
        notification.setTitle("New answer on your question");
        notification.setMessage("Someone answered your question: " + event.getTitle());
        notification.setQuestionId(event.getQuestionId());
        notification.setAnswerId(event.getAnswerId());

        saveWithRateLimit(notification);
    }

    public void handleAnswerAccepted(ForumEvent event) {
        Notification notification = new Notification();
        notification.setRecipientId(event.getAuthorId());
        notification.setType("ANSWER_ACCEPTED");
        notification.setTitle("Your answer was accepted!");
        notification.setMessage("Your answer on \"" + event.getTitle() + "\" was marked as accepted.");
        notification.setQuestionId(event.getQuestionId());
        notification.setAnswerId(event.getAnswerId());

        saveWithRateLimit(notification);
    }

    public void handleAnswerVoted(ForumEvent event) {
        Notification notification = new Notification();
        notification.setRecipientId(event.getAuthorId());
        notification.setType("ANSWER_VOTED");
        notification.setTitle("Your answer received a vote");
        notification.setMessage("Someone voted on your answer in \"" + event.getTitle() + "\".");
        notification.setQuestionId(event.getQuestionId());
        notification.setAnswerId(event.getAnswerId());

        saveWithRateLimit(notification);
    }

   //Max 3 per question per day
    private void saveWithRateLimit(Notification notification) {
        Instant startOfDay = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);

        notificationRepository.countByQuestionIdAndCreatedAtAfter(
                        notification.getQuestionId(), startOfDay)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(count -> {
                    if (count >= MAX_NOTIFICATIONS_PER_QUESTION_PER_DAY) {
                        log.info("Rate limit reached: questionId={}, count={}/{}. Skipping notification.",
                                notification.getQuestionId(), count, MAX_NOTIFICATIONS_PER_QUESTION_PER_DAY);
                        return Mono.<Notification>empty();
                    }

                    log.info("Sending notification: questionId={}, dailyCount={}/{}",
                            notification.getQuestionId(), count + 1, MAX_NOTIFICATIONS_PER_QUESTION_PER_DAY);
                    return notificationRepository.save(notification);
                })
                .subscribe(
                        saved -> log.info("Notification created: id={}, recipient={}, type={}",
                                saved.getId(), saved.getRecipientId(), saved.getType()),
                        error -> log.error("Failed to create notification", error)
                );
    }

    public Flux<Notification> getNotifications(String userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public Flux<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public Mono<Notification> markAsRead(String notificationId) {
        return notificationRepository.findById(notificationId)
                .flatMap(notification -> {
                    notification.setRead(true);
                    return notificationRepository.save(notification);
                });
    }
}
