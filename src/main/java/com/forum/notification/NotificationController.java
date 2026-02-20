package com.forum.notification;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public Flux<Notification> getNotifications(@PathVariable String userId) {
        return notificationService.getNotifications(userId);
    }

    @GetMapping("/{userId}/unread")
    public Flux<Notification> getUnread(@PathVariable String userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @PutMapping("/{notificationId}/read")
    public Mono<Notification> markAsRead(@PathVariable String notificationId) {
        return notificationService.markAsRead(notificationId);
    }
}
